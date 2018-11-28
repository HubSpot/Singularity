package com.hubspot.singularity.executor.config;

import java.nio.file.Path;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.TaskInfo;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityTaskExecutorData;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.task.SingularityExecutorArtifactFetcher;
import com.hubspot.singularity.executor.task.SingularityExecutorTask;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskDefinition;
import com.hubspot.singularity.executor.utils.DockerUtils;
import com.hubspot.singularity.executor.utils.ExecutorUtils;
import com.hubspot.singularity.executor.utils.MesosUtils;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;
import com.hubspot.singularity.runner.base.configuration.SingularityRunnerBaseConfiguration;
import com.hubspot.singularity.runner.base.shared.JsonObjectFileHelper;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;

import ch.qos.logback.classic.Logger;

@Singleton
public class SingularityExecutorTaskBuilder {
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SingularityExecutorTaskBuilder.class);

  private final ObjectMapper jsonObjectMapper;

  private final TemplateManager templateManager;

  private final SingularityRunnerBaseConfiguration baseConfiguration;
  private final SingularityExecutorConfiguration executorConfiguration;
  private final SingularityS3Configuration s3Configuration;
  private final SingularityExecutorArtifactFetcher artifactFetcher;
  private final DockerUtils dockerUtils;

  private final SingularityExecutorLogging executorLogging;
  private final ExecutorUtils executorUtils;

  private final String executorPid;

  private final JsonObjectFileHelper jsonObjectFileHelper;

  @Inject
  public SingularityExecutorTaskBuilder(ObjectMapper jsonObjectMapper, JsonObjectFileHelper jsonObjectFileHelper, TemplateManager templateManager,
      SingularityExecutorLogging executorLogging, SingularityRunnerBaseConfiguration baseConfiguration, SingularityExecutorConfiguration executorConfiguration, @Named(SingularityRunnerBaseModule.PROCESS_NAME) String executorPid,
      ExecutorUtils executorUtils, SingularityExecutorArtifactFetcher artifactFetcher, DockerUtils dockerUtils, SingularityS3Configuration s3Configuration) {
    this.jsonObjectFileHelper = jsonObjectFileHelper;
    this.jsonObjectMapper = jsonObjectMapper;
    this.templateManager = templateManager;
    this.executorLogging = executorLogging;
    this.baseConfiguration = baseConfiguration;
    this.executorConfiguration = executorConfiguration;
    this.artifactFetcher = artifactFetcher;
    this.dockerUtils = dockerUtils;
    this.executorPid = executorPid;
    this.executorUtils = executorUtils;
    this.s3Configuration = s3Configuration;
  }

  public Logger buildTaskLogger(String taskId, String executorId) {
    Path javaExecutorLogPath = MesosUtils.getTaskDirectoryPath(taskId).resolve(executorConfiguration.getExecutorJavaLog());

    return executorLogging.buildTaskLogger(taskId, executorId, executorPid, javaExecutorLogPath.toString());
  }

  public SingularityExecutorTask buildTask(String taskId, ExecutorDriver driver, TaskInfo taskInfo, Logger log) {
    LOG.info("Building task {}", taskId);
    /*
    Executor data as read from protos: SingularityTaskExecutorData{s3UploaderAdditionalFiles=[SingularityS3UploaderFile{filename='access.log', s3UploaderBucket=Optional.absent(), s3UploaderKeyPattern=Optional.of(access/%group/%requestId/%Y/%m/%d/%taskId_%index-%s-%filename), s3UploaderFilenameHint=Optional.absent(), directory=Optional.of(logs), s3StorageClass=Optional.absent(), applyS3StorageClassAfterBytes=Optional.absent(), checkSubdirectories=false}, SingularityS3UploaderFile{filename='grpc-access.log', s3UploaderBucket=Optional.absent(), s3UploaderKeyPattern=Optional.of(grpc-access/%group/%requestId/%Y/%m/%d/%taskId_%index-%s-%filename), s3UploaderFilenameHint=Optional.absent(), directory=Optional.of(logs), s3StorageClass=Optional.absent(), applyS3StorageClassAfterBytes=Optional.absent(), checkSubdirectories=false}, SingularityS3UploaderFile{filename='service.log', s3UploaderBucket=Optional.absent(), s3UploaderKeyPattern=Optional.of(service/%group/%requestId/%Y/%m/%d/%taskId_%index-%s-%filename), s3UploaderFilenameHint=Optional.absent(), directory=Optional.of(logs), s3StorageClass=Optional.absent(), applyS3StorageClassAfterBytes=Optional.absent(), checkSubdirectories=false}, SingularityS3UploaderFile{filename='*', s3UploaderBucket=Optional.absent(), s3UploaderKeyPattern=Optional.of(extra/%group/%requestId/%Y/%m/%d/%taskId_%index-%s-%filename), s3UploaderFilenameHint=Optional.absent(), directory=Optional.of(s3uploads), s3StorageClass=Optional.absent(), applyS3StorageClassAfterBytes=Optional.absent(), checkSubdirectories=false}, SingularityS3UploaderFile{filename='*.hprof*', s3UploaderBucket=Optional.absent(), s3UploaderKeyPattern=Optional.of(heap/%group/%requestId/%Y/%m/%taskId_%index-%s-%filename), s3UploaderFilenameHint=Optional.absent(), directory=Optional.of(logs), s3StorageClass=Optional.absent(), applyS3StorageClassAfterBytes=Optional.absent(), checkSubdirectories=false}, SingularityS3UploaderFile{filename='*.gz', s3UploaderBucket=Optional.absent(), s3UploaderKeyPattern=Optional.of(thread_dumps/%group/%requestId/%Y/%m/%taskId_%index-%s-%filename), s3UploaderFilenameHint=Optional.absent(), directory=Optional.of(logs/thread_dumps), s3StorageClass=Optional.absent(), applyS3StorageClassAfterBytes=Optional.absent(), checkSubdirectories=true}]
    , defaultS3Bucket='hubspot-application-logs-test', s3UploaderKeyPattern='service/%group/%requestId/%Y/%m/%d/%taskId_%index-%s-%filename', serviceLog='service.log',
    serviceFinishedTailLog='tail_of_finished_service.log', requestGroup=Optional.absent(), s3StorageClass=Optional.of(STANDARD_IA), applyS3StorageClassAfterBytes=Optional.of(75000),
     cpuHardLimit=Optional.of(2),
      healthcheckOptions=Optional.of(
      HealthcheckOptions{
        uri='Optional.absent()',
        portIndex=Optional.absent(),
        portNumber=Optional.absent(),
        protocol=Optional.absent(),
        method=Optional.absent(),
        startupTimeoutSeconds=Optional.absent(),
        startupDelaySeconds=Optional.absent(),
        startupIntervalSeconds=Optional.absent(),
        intervalSeconds=Optional.absent(),
        responseTimeoutSeconds=Optional.absent(),
        maxRetries=Optional.absent(),
        failureStatusCodes=Optional.absent(),
        healthcheckResultFilePath=Optional.of(./healthy)})}
        ExecutorData{cmd='bash -c 'touch ./healthy'', embeddedArtifacts=[], externalArtifacts=[], s3Artifacts=[], successfulExitCodes=[], runningSentinel=Optional.absent(), user=Optional.of(root), extraCmdLineArgs=[], loggingTag=Optional.absent(), loggingExtraFields={}, sigKillProcessesAfterMillis=Optional.of(120000), maxTaskThreads=Optional.absent(), preserveTaskSandboxAfterFinish=Optional.absent(), maxOpenFiles=Optional.absent(), skipLogrotateAndCompress=Optional.absent(), s3ArtifactSignatures=Optional.absent(), logrotateFrequency=Optional.absent()}
     */
    SingularityTaskExecutorData taskExecutorData = readExecutorData(jsonObjectMapper, taskInfo);
    LOG.info("Executor data as read from protos: {}", taskExecutorData);

    SingularityExecutorTaskDefinition taskDefinition = new SingularityExecutorTaskDefinition(taskId, taskExecutorData, MesosUtils.getTaskDirectoryPath(taskId).toString(), executorPid,
        taskExecutorData.getServiceLog(), Files.getFileExtension(taskExecutorData.getServiceLog()), taskExecutorData.getServiceFinishedTailLog(), executorConfiguration.getTaskAppDirectory(),
        executorConfiguration.getExecutorBashLog(), executorConfiguration.getLogrotateStateFile(), executorConfiguration.getSignatureVerifyOut());

    jsonObjectFileHelper.writeObject(taskDefinition, executorConfiguration.getTaskDefinitionPath(taskId), log);

    return new SingularityExecutorTask(driver, executorUtils, baseConfiguration, executorConfiguration, taskDefinition, executorPid, artifactFetcher, taskInfo, templateManager, log, jsonObjectFileHelper, dockerUtils, s3Configuration, jsonObjectMapper);
  }

  private SingularityTaskExecutorData readExecutorData(ObjectMapper objectMapper, Protos.TaskInfo taskInfo) {
    try {
      Preconditions.checkState(taskInfo.hasData(), "TaskInfo was missing executor data");

      return objectMapper.readValue(taskInfo.getData().toByteArray(), SingularityTaskExecutorData.class);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

}
