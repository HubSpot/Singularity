package com.hubspot.singularity.executor.cleanup;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.SingularityContainerType;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskExecutorData;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.client.SingularityClient;
import com.hubspot.singularity.client.SingularityClientException;
import com.hubspot.singularity.client.SingularityClientProvider;
import com.hubspot.singularity.executor.SingularityExecutorCleanupStatistics;
import com.hubspot.singularity.executor.SingularityExecutorCleanupStatistics.SingularityExecutorCleanupStatisticsBuilder;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.cleanup.config.SingularityExecutorCleanupConfiguration;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskCleanup;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskDefinition;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskLogManager;
import com.hubspot.singularity.executor.task.TaskCleanupResult;
import com.hubspot.singularity.executor.utils.DockerUtils;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;
import com.hubspot.singularity.runner.base.configuration.SingularityRunnerBaseConfiguration;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.runner.base.shared.CompressionType;
import com.hubspot.singularity.runner.base.shared.JsonObjectFileHelper;
import com.hubspot.singularity.runner.base.shared.ProcessFailedException;
import com.hubspot.singularity.runner.base.shared.ProcessUtils;
import com.hubspot.singularity.runner.base.shared.SimpleProcessManager;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerInfo;

public class SingularityExecutorCleanup {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityExecutorCleanup.class);

  private final JsonObjectFileHelper jsonObjectFileHelper;
  private final SingularityRunnerBaseConfiguration baseConfiguration;
  private final SingularityExecutorConfiguration executorConfiguration;
  private final SingularityClient singularityClient;
  private final TemplateManager templateManager;
  private final SingularityExecutorCleanupConfiguration cleanupConfiguration;
  private final MesosClient mesosClient;
  private final ProcessUtils processUtils;
  private final DockerUtils dockerUtils;
  private final String hostname;
  private final SingularityRunnerExceptionNotifier exceptionNotifier;

  @Inject
  public SingularityExecutorCleanup(SingularityClientProvider singularityClientProvider, JsonObjectFileHelper jsonObjectFileHelper, SingularityRunnerBaseConfiguration baseConfiguration,
      SingularityExecutorConfiguration executorConfiguration, SingularityExecutorCleanupConfiguration cleanupConfiguration, TemplateManager templateManager, MesosClient mesosClient,
      DockerUtils dockerUtils, @Named(SingularityRunnerBaseModule.HOST_NAME_PROPERTY) String hostname, SingularityRunnerExceptionNotifier exceptionNotifier) {
    this.jsonObjectFileHelper = jsonObjectFileHelper;
    this.baseConfiguration = baseConfiguration;
    this.executorConfiguration = executorConfiguration;
    this.cleanupConfiguration = cleanupConfiguration;
    this.templateManager = templateManager;
    this.mesosClient = mesosClient;
    this.processUtils = new ProcessUtils(LOG);
    this.dockerUtils = dockerUtils;
    this.hostname = hostname;
    this.exceptionNotifier = exceptionNotifier;
    if (cleanupConfiguration.getSingularityClientCredentials().isPresent()) {
      singularityClientProvider.setCredentials(cleanupConfiguration.getSingularityClientCredentials().get());
    }
    this.singularityClient = singularityClientProvider.setSsl(cleanupConfiguration.isSingularityUseSsl()).get();
  }

  public SingularityExecutorCleanupStatistics clean() {
    final SingularityExecutorCleanupStatisticsBuilder statisticsBldr = new SingularityExecutorCleanupStatisticsBuilder();
    final Path directory = Paths.get(executorConfiguration.getGlobalTaskDefinitionDirectory());

    Set<String> runningTaskIds = null;

    try {
      runningTaskIds = getRunningTaskIds();
    } catch (Exception e) {
      LOG.error("While fetching running tasks from singularity", e);
      exceptionNotifier.notify(String.format("Error fetching running tasks (%s)", e.getMessage()), e, Collections.<String, String>emptyMap());
      statisticsBldr.setErrorMessage(e.getMessage());
      return statisticsBldr.build();
    }

    LOG.info("Found {} running tasks from Mesos", runningTaskIds);

    statisticsBldr.setMesosRunningTasks(runningTaskIds.size());

    if (runningTaskIds.isEmpty()) {
      if (!isDecommissioned()) {
        if (cleanupConfiguration.isSafeModeWontRunWithNoTasks()) {
          final String errorMessage = "Running in safe mode and found 0 running tasks - aborting cleanup";
          LOG.error(errorMessage);
          statisticsBldr.setErrorMessage(errorMessage);
          return statisticsBldr.build();
        } else {
          LOG.warn("Found 0 running tasks - proceeding with cleanup as we are not in safe mode");
        }
      } else {
        if (!cleanupConfiguration.isCleanTasksWhenDecommissioned()) {
          return statisticsBldr.build();
        }
      }
    }

    if (cleanupConfiguration.isRunDockerCleanup()) {
      cleanDocker(runningTaskIds);
    }

    for (Path file : JavaUtils.iterable(directory)) {
      if (!Objects.toString(file.getFileName()).endsWith(executorConfiguration.getGlobalTaskDefinitionSuffix())) {
        LOG.debug("Ignoring file {} that doesn't have suffix {}", file, executorConfiguration.getGlobalTaskDefinitionSuffix());
        statisticsBldr.incrInvalidTasks();
        continue;
      }

      statisticsBldr.incrTotalTaskFiles();

      try {
        Optional<SingularityExecutorTaskDefinition> maybeTaskDefinition = jsonObjectFileHelper.read(file, LOG, SingularityExecutorTaskDefinition.class);

        if (!maybeTaskDefinition.isPresent()) {
          statisticsBldr.incrInvalidTasks();
          continue;
        }

        SingularityExecutorTaskDefinition taskDefinition = withDefaults(maybeTaskDefinition.get());

        final String taskId = taskDefinition.getTaskId();

        LOG.info("{} - Starting possible cleanup", taskId);

        if (runningTaskIds.contains(taskId) || executorStillRunning(taskDefinition)) {
          statisticsBldr.incrRunningTasksIgnored();
          continue;
        }

        Optional<SingularityTaskHistory> taskHistory = null;

        try {
          taskHistory = singularityClient.getHistoryForTask(taskId);
        } catch (SingularityClientException sce) {
          LOG.error("{} - Failed fetching history", taskId, sce);
          exceptionNotifier.notify(String.format("Error fetching history (%s)", sce.getMessage()), sce, ImmutableMap.<String, String>of("taskId", taskId));
          statisticsBldr.incrErrorTasks();
          continue;
        }

        TaskCleanupResult result = cleanTask(taskDefinition, taskHistory);

        LOG.info("{} - {}", taskId, result);

        switch (result) {
          case ERROR:
            statisticsBldr.incrErrorTasks();
            break;
          case SUCCESS:
            statisticsBldr.incrSuccessfullyCleanedTasks();
            break;
          case WAITING:
            statisticsBldr.incrWaitingTasks();
            break;
           default:
            break;
        }

      } catch (IOException ioe) {
        LOG.error("Couldn't read file {}", file, ioe);
        exceptionNotifier.notify(String.format("Error reading file (%s)", ioe.getMessage()), ioe, ImmutableMap.of("file", file.toString()));
        statisticsBldr.incrIoErrorTasks();
      }
    }

    return statisticsBldr.build();
  }

  private SingularityExecutorTaskDefinition withDefaults(SingularityExecutorTaskDefinition oldDefinition) {
      return new SingularityExecutorTaskDefinition(
        oldDefinition.getTaskId(),
        new SingularityTaskExecutorData(
            oldDefinition.getExecutorData(),
            oldDefinition.getExecutorData().getS3UploaderAdditionalFiles() == null ? cleanupConfiguration.getS3UploaderAdditionalFiles() :  oldDefinition.getExecutorData().getS3UploaderAdditionalFiles(),
            Strings.isNullOrEmpty(oldDefinition.getExecutorData().getDefaultS3Bucket()) ? cleanupConfiguration.getDefaultS3Bucket() : oldDefinition.getExecutorData().getDefaultS3Bucket(),
            Strings.isNullOrEmpty(oldDefinition.getExecutorData().getS3UploaderKeyPattern()) ? cleanupConfiguration.getS3KeyFormat(): oldDefinition.getExecutorData().getS3UploaderKeyPattern(),
            Strings.isNullOrEmpty(oldDefinition.getExecutorData().getServiceLog()) ? cleanupConfiguration.getDefaultServiceLog() : oldDefinition.getExecutorData().getServiceLog(),
            Strings.isNullOrEmpty(oldDefinition.getExecutorData().getServiceFinishedTailLog()) ? cleanupConfiguration.getDefaultServiceFinishedTailLog() : oldDefinition.getExecutorData().getServiceFinishedTailLog(),
            oldDefinition.getExecutorData().getRequestGroup(),
            oldDefinition.getExecutorData().getS3StorageClass(),
            oldDefinition.getExecutorData().getApplyS3StorageClassAfterBytes()
        ),
        oldDefinition.getTaskDirectory(),
        oldDefinition.getExecutorPid(),
        Strings.isNullOrEmpty(oldDefinition.getServiceLogFileName()) ? cleanupConfiguration.getDefaultServiceLog() :oldDefinition.getServiceLogFileName(),
        oldDefinition.getServiceLogOutExtension(),
        Strings.isNullOrEmpty(oldDefinition.getServiceFinishedTailLogFileName()) ? cleanupConfiguration.getDefaultServiceFinishedTailLog() : oldDefinition.getServiceFinishedTailLogFileName(),
        oldDefinition.getTaskAppDirectory(),
        oldDefinition.getExecutorBashOut(),
        oldDefinition.getLogrotateStateFile(),
        oldDefinition.getSignatureVerifyOut()
    );
  }

  private boolean isDecommissioned() {
    Collection<SingularitySlave> slaves = singularityClient.getSlaves(Optional.of(MachineState.DECOMMISSIONED));
    boolean decommissioned = false;
    for (SingularitySlave slave : slaves) {
      if (slave.getHost().equals(hostname)) {
        decommissioned = true;
      }
    }
    return decommissioned;
  }

  private Set<String> getRunningTaskIds() {
    final String slaveId = mesosClient.getSlaveState(mesosClient.getSlaveUri(hostname)).getId();

    final Collection<SingularityTask> activeTasks = singularityClient.getActiveTasksOnSlave(slaveId);

    final Set<String> runningTaskIds = Sets.newHashSet();

    for (SingularityTask task : activeTasks) {
      runningTaskIds.add(task.getTaskId().getId());
    }

    return runningTaskIds;
  }

  private boolean executorStillRunning(SingularityExecutorTaskDefinition taskDefinition) {
    Optional<Integer> executorPidSafe = taskDefinition.getExecutorPidSafe();

    if (!executorPidSafe.isPresent()) {
      return false;
    }

    return processUtils.doesProcessExist(executorPidSafe.get());
  }

  private TaskCleanupResult cleanTask(SingularityExecutorTaskDefinition taskDefinition, Optional<SingularityTaskHistory> taskHistory) {
    SingularityExecutorTaskLogManager logManager = new SingularityExecutorTaskLogManager(taskDefinition, templateManager, baseConfiguration, executorConfiguration, LOG, jsonObjectFileHelper);

    SingularityExecutorTaskCleanup taskCleanup = new SingularityExecutorTaskCleanup(logManager, executorConfiguration, taskDefinition, LOG, dockerUtils);

    boolean cleanupTaskAppDirectory = !taskDefinition.getExecutorData().getPreserveTaskSandboxAfterFinish().or(Boolean.FALSE);

    if (taskHistory.isPresent()) {
      final Optional<SingularityTaskHistoryUpdate> lastUpdate = JavaUtils.getLast(taskHistory.get().getTaskUpdates());

      if (lastUpdate.isPresent() && lastUpdate.get().getTaskState().isFailed()) {
        final long delta = System.currentTimeMillis() - lastUpdate.get().getTimestamp();

        if (delta < cleanupConfiguration.getCleanupAppDirectoryOfFailedTasksAfterMillis()) {
          LOG.info("Not cleaning up task app directory of {} because only {} has elapsed since it failed (will cleanup after {})", taskDefinition.getTaskId(),
              JavaUtils.durationFromMillis(delta), JavaUtils.durationFromMillis(cleanupConfiguration.getCleanupAppDirectoryOfFailedTasksAfterMillis()));
          cleanupTaskAppDirectory = false;
        }
      }
    }

    if (taskDefinition.shouldLogrotateLogFile()) {
      checkForUncompressedLogrotatedFile(taskDefinition);
    }

    boolean isDocker = (taskHistory.isPresent()
        && taskHistory.get().getTask().getTaskRequest().getDeploy().getContainerInfo().isPresent()
        && taskHistory.get().getTask().getTaskRequest().getDeploy().getContainerInfo().get().getType() == SingularityContainerType.DOCKER);

    return taskCleanup.cleanup(cleanupTaskAppDirectory, isDocker);
  }

  private Iterator<Path> getUncompressedLogrotatedFileIterator(SingularityExecutorTaskDefinition taskDefinition) {
    final Path serviceLogOutPath = taskDefinition.getServiceLogOutPath();
    final Path parent = serviceLogOutPath.getParent();
    if (parent == null) {
      throw new IllegalStateException("Service log path " + serviceLogOutPath + " has no parent");
    }
    final Path logrotateToPath = parent.resolve(executorConfiguration.getLogrotateToDirectory());

    if (!logrotateToPath.toFile().exists() || !logrotateToPath.toFile().isDirectory()) {
      LOG.warn("Skipping uncompressed logrotated file cleanup for {} -- {} does not exist or is not a directory (task sandbox was probably garbage collected by Mesos)", taskDefinition.getTaskId(), logrotateToPath);
      return Collections.emptyIterator();
    }

    try {
      DirectoryStream<Path> dirStream = Files.newDirectoryStream(logrotateToPath, String.format("%s-*", serviceLogOutPath.getFileName()));
      return dirStream.iterator();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private void checkForUncompressedLogrotatedFile(SingularityExecutorTaskDefinition taskDefinition) {
    final Iterator<Path> iterator = getUncompressedLogrotatedFileIterator(taskDefinition);
    final Set<String> emptyPaths = new HashSet<>();
    final List<Path> uncompressedFiles = new ArrayList<>();

    // check for matched 0 byte compressed files.. and delete/compress them

    while (iterator.hasNext()) {
      Path path = iterator.next();

      final String fileName = Objects.toString(path.getFileName());
      Optional<CompressionType> maybeCompressionType = getFileCompressionType(fileName);
      if (maybeCompressionType.isPresent()) {
        try {
          if (Files.size(path) == 0) {
            Files.deleteIfExists(path);

            emptyPaths.add(fileName.substring(0, fileName.length() - maybeCompressionType.get().getExtention().length()));
          }
        } catch (IOException ioe) {
          LOG.error("Failed to handle empty {} file {}", maybeCompressionType.get(), path, ioe);
          exceptionNotifier.notify(String.format("Error handling empty file (%s)", ioe.getMessage()), ioe, ImmutableMap.of("file", path.toString()));
        }
      } else {
        uncompressedFiles.add(path);
      }
    }

    for (Path path : uncompressedFiles) {
      if (emptyPaths.contains(Objects.toString(path.getFileName()))) {
        LOG.info("Compressing abandoned file {}", path);
        try {
          new SimpleProcessManager(LOG).runCommand(ImmutableList.<String> of(cleanupConfiguration.getCompressionType().getCommand(), path.toString()));
        } catch (InterruptedException | ProcessFailedException e) {
          LOG.error("Failed to {} {}", cleanupConfiguration.getCompressionType(), path, e);
          exceptionNotifier.notify(String.format("Failed to gzip (%s)", e.getMessage()), e, ImmutableMap.of("file", path.toString()));
        }
      } else {
        LOG.debug("Didn't find matched empty {} file for {}", cleanupConfiguration.getCompressionType(), path);
      }
    }
  }

  private Optional<CompressionType> getFileCompressionType(String fileName) {
    if (fileName.endsWith(CompressionType.GZIP.getExtention())) {
      return Optional.of(CompressionType.GZIP);
    } else if (fileName.endsWith(CompressionType.BZIP2.getExtention())) {
      return Optional.of(CompressionType.BZIP2);
    } else {
      return Optional.absent();
    }
  }

  private void cleanDocker(Set<String> runningTaskIds) {
    try {
      for (Container container : dockerUtils.listContainers()) {
        for (String name : container.names()) {
          if (name.startsWith(executorConfiguration.getDockerPrefix())) {
            if (!runningTaskIds.contains(name.substring(executorConfiguration.getDockerPrefix().length()))) {
              stopContainer(container);
            }
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Could not get list of Docker containers", e);
      exceptionNotifier.notify(String.format("Error listing docker containers (%s)", e.getMessage()), e, Collections.<String, String>emptyMap());
    }
  }

  private void stopContainer(Container container) {
    try {
      ContainerInfo containerInfo = dockerUtils.inspectContainer(container.id());
      if (containerInfo.state().running()) {
        dockerUtils.stopContainer(container.id(), executorConfiguration.getDockerStopTimeout());
        LOG.debug("Forcefully stopped container {}", container.names());
      }
      dockerUtils.removeContainer(container.id(), true);
      LOG.debug("Removed container {}", container.names());
    } catch (Exception e) {
      LOG.error("Failed to stop or remove container {}", container.names(), e);
      exceptionNotifier.notify(String.format("Failed stopping container (%s)", e.getMessage()), e, Collections.<String, String>emptyMap());
    }
  }

}
