package com.hubspot.singularity.executor.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos.TaskInfo;

import ch.qos.logback.classic.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.executor.ArtifactManager;
import com.hubspot.singularity.executor.SingularityExecutorTask;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.utils.ExecutorUtils;

public class SingularityTaskBuilder {

  private final ObjectMapper yamlObjectMapper;
  private final ObjectMapper jsonObjectMapper;

  private final TemplateManager templateManager;
  private final String deployEnv;

  private final ExecutorUtils executorUtils;
  
  private final String executorJavaLog;
  private final String executorBashLog;
  
  private final Path cacheDirectory;
  private final SingularityExecutorLogging executorLogging;
  
  @Inject
  public SingularityTaskBuilder(@Named(SingularityExecutorModule.YAML_MAPPER) ObjectMapper yamlObjectMapper, @Named(SingularityExecutorModule.JSON_MAPPER) ObjectMapper jsonObjectMapper, TemplateManager templateManager, 
      String deployEnv, ExecutorUtils executorUtils, @Named(SingularityExecutorModule.ARTIFACT_CACHE_DIRECTORY) String cacheDirectory, SingularityExecutorLogging executorLogging, 
      @Named(SingularityExecutorModule.TASK_EXECUTOR_BASH_LOG_PATH) String executorBashLog, @Named(SingularityExecutorModule.TASK_EXECUTOR_JAVA_LOG__PATH) String executorJavaLog) {
    this.yamlObjectMapper = yamlObjectMapper;
    this.jsonObjectMapper = jsonObjectMapper;
    this.templateManager = templateManager;
    this.deployEnv = deployEnv;
    this.executorBashLog = executorBashLog;
    this.executorJavaLog = executorJavaLog;
    this.executorUtils = executorUtils;
    this.executorLogging = executorLogging;
    this.cacheDirectory = Paths.get(cacheDirectory);
  }
  
  public Logger buildTaskLogger(String taskId) {
    Path javaExecutorLogPath = getExecutorJavaLogPath(taskId);
    
    return executorLogging.buildTaskLogger(taskId, javaExecutorLogPath.toAbsolutePath().toString());
  }
  
  private Path getExecutorJavaLogPath(String taskId) {
    return getTaskDirectory(taskId).resolve(executorJavaLog);
  }
  
  private Path getExecutorBashLogPath(String taskId) {
    return getTaskDirectory(taskId).resolve(executorBashLog);
  }
  
  private Path getTaskDirectory(String taskId) {
    return Paths.get(taskId);
  }
  
  public SingularityExecutorTask buildTask(String taskId, ExecutorDriver driver, TaskInfo taskInfo, Logger log) {
    Path taskDirectory = getTaskDirectory(taskId);
    Path executorBashLog = getExecutorBashLogPath(taskId);
    
    ArtifactManager artifactManager = buildArtifactManager(executorBashLog, log);
    
    return new SingularityExecutorTask(driver, executorUtils, taskId, taskInfo, jsonObjectMapper, yamlObjectMapper, artifactManager, templateManager, deployEnv, taskDirectory, executorBashLog, log);
  }
  
  private ArtifactManager buildArtifactManager(Path executorOut, Logger log) {
    return new ArtifactManager(cacheDirectory, executorOut, log);
  }
    
}
