package com.hubspot.singularity.executor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.TaskInfo;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.hubspot.deploy.DeployConfig;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.models.EnvironmentContext;
import com.hubspot.singularity.executor.models.RunnerContext;
import com.hubspot.singularity.executor.utils.ExecutorUtils;
import com.hubspot.singularity.executor.utils.ProcessUtils;

public class SingularityExecutorTask implements Callable<Integer> {

  private final ExecutorDriver driver;
  private final ExecutorUtils executorUtils;
  private final String taskId;
  private final Protos.TaskInfo taskInfo;
  private final ObjectMapper jsonObjectMapper;
  private final ObjectMapper yamlObjectMapper;
  private final ArtifactManager artifactManager;
  private final TemplateManager templateManager;
  private final SingularityExecutorConfiguration configuration;
  
  private volatile Process process;
  
  private final Path taskDirectory;
  private final Path executorOut;

  private final Logger log;
  
  private AtomicBoolean killed;

  public SingularityExecutorTask(ExecutorDriver driver, SingularityExecutorConfiguration configuration, ExecutorUtils executorUtils, String taskId, TaskInfo taskInfo, ObjectMapper jsonObjectMapper, ObjectMapper yamlObjectMapper, 
      ArtifactManager artifactManager, TemplateManager templateManager, Logger log) {
    this.driver = driver;
    this.executorUtils = executorUtils;
    this.taskId = taskId;
    this.taskInfo = taskInfo;
    this.jsonObjectMapper = jsonObjectMapper;
    this.yamlObjectMapper = yamlObjectMapper;
    this.artifactManager = artifactManager;
    this.templateManager = templateManager;
    this.log = log;
    this.configuration = configuration;
    
    this.taskDirectory = configuration.getTaskDirectoryPath(taskId);
    this.executorOut = configuration.getExecutorBashLogPath(taskId);
    
    this.killed = new AtomicBoolean(false);
  }

  @Override
  public Integer call() throws Exception {
    executorUtils.sendStatusUpdate(driver, taskInfo, Protos.TaskState.TASK_STARTING, "Downloading files", log);
    
    final ExecutorData executorData = getExecutorData(jsonObjectMapper, taskInfo);
    
    DeployConfig deployConfig = downloadFiles(executorData);
    
    ProcessBuilder processBuilder = buildProcessBuilder(executorData, deployConfig);
    
    process = processBuilder.start();
    
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
  
  private DeployConfig readDeployConfig(Path deployConfigPath) {
    try {
      return yamlObjectMapper.readValue(Files.newInputStream(deployConfigPath), DeployConfig.class);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
  
  private DeployConfig downloadFiles(ExecutorData executorData) {
    final Path deployConfigPath = getPath("config.yaml");
    
    artifactManager.downloadAndCheck(executorData.getDeployConfig().get(), deployConfigPath);
    
    final DeployConfig deployConfig = readDeployConfig(deployConfigPath);

    if (executorData.getArtifact().isPresent()) {
      Path fetched = artifactManager.fetch(executorData.getArtifact().get());
      
      Preconditions.checkState(fetched.getFileName().toString().endsWith(".tar.gz"), "%s did not appear to be a tar archive (did not end with .tar.gz)", fetched.getFileName());
      
      artifactManager.untar(fetched, taskDirectory);
    }
    
    return deployConfig;
  }
  
  private ProcessBuilder buildProcessBuilder(ExecutorData executorData, DeployConfig deployConfig) {
    templateManager.writeEnvironmentScript(getPath("deploy.env"), new EnvironmentContext(executorData, deployConfig.getEnv().get(configuration.getDeployEnv())));
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
    
  public void killSoft() {
    log.info("Asked to kill task gently");
    
    Preconditions.checkState(!killed.get(), "Task %s is already killed", taskId);
    
    killed.set(true);
    
    if (process == null) {
      log.info("Checking artifact process for task");
      artifactManager.destroyProcessIfActive();
    } else {
      log.info("Gracefully killing main process for task");
      ProcessUtils.gracefulKillUnixProcess(process);
    }
  }
  
  public boolean wasKilled() {
    return killed.get();
  }
  
  public void killHard() {
    log.info("Asked to kill task hard");
    
    if (process == null) {
      log.info("Task did not have a process to kill hard");
      return;
    }
    
    log.info("Destroying process for task");
    
    process.destroy();
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
    return "SingularityExecutorTask [driver=" + driver + ", taskId=" + taskId + ", taskInfo=" + taskInfo + ", process=" + process + "]";
  }

}
