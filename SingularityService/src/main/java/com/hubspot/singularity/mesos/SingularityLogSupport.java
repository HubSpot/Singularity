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
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.history.HistoryManager;

public class SingularityLogSupport implements SingularityCloseable {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityLogSupport.class);

  private final MesosClient mesosClient;
  private final HistoryManager historyManager;

  private final ThreadPoolExecutor logLookupExecutorService;

  private final SingularityCloser closer;

  @Inject
  public SingularityLogSupport(SingularityConfiguration configuration, MesosClient mesosClient, HistoryManager historyManager, SingularityCloser closer) {
    this.mesosClient = mesosClient;
    this.historyManager = historyManager;
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
    }
  
    return Optional.absent();
  }

  private void loadDirectory(SingularityTaskId taskId, SingularityTaskHistory history) {
    final long now = System.currentTimeMillis();
    
    final String slaveUri = mesosClient.getSlaveUri(history.getTask().getOffer().getHostname());

    LOG.info(String.format("Fetching slave data to find log directory for task %s from uri %s", taskId.getId(), slaveUri));

    MesosSlaveStateObject slaveState = mesosClient.getSlaveState(slaveUri);

    Optional<String> directory = null;

    for (MesosSlaveFrameworkObject slaveFramework : slaveState.getFrameworks()) {
      directory = findDirectory(taskId, slaveFramework.getExecutors());
      if (!directory.isPresent()) {
        directory = findDirectory(taskId, slaveFramework.getCompletedExecutors());
      }
    }
   
    if (!directory.isPresent()) {
      LOG.warn(String.format("Couldn't find matching executor for task %s", taskId.getId()));
      return;
    }

    LOG.debug(String.format("Found a directory %s for task %s", directory, taskId.getId()));

    historyManager.updateTaskDirectory(taskId.getId(), directory.get());

    LOG.trace(String.format("Updated task directory in %sms", System.currentTimeMillis() - now));
  }

  public void checkDirectory(final SingularityTaskId taskId) {
    final Optional<SingularityTaskHistory> maybeHistory = historyManager.getTaskHistory(taskId.getId(), false);

    if (maybeHistory.isPresent() && maybeHistory.get().getDirectory().isPresent()) {
      LOG.debug(String.format("Already had a directory for task %s, skipping lookup", taskId.getId()));
      return;
    } else if (!maybeHistory.isPresent()) {
      LOG.warn(String.format("No history available for task %s, can't locate directory", taskId.getId()));
      return;
    }

    Runnable cmd = new Runnable() {

      @Override
      public void run() {
        try {
          loadDirectory(taskId, maybeHistory.get());
        } catch (Throwable t) {
          LOG.error(String.format("While fetching directory for task: %s", taskId, t));
        }
      }
    };

    LOG.trace(String.format("Enqueing a request to fetch directory for task: %s, current queue size: %s", taskId, logLookupExecutorService.getQueue().size()));
  
    logLookupExecutorService.submit(cmd);
  }

}
