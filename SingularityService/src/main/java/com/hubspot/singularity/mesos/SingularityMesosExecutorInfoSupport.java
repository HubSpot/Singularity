package com.hubspot.singularity.mesos;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosExecutorObject;
import com.hubspot.mesos.json.MesosSlaveFrameworkObject;
import com.hubspot.mesos.json.MesosSlaveStateObject;
import com.hubspot.mesos.json.MesosTaskObject;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;

import io.dropwizard.lifecycle.Managed;

@Singleton
public class SingularityMesosExecutorInfoSupport implements Managed {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosExecutorInfoSupport.class);

  private final MesosClient mesosClient;
  private final TaskManager taskManager;

  private final ThreadPoolExecutor logLookupExecutorService;

  @Inject
  public SingularityMesosExecutorInfoSupport(SingularityConfiguration configuration, MesosClient mesosClient, TaskManager taskManager) {
    this.mesosClient = mesosClient;
    this.taskManager = taskManager;

    this.logLookupExecutorService = JavaUtils.newFixedTimingOutThreadPool(configuration.getLogFetchMaxThreads(), TimeUnit.SECONDS.toMillis(1), "SingularityDirectoryFetcher-%d");
  }

  @Override
  public void start() {
  }

  @Override
  public void stop() {
    MoreExecutors.shutdownAndAwaitTermination(logLookupExecutorService, 1, TimeUnit.SECONDS);
  }

  private Optional<MesosExecutorObject> findExecutor(SingularityTaskId taskId, List<MesosExecutorObject> executors) {
    for (MesosExecutorObject executor : executors) {
      for (MesosTaskObject executorTask : executor.getTasks()) {
        if (taskId.getId().equals(executorTask.getId())) {
          return Optional.of(executor);
        }
      }
      for (MesosTaskObject executorTask : executor.getCompletedTasks()) {
        if (taskId.getId().equals(executorTask.getId())) {
          return Optional.of(executor);
        }
      }
    }

    return Optional.absent();
  }

  private void loadDirectoryAndContainer(SingularityTask task) {
    final long start = System.currentTimeMillis();

    final String slaveUri = mesosClient.getSlaveUri(task.getHostname());

    LOG.info("Fetching slave data to find log directory and container id for task {} from uri {}", task.getTaskId(), slaveUri);

    MesosSlaveStateObject slaveState = mesosClient.getSlaveState(slaveUri);

    Optional<String> directory = Optional.absent();
    Optional<String> containerId = Optional.absent();

    for (MesosSlaveFrameworkObject slaveFramework : slaveState.getFrameworks()) {
      Optional<MesosExecutorObject> maybeExecutor = findExecutor(task.getTaskId(), slaveFramework.getExecutors());
      if (maybeExecutor.isPresent()) {
        directory = Optional.of(maybeExecutor.get().getDirectory());
        containerId = Optional.of(maybeExecutor.get().getContainer());
        break;
      }

      maybeExecutor = findExecutor(task.getTaskId(), slaveFramework.getCompletedExecutors());
      if (maybeExecutor.isPresent()) {
        directory = Optional.of(maybeExecutor.get().getDirectory());
        containerId = Optional.of(maybeExecutor.get().getContainer());
        break;
      }
    }

    if (!directory.isPresent() && !containerId.isPresent()) {
      LOG.warn("Couldn't find matching executor for task {}", task.getTaskId());
      return;
    }

    LOG.debug("Found a directory {} and container id {} for task {}", directory.or(""), containerId.or(""), task.getTaskId());

    if (directory.isPresent()) {
      taskManager.saveTaskDirectory(task.getTaskId(), directory.get());
    }
    if (containerId.isPresent()) {
      taskManager.saveContainerId(task.getTaskId(), containerId.get());
    }

    LOG.trace("Updated task {} directory and container id in {}", task.getTaskId(), JavaUtils.duration(start));
  }

  @Timed
  public void checkDirectoryAndContainerId(final SingularityTaskId taskId) {
    final Optional<String> maybeDirectory = taskManager.getDirectory(taskId);
    final Optional<String> maybeContainerId = taskManager.getContainerId(taskId);

    if (maybeDirectory.isPresent() && maybeContainerId.isPresent()) {
      LOG.debug("Already had a directory and container id for task {}, skipping lookup", taskId);
      return;
    }

    final Optional<SingularityTask> task = taskManager.getTask(taskId);

    if (!task.isPresent()) {
      LOG.warn("No task found available for task {}, can't locate directory or container id", taskId);
      return;
    }

    Runnable cmd = generateLookupCommand(task.get());

    LOG.trace("Enqueing a request to fetch directory and container id for task: {}, current queue size: {}", taskId, logLookupExecutorService.getQueue().size());

    logLookupExecutorService.submit(cmd);
  }

  private Runnable generateLookupCommand(final SingularityTask task) {
    return new Runnable() {

      @Override
      public void run() {
        try {
          loadDirectoryAndContainer(task);
        } catch (Throwable t) {
          LOG.error("While fetching directory and container id for task: {}", task.getTaskId(), t);
        }
      }
    };
  }

}
