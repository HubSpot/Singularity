package com.hubspot.singularity.mesos;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosExecutorObject;
import com.hubspot.mesos.json.MesosSlaveFrameworkObject;
import com.hubspot.mesos.json.MesosSlaveStateObject;
import com.hubspot.mesos.json.MesosTaskObject;
import com.hubspot.singularity.SingularityCloseable;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.Utils;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;

public class SingularityLogSupport implements SingularityCloseable {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityLogSupport.class);

  private final MesosClient mesosClient;
  private final TaskManager taskManager;
  
  private final ThreadPoolExecutor logLookupExecutorService;
  
  private final SingularityCloser closer;

  @Inject
  public SingularityLogSupport(SingularityConfiguration configuration, MesosClient mesosClient, TaskManager taskManager, SingularityCloser closer) {
    this.mesosClient = mesosClient;
    this.taskManager = taskManager;
    this.closer = closer;

    this.logLookupExecutorService = new ThreadPoolExecutor(configuration.getLogFetchCoreThreads(), configuration.getLogFetchMaxThreads(), 250L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactoryBuilder().setNameFormat("SingularityDirectoryFetcher-%d").build());
  }

  @Override
  public void close() {
    closer.shutdown(getClass().getName(), logLookupExecutorService);
  }
  
  private Optional<String> findDirectory(SingularityTaskId taskId, List<MesosExecutorObject> executors) {
    for (MesosExecutorObject executor : executors) {
      for (MesosTaskObject executorTask : executor.getTasks()) {
        if (taskId.getId().equals(executorTask.getId())) {
          return Optional.of(executor.getDirectory());
        }
      }
      for (MesosTaskObject executorTask : executor.getCompletedTasks()) {
        if (taskId.getId().equals(executorTask.getId())) {
          return Optional.of(executor.getDirectory());
        }
      }
    }
  
    return Optional.absent();
  }

  private void loadDirectory(SingularityTask task) {
    final long start = System.currentTimeMillis();
    
    final String slaveUri = mesosClient.getSlaveUri(task.getOffer().getHostname());

    LOG.info("Fetching slave data to find log directory for task {} from uri {}", task.getTaskId(), slaveUri);

    MesosSlaveStateObject slaveState = mesosClient.getSlaveState(slaveUri);

    Optional<String> directory = null;

    for (MesosSlaveFrameworkObject slaveFramework : slaveState.getFrameworks()) {
      directory = findDirectory(task.getTaskId(), slaveFramework.getExecutors());
      if (directory.isPresent()) {
        break;
      }
      
      directory = findDirectory(task.getTaskId(), slaveFramework.getCompletedExecutors());
      if (directory.isPresent()) {
        break;
      }
    }
   
    if (!directory.isPresent()) {
      LOG.warn("Couldn't find matching executor for task {}", task.getTaskId());
      return;
    }

    LOG.debug("Found a directory {} for task {}", directory.get(), task.getTaskId());

    taskManager.updateTaskDirectory(task.getTaskId(), directory.get());
    
    LOG.trace("Updated task {} directory in {}", task.getTaskId(), Utils.duration(start));
  }

  public void checkDirectory(final SingularityTaskId taskId) {
    final Optional<String> maybeDirectory = taskManager.getDirectory(taskId);
    
    if (maybeDirectory.isPresent()) {
      LOG.debug("Already had a directory for task {}, skipping lookup", taskId);
      return;
    }
    
    final Optional<SingularityTask> task = taskManager.getTask(taskId);
    
    if (!task.isPresent()) {
      LOG.warn("No task found available for task {}, can't locate directory", taskId);
      return;
    }
    
    Runnable cmd = generateLookupCommand(task.get());

    LOG.trace("Enqueing a request to fetch directory for task: {}, current queue size: {}", taskId, logLookupExecutorService.getQueue().size());
  
    logLookupExecutorService.submit(cmd);
  }
  
  private Runnable generateLookupCommand(final SingularityTask task) {
    return new Runnable() {

      @Override
      public void run() {
        try {
          loadDirectory(task);
        } catch (Throwable t) {
          LOG.error("While fetching directory for task: {}", task.getTaskId(), t);
        }
      }
    };
  }

}
