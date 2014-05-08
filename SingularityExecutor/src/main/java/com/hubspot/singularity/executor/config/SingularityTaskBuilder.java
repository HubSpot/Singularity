package com.hubspot.singularity.executor.config;

import java.nio.file.Path;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.TaskInfo;

import ch.qos.logback.classic.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.singularity.executor.ArtifactManager;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.task.SingularityExecutorTask;
import com.hubspot.singularity.executor.utils.ExecutorUtils;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;

public class SingularityTaskBuilder {

  private final ObjectMapper jsonObjectMapper;

  private final TemplateManager templateManager;
  private final SingularityExecutorConfiguration configuration;
  
  private final SingularityExecutorLogging executorLogging;
  private final ExecutorUtils executorUtils;
  
  @Inject
  public SingularityTaskBuilder(@Named(SingularityRunnerBaseModule.JSON_MAPPER) ObjectMapper jsonObjectMapper, ExecutorUtils executorUtils, TemplateManager templateManager, SingularityExecutorLogging executorLogging, 
      SingularityExecutorConfiguration configuration) {
    this.executorUtils = executorUtils;
    this.jsonObjectMapper = jsonObjectMapper;
    this.templateManager = templateManager;
    this.executorLogging = executorLogging;
    this.configuration = configuration;
  }
  
  public Logger buildTaskLogger(String taskId) {
    Path javaExecutorLogPath = configuration.getExecutorJavaLogPath(taskId);
    
    return executorLogging.buildTaskLogger(taskId, javaExecutorLogPath.toString());
  }
  
  public SingularityExecutorTask buildTask(String taskId, ExecutorDriver driver, TaskInfo taskInfo, Logger log) {
    ArtifactManager artifactManager = buildArtifactManager(taskId, log);
    
    return new SingularityExecutorTask(driver, executorUtils, configuration, taskId, readExecutorData(jsonObjectMapper, taskInfo), artifactManager, taskInfo, templateManager, jsonObjectMapper, log);
  }
  
  private ArtifactManager buildArtifactManager(String taskId, Logger log) {
    return new ArtifactManager(configuration, taskId, log);
  }
  
  private ExecutorData readExecutorData(ObjectMapper objectMapper, Protos.TaskInfo taskInfo) {
    try {
      Preconditions.checkState(taskInfo.hasData(), "TaskInfo was missing executor data");
      
      return objectMapper.readValue(taskInfo.getData().toByteArray(), ExecutorData.class);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
    
}
