package com.hubspot.singularity.executor;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskProcessCallable;

@Singleton
public class SingularityExecutorProcessKiller {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SingularityExecutorProcessKiller.class);

  private final SingularityExecutorConfiguration configuration;
  private final ScheduledExecutorService scheduledExecutorService;
  private final Map<String, ScheduledFuture<?>> destroyFutures;

  @Inject
  public SingularityExecutorProcessKiller(SingularityExecutorConfiguration configuration) {
    this.configuration = configuration;

    this.destroyFutures = Maps.newConcurrentMap();
    this.scheduledExecutorService = Executors.newScheduledThreadPool(configuration.getKillThreads(), new ThreadFactoryBuilder().setNameFormat("SingularityExecutorKillThread-%d").build());
  }

  public void submitKillRequest(final SingularityExecutorTaskProcessCallable processCallable) {
    final long start = System.currentTimeMillis();

    destroyFutures.put(processCallable.getTask().getTaskId(), scheduledExecutorService.schedule(new Runnable() {
      @Override
      public void run() {
        LOG.info("Killing (-9) process {} ({}) after waiting {} (max: {})", processCallable.getTask().getTaskId(), processCallable.getCurrentPid(), JavaUtils.duration(start), JavaUtils.durationFromMillis(configuration.getHardKillAfterMillis()));
        processCallable.getTask().markDestroyedAfterWaiting();
        processCallable.signalKillToProcessIfActive();
      }
    }, processCallable.getTask().getExecutorData().getSigKillProcessesAfterMillis().or(configuration.getHardKillAfterMillis()), TimeUnit.MILLISECONDS));

    LOG.info("Signaling -15 to process {} ({})", processCallable.getTask().getTaskId(), processCallable.getCurrentPid());
    processCallable.markKilled();  // makes it so that the task can not start
    processCallable.signalTermToProcessIfActive();
  }

  public boolean isKillInProgress(String taskId) {
    return destroyFutures.get(taskId) != null;
  }

  public void cancelDestroyFuture(String taskId) {
    ScheduledFuture<?> future = destroyFutures.remove(taskId);

    if (future != null) {
      boolean canceled = future.cancel(false);
      LOG.info("Canceled kill future for {} - {}", taskId, canceled);
    } else {
      LOG.info("No kill future to cancel for {} ({} futures)", taskId, destroyFutures.size());
    }
  }

  public ExecutorService getExecutorService() {
    return scheduledExecutorService;
  }

}
