package com.hubspot.singularity.executor.cleanup;

import java.io.IOException;
import java.net.SocketException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.client.SingularityClient;
import com.hubspot.singularity.client.SingularityClientException;
import com.hubspot.singularity.executor.SingularityExecutorCleanupStatistics;
import com.hubspot.singularity.executor.SingularityExecutorCleanupStatistics.SingularityExecutorCleanupStatisticsBuilder;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.cleanup.config.SingularityExecutorCleanupConfiguration;
import com.hubspot.singularity.executor.cleanup.config.SingularityExecutorCleanupConfigurationLoader;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskCleanup;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskDefinition;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskLogManager;
import com.hubspot.singularity.runner.base.shared.JsonObjectFileHelper;

public class SingularityExecutorCleanup {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityExecutorCleanup.class);

  private final JsonObjectFileHelper jsonObjectFileHelper;
  private final SingularityExecutorConfiguration executorConfiguration;
  private final SingularityClient singularityClient;
  private final TemplateManager templateManager;
  private final SingularityExecutorCleanupConfiguration cleanupConfiguration;

  @Inject
  public SingularityExecutorCleanup(SingularityClient singularityClient, JsonObjectFileHelper jsonObjectFileHelper, SingularityExecutorConfiguration executorConfiguration, SingularityExecutorCleanupConfiguration cleanupConfiguration, TemplateManager templateManager) {
    this.jsonObjectFileHelper = jsonObjectFileHelper;
    this.executorConfiguration = executorConfiguration;
    this.cleanupConfiguration = cleanupConfiguration;
    this.singularityClient = singularityClient;
    this.templateManager = templateManager;
  }

  public SingularityExecutorCleanupStatistics clean() {
    final SingularityExecutorCleanupStatisticsBuilder statisticsBldr = new SingularityExecutorCleanupStatisticsBuilder();
    final Path directory = Paths.get(executorConfiguration.getGlobalTaskDefinitionDirectory());

    Set<String> runningTaskIds = null;

    try {
      runningTaskIds = getRunningTaskIds();
    } catch (SingularityClientException sce) {
      LOG.error("While fetching running tasks from singularity", sce);
      statisticsBldr.setErrorMessage(sce.getMessage());
      return statisticsBldr.build();
    }

    LOG.info("Found {} running tasks from Mesos", runningTaskIds);

    statisticsBldr.setMesosRunningTasks(runningTaskIds.size());

    if (runningTaskIds.isEmpty()) {
      if (cleanupConfiguration.isSafeModeWontRunWithNoTasks()) {
        final String errorMessage = String.format("Running in safe mode (%s) and found 0 running tasks - aborting cleanup", SingularityExecutorCleanupConfigurationLoader.SAFE_MODE_WONT_RUN_WITH_NO_TASKS);
        LOG.error(errorMessage);
        statisticsBldr.setErrorMessage(errorMessage);
        return statisticsBldr.build();
      } else {
        LOG.warn("Found 0 running tasks - proceeding with cleanup as we are not in safe mode ({})", SingularityExecutorCleanupConfigurationLoader.SAFE_MODE_WONT_RUN_WITH_NO_TASKS);
      }
    }

    for (Path file : JavaUtils.iterable(directory)) {
      if (!file.getFileName().toString().endsWith(executorConfiguration.getGlobalTaskDefinitionSuffix())) {
        LOG.debug("Ignoring file {} that doesn't have suffix {}", file, executorConfiguration.getGlobalTaskDefinitionSuffix());
        statisticsBldr.incrInvalidTasks();
        continue;
      }

      statisticsBldr.incrTotalTaskFiles();

      try {
        Optional<SingularityExecutorTaskDefinition> taskDefinition = jsonObjectFileHelper.read(file, LOG, SingularityExecutorTaskDefinition.class);

        if (!taskDefinition.isPresent()) {
          statisticsBldr.incrInvalidTasks();
          continue;
        }

        final String taskId = taskDefinition.get().getTaskId();

        if (runningTaskIds.contains(taskId)) {
          statisticsBldr.incrRunningTasksIgnored();
          continue;
        }

        Optional<SingularityTaskHistory> taskHistory = null;

        try {
          taskHistory = singularityClient.getHistoryForTask(taskId);
        } catch (SingularityClientException sce) {
          LOG.error("While fetching history for {}", taskId, sce);
          statisticsBldr.incrErrorTasks();
          continue;
        }

        if (cleanTask(taskDefinition.get(), taskHistory)) {
          statisticsBldr.incrSuccessfullyCleanedTasks();
        } else {
          statisticsBldr.incrErrorTasks();
        }

      } catch (IOException ioe) {
        LOG.error("Couldn't read file {}", file, ioe);

        statisticsBldr.incrIoErrorTasks();
      }
    }

    return statisticsBldr.build();
  }

  private Collection<SingularityTask> getActiveTasksOnSlave() {
    try {
      return singularityClient.getActiveTasks(JavaUtils.getHostAddress());
    } catch (SocketException e) {
      throw Throwables.propagate(e);
    }
  }

  private Set<String> getRunningTaskIds() {
    final Collection<SingularityTask> activeTasks = getActiveTasksOnSlave();

    final Set<String> runningTaskIds = Sets.newHashSet();

    for (SingularityTask task : activeTasks) {
      runningTaskIds.add(task.getTaskId().getId());
    }

    return runningTaskIds;
  }

  private boolean cleanTask(SingularityExecutorTaskDefinition taskDefinition, Optional<SingularityTaskHistory> taskHistory) {
    SingularityExecutorTaskLogManager logManager = new SingularityExecutorTaskLogManager(taskDefinition, templateManager, executorConfiguration, LOG, jsonObjectFileHelper);

    SingularityExecutorTaskCleanup taskCleanup = new SingularityExecutorTaskCleanup(logManager, executorConfiguration, taskDefinition, LOG);

    boolean cleanupTaskAppDirectory = true;

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

    return taskCleanup.cleanup(cleanupTaskAppDirectory);
  }

}
