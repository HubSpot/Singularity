package com.hubspot.singularity.executor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.SingularityExecutorMonitor.KillState;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskProcessCallable;
import com.hubspot.singularity.runner.base.shared.ProcessFailedException;
import com.hubspot.singularity.runner.base.shared.SimpleProcessManager;

@Singleton
public class SingularityExecutorThreadChecker {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityExecutorThreadChecker.class);

  private final SingularityExecutorConfiguration configuration;
  private final ScheduledExecutorService scheduledExecutorService;

  private SingularityExecutorMonitor monitor;

  @Inject
  public SingularityExecutorThreadChecker(SingularityExecutorConfiguration configuration) {
    this.configuration = configuration;

    this.scheduledExecutorService = Executors.newScheduledThreadPool(configuration.getThreadCheckThreads(), new ThreadFactoryBuilder().setNameFormat("SingularityExecutorThreadCheckerThread-%d").build());
  }

  public void start(SingularityExecutorMonitor monitor) {
    LOG.info("Starting a thread checker that will run every {}", JavaUtils.durationFromMillis(configuration.getCheckThreadsEveryMillis()));

    this.monitor = monitor;

    this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

      @Override
      public void run() {
        final long start = System.currentTimeMillis();

        try {
          checkThreads();
        } catch (Throwable t) {
          LOG.error("While checking threads", t);
        } finally {
          LOG.debug("Finished checking threads after {}", JavaUtils.duration(start));
        }
      }
    }, configuration.getCheckThreadsEveryMillis(), configuration.getCheckThreadsEveryMillis(), TimeUnit.MILLISECONDS);
  }

  private void checkThreads() {
    for (SingularityExecutorTaskProcessCallable taskProcess : monitor.getRunningTasks()) {
      if (!taskProcess.getTask().getExecutorData().getMaxTaskThreads().isPresent()) {
        continue;
      }

      final int maxThreads = taskProcess.getTask().getExecutorData().getMaxTaskThreads().get();

      int usedThreads = 0;

      try {
        usedThreads = getNumUsedThreads(taskProcess);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        return;
      } catch (Throwable t) {
        taskProcess.getTask().getLog().error("While fetching used threads for {}", taskProcess.getTask().getTaskId(), t);
        continue;
      }

      if (usedThreads > maxThreads) {
        taskProcess.getTask().getLog().info("{} using too many threads: {} (max {})", taskProcess.getTask().getTaskId(), usedThreads, maxThreads);

        taskProcess.getTask().markKilledDueToThreads(usedThreads);
        KillState killState = monitor.requestKill(taskProcess.getTask().getTaskId());

        taskProcess.getTask().getLog().info("Killing {} due to thread overage (kill state {})", taskProcess.getTask().getTaskId(), killState);
      }
    }
  }

  public ExecutorService getExecutorService() {
    return scheduledExecutorService;
  }

  private int getNumUsedThreads(SingularityExecutorTaskProcessCallable taskProcess) throws InterruptedException, ProcessFailedException {
    SimpleProcessManager checkThreadsProcessManager = new SimpleProcessManager(NOPLogger.NOP_LOGGER);

    List<String> cmd = ImmutableList.of("/bin/sh",
        "-c",
        String.format("pstree %s -p | wc -l", taskProcess.getCurrentPid().get()));

    List<String> output = checkThreadsProcessManager.runCommandWithOutput(cmd);

    if (output.isEmpty()) {
      throw new ProcessFailedException("Output from ps was empty");
    }

    return Integer.parseInt(output.get(0));
  }

}
