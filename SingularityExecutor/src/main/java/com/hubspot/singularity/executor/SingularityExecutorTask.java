package com.hubspot.singularity.executor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.hubspot.deploy.DeployConfig;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.singularity.executor.models.EnvironmentContext;
import com.hubspot.singularity.executor.models.RunnerContext;
import com.hubspot.singularity.executor.utils.ExecutorUtils;

public class SingularityExecutorTask implements Callable<Integer> {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityExecutorTask.class);

  private final ExecutorDriver driver;
  private final String taskId;
  private final Protos.TaskInfo taskInfo;
  private final ObjectMapper jsonObjectMapper;
  private final ObjectMapper yamlObjectMapper;
  private final ArtifactManager artifactManager;
  private final TemplateManager templateManager;
  private final String deployEnv;
  
  private volatile Process process;

  public SingularityExecutorTask(ExecutorDriver driver, String deployEnv, ArtifactManager artifactManager, TemplateManager templateManager, ObjectMapper jsonObjectMapper, ObjectMapper yamlObjectMapper, String taskId, Protos.TaskInfo taskInfo) {
    this.driver = driver;
    this.deployEnv = deployEnv;
    this.templateManager = templateManager;
    this.jsonObjectMapper = jsonObjectMapper;
    this.yamlObjectMapper = yamlObjectMapper;
    this.taskId = taskId;
    this.taskInfo = taskInfo;
    this.artifactManager = artifactManager;
  }
  
  @Override
  public Integer call() throws Exception {
    ExecutorUtils.sendStatusUpdate(driver, taskInfo, Protos.TaskState.TASK_STARTING, "Downloading files");
    
    final ExecutorData executorData = getExecutorData(jsonObjectMapper, taskInfo);
    
    DeployConfig deployConfig = downloadFiles(executorData);
    
    ProcessBuilder processBuilder = buildProcessBuilder(executorData, deployConfig);
    
    process = processBuilder.start();
    
    ExecutorUtils.sendStatusUpdate(driver, taskInfo, Protos.TaskState.TASK_RUNNING, "Starting process");
    
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
  
  private Path getTaskDirectory() {
    return Paths.get("");
  }
  
  private Path getPath(String filename) {
    return getTaskDirectory().resolve(filename);
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
      
      if (fetched.endsWith(".tar.gz")) {
        artifactManager.untar(fetched, getTaskDirectory());
      } else {
        LOG.warn(String.format("Not sure what to do with %s", fetched));
      }
    }
    
    return deployConfig;
  }
  
  private ProcessBuilder buildProcessBuilder(ExecutorData executorData, DeployConfig deployConfig) {
    templateManager.writeEnvironmentScript(getPath("deploy.env"), new EnvironmentContext(executorData, deployConfig.getEnv().get(deployEnv)));
    templateManager.writeRunnerScript(getPath("runner.sh"), new RunnerContext(executorData.getCmd(), "wsorenson", "logfile.out", taskId)); // TODO
    
    List<String> command = Lists.newArrayList();
    command.add("./runner.sh");
    
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    
    processBuilder.inheritIO();
    
    return processBuilder;
  }
  
  public void kill() {
    // TODO handle process kill if process doesn't exist.
    if (process == null) {
      return;
    }
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
