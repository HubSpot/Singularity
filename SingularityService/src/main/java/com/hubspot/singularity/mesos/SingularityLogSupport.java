package com.hubspot.singularity.mesos;

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
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.data.history.HistoryManager;

public class SingularityLogSupport implements SingularityCloseable {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityLogSupport.class);

  private final MesosClient mesosClient;
  private final HistoryManager historyManager;

  private final ThreadPoolExecutor logLookupExecutorService;

  private final SingularityCloser closer;

  @Inject
  public SingularityLogSupport(MesosClient mesosClient, HistoryManager historyManager, SingularityCloser closer) {
    this.mesosClient = mesosClient;
    this.historyManager = historyManager;
    this.closer = closer;

    this.logLookupExecutorService = new ThreadPoolExecutor(1, 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactoryBuilder().setNameFormat("SingularityDirectoryFetcher-%d").build());
  }

  @Override
  public void close() {
    closer.shutdown(getClass().getName(), logLookupExecutorService);
  }

  private void loadDirectory(SingularityTask task) {
    final long now = System.currentTimeMillis();

    final String slaveUri = mesosClient.getSlaveUri(task.getOffer().getHostname());

    LOG.info(String.format("Fetching slave data to find log directory for task %s from uri %s", task.getTaskId(), slaveUri));

    MesosSlaveStateObject slaveState = mesosClient.getSlaveState(slaveUri);

    String directory = null;

    for (MesosSlaveFrameworkObject slaveFramework : slaveState.getFrameworks()) {
      for (MesosExecutorObject executor : slaveFramework.getExecutors()) {
        for (MesosTaskObject executorTask : executor.getTasks()) {
          if (task.getTaskId().getId().equals(executorTask.getId())) {
            directory = executor.getDirectory();
            break;
          }
        }
      }
    }

    if (directory == null) {
      LOG.warn(String.format("Couldn't find matching executor for task %s", task.getTaskId()));
      return;
    }

    LOG.debug(String.format("Found a directory %s for task %s", directory, task.getTaskId().getId()));

    historyManager.updateTaskDirectory(task.getTaskId().getId(), directory);

    LOG.trace(String.format("Updated task directory in %sms", System.currentTimeMillis() - now));
  }

  public void checkDirectory(final SingularityTask task) {
    Optional<SingularityTaskHistory> maybeHistory = historyManager.getTaskHistory(task.getTaskId().getId());

    if (maybeHistory.isPresent() && maybeHistory.get().getDirectory().isPresent()) {
      LOG.debug(String.format("Already had a directory for task %s, skipping lookup", task.getTaskId().getId()));
      return;
    }

    Runnable cmd = new Runnable() {

      @Override
      public void run() {
        loadDirectory(task);
      }
    };

    logLookupExecutorService.submit(cmd);
  }

}
