package com.hubspot.singularity.executor;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.TaskInfo;

import ch.qos.logback.classic.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.hubspot.deploy.EmbeddedArtifact;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.ExternalArtifact;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.models.EnvironmentContext;
import com.hubspot.singularity.executor.models.RunnerContext;
import com.hubspot.singularity.executor.utils.ExecutorUtils;

public class SingularityExecutorTask extends SafeProcessManager implements Callable<Integer> {

  private final ExecutorDriver driver;
  private final ExecutorUtils executorUtils;
  private final String taskId;
  private final Protos.TaskInfo taskInfo;
  private final ObjectMapper jsonObjectMapper;
  private final ArtifactManager artifactManager;
  private final TemplateManager templateManager;
  private final SingularityExecutorConfiguration configuration;
  
  private final Path taskDirectory;
  private final Path executorOut;

  private final Logger log;
  
  public SingularityExecutorTask(ExecutorDriver driver, SingularityExecutorConfiguration configuration, ExecutorUtils executorUtils, String taskId, TaskInfo taskInfo, ObjectMapper jsonObjectMapper, 
      ArtifactManager artifactManager, TemplateManager templateManager, Logger log) {
    super(log);
    
    this.driver = driver;
    this.executorUtils = executorUtils;
    this.taskId = taskId;
    this.taskInfo = taskInfo;
    this.jsonObjectMapper = jsonObjectMapper;
    this.artifactManager = artifactManager;
    this.templateManager = templateManager;
    this.log = log;
    this.configuration = configuration;
    
    this.taskDirectory = configuration.getTaskDirectoryPath(taskId);
    this.executorOut = configuration.getExecutorBashLogPath(taskId);
  }
  
  public Logger getLog() {
    return log;
  }

  @Override
  public Integer call() throws Exception {
    executorUtils.sendStatusUpdate(driver, taskInfo, Protos.TaskState.TASK_STARTING, "Downloading files", log);
    
    final ExecutorData executorData = getExecutorData(jsonObjectMapper, taskInfo);
    
    downloadFiles(executorData);
    extractFiles(executorData);
    
    ProcessBuilder processBuilder = buildProcessBuilder(executorData);
    
    Process process = startProcess(processBuilder);
    
    executorUtils.sendStatusUpdate(driver, taskInfo, Protos.TaskState.TASK_RUNNING, "Starting process", log);
    
    return process.waitFor();
  }

  public ExecutorData getExecutorData(ObjectMapper objectMapper, Protos.TaskInfo taskInfo) {
    try {
      Preconditions.checkState(taskInfo.hasData(), "TaskInfo was missing executor data");
      
      return objectMapper.readValue(taskInfo.getData().toByteArray(), ExecutorData.class);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
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
  
  private ProcessBuilder buildProcessBuilder(ExecutorData executorData) {
    templateManager.writeEnvironmentScript(getPath("deploy.env"), new EnvironmentContext(executorData));
    templateManager.writeRunnerScript(getPath("runner.sh"), new RunnerContext(executorData.getCmd(), executorData.getUser().or(configuration.getDefaultRunAsUser()), configuration.getServiceLog(), taskId)); 
    
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
  public void markKilled() {
    artifactManager.markKilled();

    super.markKilled();
  }
  
  @Override
  public void signalProcessIfActive() {
    artifactManager.destroyProcessIfActive();
    
    super.signalProcessIfActive();
  }
  
  @Override
  public void destroyProcessIfActive() {
    artifactManager.destroyProcessIfActive();
    
    super.destroyProcessIfActive();
  }

  public ExecutorDriver getDriver() {
    return driver;
  }

  public Protos.TaskInfo getTaskInfo() {
    return taskInfo;
  }

  public String getTaskId() {
    return taskId;
  }

  @Override
  public String toString() {
    return "SingularityExecutorTask [driver=" + driver + ", executorUtils=" + executorUtils + ", taskId=" + taskId + ", taskInfo=" + taskInfo + ", jsonObjectMapper=" + jsonObjectMapper + ", artifactManager=" + artifactManager
        + ", templateManager=" + templateManager + ", configuration=" + configuration + ", taskDirectory=" + taskDirectory + ", executorOut=" + executorOut + ", log=" + log + "]";
  }

}
