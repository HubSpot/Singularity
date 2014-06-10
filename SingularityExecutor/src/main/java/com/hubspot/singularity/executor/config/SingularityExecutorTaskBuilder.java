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
import com.hubspot.singularity.executor.task.SingularityExecutorTaskDefinition;
import com.hubspot.singularity.executor.utils.ExecutorUtils;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;

public class SingularityExecutorTaskBuilder {

  private final ObjectMapper jsonObjectMapper;

  private final TemplateManager templateManager;
  private final SingularityExecutorConfiguration configuration;
  
  private final SingularityExecutorLogging executorLogging;
  private final ExecutorUtils executorUtils;
  
  private final String executorPid;
  
  @Inject
  public SingularityExecutorTaskBuilder(@Named(SingularityRunnerBaseModule.JSON_MAPPER) ObjectMapper jsonObjectMapper, ExecutorUtils executorUtils, TemplateManager templateManager, SingularityExecutorLogging executorLogging, 
      SingularityExecutorConfiguration configuration, @Named(SingularityRunnerBaseModule.PROCESS_NAME) String executorPid) {
    this.executorUtils = executorUtils;
    this.jsonObjectMapper = jsonObjectMapper;
    this.templateManager = templateManager;
    this.executorLogging = executorLogging;
    this.configuration = configuration;
    this.executorPid = executorPid;
  }
  
  public Logger buildTaskLogger(String taskId) {
    Path javaExecutorLogPath = configuration.getExecutorJavaLogPath(taskId);
    
    return executorLogging.buildTaskLogger(taskId, javaExecutorLogPath.toString());
  }
  
  public SingularityExecutorTask buildTask(String taskId, ExecutorDriver driver, TaskInfo taskInfo, Logger log) {
    ExecutorData executorData = readExecutorData(jsonObjectMapper, taskInfo);
    
    SingularityExecutorTaskDefinition taskDefinition = new SingularityExecutorTaskDefinition(taskId, executorData);
    
    executorUtils.writeObject(taskDefinition, configuration.getTaskDefinitionPath(taskId), log);
    
    ArtifactManager artifactManager = buildArtifactManager(taskId, log);
    
    return new SingularityExecutorTask(driver, executorUtils, configuration, taskDefinition, executorPid, artifactManager, taskInfo, templateManager, jsonObjectMapper, log);
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
