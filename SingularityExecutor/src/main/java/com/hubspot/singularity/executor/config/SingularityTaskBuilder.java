package com.hubspot.singularity.executor.config;

import java.nio.file.Path;

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

  private final ObjectMapper jsonObjectMapper;

  private final TemplateManager templateManager;
  private final SingularityExecutorConfiguration configuration;

  private final ExecutorUtils executorUtils;
  
  private final SingularityExecutorLogging executorLogging;
  
  @Inject
  public SingularityTaskBuilder(@Named(SingularityExecutorModule.JSON_MAPPER) ObjectMapper jsonObjectMapper, TemplateManager templateManager, 
      ExecutorUtils executorUtils, SingularityExecutorLogging executorLogging, SingularityExecutorConfiguration configuration) {
    this.jsonObjectMapper = jsonObjectMapper;
    this.templateManager = templateManager;
    this.executorUtils = executorUtils;
    this.executorLogging = executorLogging;
    this.configuration = configuration;
  }
  
  public Logger buildTaskLogger(String taskId) {
    Path javaExecutorLogPath = configuration.getExecutorJavaLogPath(taskId);
    
    return executorLogging.buildTaskLogger(taskId, javaExecutorLogPath.toAbsolutePath().toString());
  }
  
  public SingularityExecutorTask buildTask(String taskId, ExecutorDriver driver, TaskInfo taskInfo, Logger log) {
    ArtifactManager artifactManager = buildArtifactManager(taskId, log);
    
    return new SingularityExecutorTask(driver, configuration, executorUtils, taskId, taskInfo, jsonObjectMapper, artifactManager, templateManager, log);
  }
  
  private ArtifactManager buildArtifactManager(String taskId, Logger log) {
    return new ArtifactManager(configuration, taskId, log);
  }
    
}
