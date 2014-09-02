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
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.task.SingularityExecutorArtifactFetcher;
import com.hubspot.singularity.executor.task.SingularityExecutorTask;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskDefinition;
import com.hubspot.singularity.executor.utils.ExecutorUtils;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;
import com.hubspot.singularity.runner.base.shared.JsonObjectFileHelper;

@Singleton
public class SingularityExecutorTaskBuilder {

  private final ObjectMapper jsonObjectMapper;

  private final TemplateManager templateManager;

  private final SingularityExecutorConfiguration configuration;
  private final SingularityExecutorArtifactFetcher artifactFetcher;

  private final SingularityExecutorLogging executorLogging;
  private final ExecutorUtils executorUtils;

  private final String executorPid;

  private final JsonObjectFileHelper jsonObjectFileHelper;

  @Inject
  public SingularityExecutorTaskBuilder(ObjectMapper jsonObjectMapper, JsonObjectFileHelper jsonObjectFileHelper, TemplateManager templateManager,
      SingularityExecutorLogging executorLogging,  SingularityExecutorConfiguration configuration, @Named(SingularityRunnerBaseModule.PROCESS_NAME) String executorPid,
      ExecutorUtils executorUtils, SingularityExecutorArtifactFetcher artifactFetcher) {
    this.jsonObjectFileHelper = jsonObjectFileHelper;
    this.jsonObjectMapper = jsonObjectMapper;
    this.templateManager = templateManager;
    this.executorLogging = executorLogging;
    this.configuration = configuration;
    this.artifactFetcher = artifactFetcher;
    this.executorPid = executorPid;
    this.executorUtils = executorUtils;
  }

  public Logger buildTaskLogger(String taskId) {
    Path javaExecutorLogPath = configuration.getTaskDirectoryPath(taskId).resolve(configuration.getExecutorJavaLog());

    return executorLogging.buildTaskLogger(taskId, javaExecutorLogPath.toString());
  }

  public SingularityExecutorTask buildTask(String taskId, ExecutorDriver driver, TaskInfo taskInfo, Logger log) {
    ExecutorData executorData = readExecutorData(jsonObjectMapper, taskInfo);

    SingularityExecutorTaskDefinition taskDefinition = new SingularityExecutorTaskDefinition(taskId, executorData, configuration.getTaskDirectoryPath(taskId).toString(), configuration.getServiceLog(),
        configuration.getTaskAppDirectory(), configuration.getExecutorBashLog(), configuration.getLogrotateStateFile());

    jsonObjectFileHelper.writeObject(taskDefinition, configuration.getTaskDefinitionPath(taskId), log);

    return new SingularityExecutorTask(driver, executorUtils, configuration, taskDefinition, executorPid, artifactFetcher, taskInfo, templateManager, jsonObjectMapper, log, jsonObjectFileHelper);
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
