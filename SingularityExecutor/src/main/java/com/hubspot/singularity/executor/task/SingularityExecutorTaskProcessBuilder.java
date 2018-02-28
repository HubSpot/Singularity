package com.hubspot.singularity.executor.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.TaskState;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.hubspot.deploy.ArtifactList;
import com.hubspot.deploy.EmbeddedArtifact;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.ExternalArtifact;
import com.hubspot.deploy.RemoteArtifact;
import com.hubspot.deploy.S3Artifact;
import com.hubspot.deploy.S3ArtifactSignature;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.models.DockerContext;
import com.hubspot.singularity.executor.models.EnvironmentContext;
import com.hubspot.singularity.executor.models.RunnerContext;
import com.hubspot.singularity.executor.task.SingularityExecutorArtifactFetcher.SingularityExecutorTaskArtifactFetcher;
import com.hubspot.singularity.executor.utils.DockerUtils;
import com.hubspot.singularity.executor.utils.ExecutorUtils;
import com.hubspot.singularity.runner.base.shared.ProcessFailedException;
import com.spotify.docker.client.exceptions.DockerException;

public class SingularityExecutorTaskProcessBuilder implements Callable<ProcessBuilder> {

  private final SingularityExecutorTask task;

  private final TemplateManager templateManager;
  private final SingularityExecutorConfiguration configuration;

  private final String executorPid;

  private final ExecutorUtils executorUtils;

  private final ExecutorData executorData;

  private final SingularityExecutorArtifactFetcher artifactFetcher;

  private Optional<SingularityExecutorTaskArtifactFetcher> taskArtifactFetcher;

  private DockerUtils dockerUtils;

  private final ObjectMapper objectMapper;

  public SingularityExecutorTaskProcessBuilder(SingularityExecutorTask task,
      ExecutorUtils executorUtils,
      SingularityExecutorArtifactFetcher artifactFetcher,
      TemplateManager templateManager,
      SingularityExecutorConfiguration configuration,
      ExecutorData executorData, String executorPid,
      DockerUtils dockerUtils, ObjectMapper objectMapper) {
    this.executorData = executorData;
    this.objectMapper = objectMapper;
    this.task = task;
    this.executorUtils = executorUtils;
    this.artifactFetcher = artifactFetcher;
    this.templateManager = templateManager;
    this.configuration = configuration;
    this.executorPid = executorPid;
    this.taskArtifactFetcher = Optional.absent();
    this.dockerUtils = dockerUtils;
  }

