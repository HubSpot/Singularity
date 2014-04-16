package com.hubspot.singularity.executor.task;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.hubspot.deploy.EmbeddedArtifact;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.ExternalArtifact;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.executor.ArtifactManager;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.models.EnvironmentContext;
import com.hubspot.singularity.executor.models.RunnerContext;
import com.hubspot.singularity.executor.utils.ExecutorUtils;
import org.apache.mesos.Protos.TaskState;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

public class SingularityExecutorTaskProcessBuilder implements Callable<ProcessBuilder> {

  private final SingularityExecutorTask task;

  private final ArtifactManager artifactManager;
  private final TemplateManager templateManager;
  private final SingularityExecutorConfiguration configuration;
  
  private final ExecutorUtils executorUtils;
  
  private final Path taskDirectory;
  private final Path executorOut;

  private final ExecutorData executorData;
  
  public SingularityExecutorTaskProcessBuilder(SingularityExecutorTask task, ExecutorUtils executorUtils, ArtifactManager artifactManager, TemplateManager templateManager, SingularityExecutorConfiguration configuration, ExecutorData executorData) {
    this.executorData = executorData;
    this.task = task;
    this.executorUtils = executorUtils;
    this.artifactManager = artifactManager;
    this.templateManager = templateManager;
    this.configuration = configuration;
    
    this.taskDirectory = configuration.getTaskDirectoryPath(task.getTaskId());
    this.executorOut = configuration.getExecutorBashLogPath(task.getTaskId());
  }
  
  public ArtifactManager getArtifactManager() {
    return artifactManager;
  }

  @Override
  public ProcessBuilder call() throws Exception {
    executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo(), TaskState.TASK_STARTING, "Staging files...", task.getLog());
    
    downloadFiles(executorData);
    extractFiles(executorData);
    
    ProcessBuilder processBuilder = buildProcessBuilder(executorData, MesosUtils.getAllPorts(task.getTaskInfo()));
  
    return processBuilder;
  }

  private Path getPath(String filename) {
    return taskDirectory.resolve(filename);
  }
  
  private void extractFiles(ExecutorData executorData) {
    for (EmbeddedArtifact artifact : executorData.getEmbeddedArtifacts()) {
      artifactManager.extract(artifact, taskDirectory);
    }
  }
  
  private void downloadFiles(ExecutorData executorData) {
    for (ExternalArtifact artifact : executorData.getExternalArtifacts()) {
      Path fetched = artifactManager.fetch(artifact);
      
      Preconditions.checkState(fetched.getFileName().toString().endsWith(".tar.gz"), "%s did not appear to be a tar archive (did not end with .tar.gz)", fetched.getFileName());
      
      artifactManager.untar(fetched, taskDirectory);
    }
  }
  
  private ProcessBuilder buildProcessBuilder(ExecutorData executorData, List<Long> ports) {
    templateManager.writeEnvironmentScript(getPath("deploy.env"), new EnvironmentContext(executorData, ports));
    templateManager.writeRunnerScript(getPath("runner.sh"), new RunnerContext(executorData.getCmd(), executorData.getUser().or(configuration.getDefaultRunAsUser()), configuration.getServiceLog(), task.getTaskId())); 
    
    List<String> command = Lists.newArrayList();
    command.add("bash");
    command.add("runner.sh");
    
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    
    processBuilder.directory(taskDirectory.toFile());
    
    processBuilder.redirectError(executorOut.toFile());
    processBuilder.redirectOutput(executorOut.toFile());
    
    return processBuilder;
  }

  @Override
  public String toString() {
    return "SingularityExecutorTaskProcessBuilder [task=" + task.getTaskId() + "]";
  }

}
