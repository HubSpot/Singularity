package com.hubspot.singularity.executor.task;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.TaskState;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.models.DockerContext;
import com.hubspot.singularity.executor.models.EnvironmentContext;
import com.hubspot.singularity.executor.models.RunnerContext;
import com.hubspot.singularity.executor.task.SingularityExecutorArtifactFetcher.SingularityExecutorTaskArtifactFetcher;
import com.hubspot.singularity.executor.utils.DockerUtils;
import com.hubspot.singularity.executor.utils.ExecutorUtils;
import com.hubspot.singularity.runner.base.shared.ProcessFailedException;
import com.spotify.docker.client.DockerException;

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

  public SingularityExecutorTaskProcessBuilder(SingularityExecutorTask task,
                                               ExecutorUtils executorUtils,
                                               SingularityExecutorArtifactFetcher artifactFetcher,
                                               TemplateManager templateManager,
                                               SingularityExecutorConfiguration configuration,
                                               ExecutorData executorData, String executorPid,
                                               DockerUtils dockerUtils) {
    this.executorData = executorData;
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
      executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo(), TaskState.TASK_STARTING, String.format("Pulling image... (executor pid: %s)", executorPid), task.getLog());
      try {
        dockerUtils.pull(task.getTaskInfo().getContainer().getDocker().getImage());
      } catch (DockerException e) {
        throw new ProcessFailedException("Could not pull docker image", e);
      }
    }

    executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo(), TaskState.TASK_STARTING, String.format("Staging files... (executor pid: %s)", executorPid), task.getLog());

    taskArtifactFetcher = Optional.of(artifactFetcher.buildTaskFetcher(executorData, task));
    taskArtifactFetcher.get().fetchFiles();

    task.getArtifactVerifier().checkSignatures();

    ProcessBuilder processBuilder = buildProcessBuilder(task.getTaskInfo(), executorData);

    task.getTaskLogManager().setup();

    return processBuilder;
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
    final StringBuilder bldr = new StringBuilder(executorData.getCmd());
    for (String extraCmdLineArg : executorData.getExtraCmdLineArgs()) {
      bldr.append(" ");
      bldr.append(extraCmdLineArg);
    }
    return bldr.toString();
  }

  private String getExecutorUser() {
    return System.getProperty("user.name");  // TODO: better way to do this?
  }

  private ProcessBuilder buildProcessBuilder(TaskInfo taskInfo, ExecutorData executorData) {
    final String cmd = getCommand(executorData);

    RunnerContext runnerContext = new RunnerContext(
      cmd,
      configuration.getTaskAppDirectory(),
      configuration.getLogrotateToDirectory(),
      executorData.getUser().or(configuration.getDefaultRunAsUser()),
      configuration.getServiceLog(),
      serviceLogOutPath(),
      task.getTaskId(),
      executorData.getMaxTaskThreads().or(configuration.getMaxTaskThreads()),
      !getExecutorUser().equals(executorData.getUser().or(configuration.getDefaultRunAsUser())),
      executorData.getMaxOpenFiles().orNull(),
      String.format(configuration.getSwitchUserCommandFormat(), executorData.getUser().or(configuration.getDefaultRunAsUser())));

    EnvironmentContext environmentContext = new EnvironmentContext(taskInfo);

    if (taskInfo.hasContainer() && taskInfo.getContainer().hasDocker()) {
      task.getLog().info("Writing a runner script to execute {} in docker container", cmd);
      templateManager.writeDockerScript(getPath("runner.sh"), new DockerContext(environmentContext, runnerContext, configuration.getDockerPrefix(), configuration.getDockerStopTimeout(), taskInfo.getContainer().getDocker().getPrivileged()));
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

  private String serviceLogOutPath() {
    Path basePath = task.getTaskDefinition().getTaskDirectoryPath();
    Path app = basePath.resolve(configuration.getTaskAppDirectory()).normalize();
    return app.relativize(basePath).resolve(configuration.getServiceLog()).toString();
  }

  @Override
  public String toString() {
    return "SingularityExecutorTaskProcessBuilder [task=" + task.getTaskId() + "]";
  }

}