  @Override
  public ProcessBuilder call() throws Exception {
    if (task.getTaskInfo().hasContainer() && task.getTaskInfo().getContainer().hasDocker()) {
      executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), TaskState.TASK_STARTING, String.format("Pulling image... (executor pid: %s)", executorPid), task.getLog());
      try {
        dockerUtils.pull(task.getTaskInfo().getContainer().getDocker().getImage());
      } catch (DockerException e) {
        throw new ProcessFailedException("Could not pull docker image", e);
      }
    }

    executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), TaskState.TASK_STARTING, String.format("Staging files... (executor pid: %s)", executorPid), task.getLog());

    taskArtifactFetcher = Optional.of(artifactFetcher.buildTaskFetcher(executorData, task));

    taskArtifactFetcher.get().fetchFiles(executorData.getEmbeddedArtifacts(), executorData.getS3Artifacts(),
        executorData.getS3ArtifactSignaturesOrEmpty(), executorData.getExternalArtifacts());
    task.getArtifactVerifier().checkSignatures(executorData.getS3ArtifactSignaturesOrEmpty());

    List<ArtifactList> artifactLists = new ArrayList<>();
    artifactLists.addAll(checkArtifactsForArtifactLists(executorData.getS3Artifacts()));
    artifactLists.addAll(checkArtifactsForArtifactLists(executorData.getS3ArtifactSignaturesOrEmpty()));
    artifactLists.addAll(checkArtifactsForArtifactLists(executorData.getExternalArtifacts()));

    if (!artifactLists.isEmpty()) {
      List<EmbeddedArtifact> embeddedArtifacts = new ArrayList<>();
      List<S3Artifact> s3Artifacts = new ArrayList<>();
      List<S3ArtifactSignature> s3ArtifactSignatures = new ArrayList<>();
      List<ExternalArtifact> externalArtifacts = new ArrayList<>();

      for (ArtifactList artifactList : artifactLists) {
        embeddedArtifacts.addAll(artifactList.getEmbeddedArtifacts());
        s3Artifacts.addAll(artifactList.getS3Artifacts());
        s3ArtifactSignatures.addAll(artifactList.getS3ArtifactSignatures());
        externalArtifacts.addAll(artifactList.getExternalArtifacts());
      }

      task.getLog().info("Found {} artifact lists with {} embedded, {} s3, {} external, fetching...", artifactLists.size(), embeddedArtifacts.size(), s3Artifacts.size() + s3ArtifactSignatures.size(),
          externalArtifacts.size());

      taskArtifactFetcher.get().fetchFiles(embeddedArtifacts, s3Artifacts, s3ArtifactSignatures, externalArtifacts);
      task.getArtifactVerifier().checkSignatures(s3ArtifactSignatures);
    }

    ProcessBuilder processBuilder = buildProcessBuilder(task.getTaskInfo(), executorData, task.getTaskDefinition().getServiceLogFileName());

    task.getTaskLogManager().setup();

    return processBuilder;
  }

  private List<ArtifactList> checkArtifactsForArtifactLists(List<? extends RemoteArtifact> remoteArtifacts) {
    List<ArtifactList> artifactLists = new ArrayList<>();
    for (RemoteArtifact remoteArtifact : remoteArtifacts) {
      if (remoteArtifact.isArtifactList()) {
        Path pathToArtifact = task.getArtifactPath(remoteArtifact, task.getTaskDefinition().getTaskDirectoryPath()).resolve(remoteArtifact.getFilename());
        if (!Files.exists(pathToArtifact)) {
          throw new IllegalStateException(String.format("Couldn't find artifact at %s - %s", pathToArtifact, remoteArtifact));
        }
        try {
          artifactLists.add(objectMapper.readValue(pathToArtifact.toFile(), ArtifactList.class));
        } catch (IOException e) {
          throw new RuntimeException("Couldn't read artifacts from " + pathToArtifact, e);
        }
      }
    }
    return artifactLists;
  }

  public void cancel() {
    if (taskArtifactFetcher.isPresent()) {
      taskArtifactFetcher.get().cancel();
    }
  }

  private Path getPath(String filename) {
    return task.getTaskDefinition().getTaskDirectoryPath().resolve(filename);
  }

  private String getCommand(ExecutorData executorData) {
    final StringBuilder bldr = new StringBuilder(Strings.isNullOrEmpty(executorData.getCmd()) ? "" : executorData.getCmd());
    for (String extraCmdLineArg : executorData.getExtraCmdLineArgs()) {
      bldr.append(" ");
      bldr.append(extraCmdLineArg);
    }
    return bldr.toString();
  }

  private String getExecutorUser() {
    return System.getProperty("user.name"); // TODO: better way to do this?
  }

  private ProcessBuilder buildProcessBuilder(TaskInfo taskInfo, ExecutorData executorData, String serviceLog) {
    final String cmd = getCommand(executorData);

    RunnerContext runnerContext = new RunnerContext(
        cmd,
        configuration.getTaskAppDirectory(),
        configuration.getLogrotateToDirectory(),
        executorData.getUser().or(configuration.getDefaultRunAsUser()),
        serviceLog,
        serviceLogOutPath(serviceLog),
        task.getTaskId(),
        executorData.getMaxTaskThreads().or(configuration.getMaxTaskThreads()),
        !getExecutorUser().equals(executorData.getUser().or(configuration.getDefaultRunAsUser())),
        executorData.getMaxOpenFiles().orNull(),
        String.format(configuration.getSwitchUserCommandFormat(), executorData.getUser().or(configuration.getDefaultRunAsUser())),
        configuration.isUseFileAttributes());

    EnvironmentContext environmentContext = new EnvironmentContext(taskInfo);

    if (taskInfo.hasContainer() && taskInfo.getContainer().hasDocker()) {
      task.getLog().info("Writing a runner script to execute {} in docker container", cmd);
      templateManager.writeDockerScript(getPath("runner.sh"),
          new DockerContext(environmentContext, runnerContext, configuration.getDockerPrefix(), configuration.getDockerStopTimeout(), taskInfo.getContainer().getDocker().getPrivileged()));
    } else {
      templateManager.writeEnvironmentScript(getPath("deploy.env"), environmentContext);

      task.getLog().info("Writing a runner script to execute {} with {}", cmd, runnerContext);

      templateManager.writeRunnerScript(getPath("runner.sh"), runnerContext);
    }

    List<String> command = Lists.newArrayList();
    command.add("bash");
    command.add("runner.sh");

    ProcessBuilder processBuilder = new ProcessBuilder(command);

    processBuilder.directory(task.getTaskDefinition().getTaskDirectoryPath().toFile());

    processBuilder.redirectError(task.getTaskDefinition().getExecutorBashOutPath().toFile());
    processBuilder.redirectOutput(task.getTaskDefinition().getExecutorBashOutPath().toFile());

    return processBuilder;
  }

  private String serviceLogOutPath(String serviceLog) {
    Path basePath = task.getTaskDefinition().getTaskDirectoryPath();
    Path app = basePath.resolve(configuration.getTaskAppDirectory()).normalize();
    return app.relativize(basePath).resolve(serviceLog).toString();
  }

  @Override
  public String toString() {
    return "SingularityExecutorTaskProcessBuilder [task=" + task.getTaskId() + "]";
  }

}
