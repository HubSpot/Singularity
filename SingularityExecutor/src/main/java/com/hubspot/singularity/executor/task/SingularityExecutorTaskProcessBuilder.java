package com.hubspot.singularity.executor.task;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.TaskState;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.hubspot.deploy.EmbeddedArtifact;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.ExternalArtifact;
import com.hubspot.singularity.executor.ArtifactManager;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.models.EnvironmentContext;
import com.hubspot.singularity.executor.models.RunnerContext;
import com.hubspot.singularity.executor.utils.ExecutorUtils;

public class SingularityExecutorTaskProcessBuilder implements Callable<ProcessBuilder> {

  private final SingularityExecutorTask task;

  private final ArtifactManager artifactManager;
  private final TemplateManager templateManager;
  private final SingularityExecutorConfiguration configuration;
  
  private final String executorPid;
  
  private final ExecutorUtils executorUtils;
  
  private final ExecutorData executorData;
  
  public SingularityExecutorTaskProcessBuilder(SingularityExecutorTask task, ExecutorUtils executorUtils, ArtifactManager artifactManager, TemplateManager templateManager, SingularityExecutorConfiguration configuration, ExecutorData executorData, String executorPid) {
    this.executorData = executorData;
    this.task = task;
    this.executorUtils = executorUtils;
    this.artifactManager = artifactManager;
    this.templateManager = templateManager;
    this.configuration = configuration;
    this.executorPid = executorPid;
  }
  
  public ArtifactManager getArtifactManager() {
    return artifactManager;
  }
 
  @Override
  public ProcessBuilder call() throws Exception {
    executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo(), TaskState.TASK_STARTING, String.format("Staging files... (executor pid: %s)", executorPid), task.getLog());
    
    downloadFiles(executorData);
    extractFiles(executorData);
    
    ProcessBuilder processBuilder = buildProcessBuilder(task.getTaskInfo(), executorData);
    
    task.getTaskLogManager().setup();
    
    return processBuilder;
  }
  
  private Path getPath(String filename) {
    return task.getTaskDefinition().getTaskDirectory().resolve(filename);
  }
  
  private void extractFiles(ExecutorData executorData) {
    for (EmbeddedArtifact artifact : executorData.getEmbeddedArtifacts()) {
      artifactManager.extract(artifact, task.getTaskDefinition().getTaskDirectory());
    }
  }
  
  private void downloadFiles(ExecutorData executorData) {
    for (ExternalArtifact artifact : executorData.getExternalArtifacts()) {
      Path fetched = artifactManager.fetch(artifact);
      
      Preconditions.checkState(fetched.getFileName().toString().endsWith(".tar.gz"), "%s did not appear to be a tar archive (did not end with .tar.gz)", fetched.getFileName());
      
      artifactManager.untar(fetched, task.getTaskDefinition().getTaskDirectory());
    }
  }
  
  private String getCommand(ExecutorData executorData) {
    final StringBuilder bldr = new StringBuilder(executorData.getCmd());
    for (String extraCmdLineArg : executorData.getExtraCmdLineArgs()) {
      bldr.append(" ");
      bldr.append(extraCmdLineArg);
    }
    return bldr.toString();
  }
  
  private ProcessBuilder buildProcessBuilder(TaskInfo taskInfo, ExecutorData executorData) {
    final String cmd = getCommand(executorData);
    
    templateManager.writeEnvironmentScript(getPath("deploy.env"), new EnvironmentContext(taskInfo));
    
    task.getLog().info("Writing a runner script to execute {}", cmd);
    
    templateManager.writeRunnerScript(getPath("runner.sh"), new RunnerContext(cmd, configuration.getTaskAppDirectory(), executorData.getUser().or(configuration.getDefaultRunAsUser()), configuration.getServiceLog(), task.getTaskId())); 
    
    List<String> command = Lists.newArrayList();
    command.add("bash");
    command.add("runner.sh");
    
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    
    processBuilder.directory(task.getTaskDefinition().getTaskDirectory().toFile());
    
    processBuilder.redirectError(task.getTaskDefinition().getExecutorBashOut().toFile());
    processBuilder.redirectOutput(task.getTaskDefinition().getExecutorBashOut().toFile());
    
    return processBuilder;
  }
  
  @Override
  public String toString() {
    return "SingularityExecutorTaskProcessBuilder [task=" + task.getTaskId() + "]";
  }

}
