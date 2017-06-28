package com.hubspot.singularity.executor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.SingularityExecutorMonitor.KillState;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.models.ThreadCheckerType;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskProcessCallable;
import com.hubspot.singularity.executor.utils.DockerUtils;
import com.hubspot.singularity.runner.base.shared.ProcessFailedException;
import com.hubspot.singularity.runner.base.shared.SimpleProcessManager;
import com.spotify.docker.client.exceptions.DockerException;

@Singleton
public class SingularityExecutorThreadChecker {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityExecutorThreadChecker.class);

  private static Pattern CGROUP_CPU_REGEX = Pattern.compile("^\\d+:cpu:/(.*)$");
  private static Pattern PROC_STATUS_THREADS_REGEX = Pattern.compile("Threads:\\s*(\\d+)\\s*$");

  private final SingularityExecutorConfiguration configuration;
  private final ScheduledExecutorService scheduledExecutorService;
  private final DockerUtils dockerUtils;

  private SingularityExecutorMonitor monitor;

  @Inject
  public SingularityExecutorThreadChecker(SingularityExecutorConfiguration configuration, DockerUtils dockerUtils) {
    this.configuration = configuration;
    this.dockerUtils = dockerUtils;

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
          LOG.trace("Finished checking threads after {}", JavaUtils.duration(start));
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
        LOG.trace("{} is using {} threads", taskProcess.getTask().getTaskId(), usedThreads);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        return;
      } catch (Throwable t) {
        if (!taskProcess.wasKilled()) {
          taskProcess.getTask().getLog().error("While fetching used threads for {}", taskProcess.getTask().getTaskId(), t);
        }
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
    Optional<Integer> dockerPid = Optional.absent();
    if (taskProcess.getTask().getTaskInfo().hasContainer() && taskProcess.getTask().getTaskInfo().getContainer().hasDocker()) {
      try {
        String containerName = String.format("%s%s", configuration.getDockerPrefix(), taskProcess.getTask().getTaskId());
        int possiblePid = dockerUtils.getPid(containerName);
        if (possiblePid == 0) {
          LOG.warn(String.format("Container %s has pid %s (running: %s). Defaulting to 0 threads running.", containerName, possiblePid, dockerUtils.isContainerRunning(containerName)));
          return 0;
        } else {
          dockerPid = Optional.of(possiblePid);
        }
      } catch (DockerException e) {
        throw new ProcessFailedException("Could not get docker root pid due to error", e);
      }
    }

    try {
      Optional<Integer> numThreads = getNumThreads(configuration.getThreadCheckerType(), taskProcess, dockerPid);
      if (numThreads.isPresent()) {
        return numThreads.get();
      } else {
        LOG.warn("Could not get num threads using {} thread checker", configuration.getThreadCheckerType());
        return 0;
      }
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private Optional<Integer> getNumThreads(ThreadCheckerType type, SingularityExecutorTaskProcessCallable taskProcess, Optional<Integer> dockerPid) throws InterruptedException, ProcessFailedException, IOException {
    Optional<Integer> numThreads;
    switch (type) {
      case CGROUP:
        numThreads = getNumThreadsFromCgroup(taskProcess, dockerPid);
        break;
      case PS:
        numThreads = getNumThreadsFromCommand(taskProcess, dockerPid, "ps hH p %s | wc -l");
        break;
      case PROC_STATUS:
      default:
        numThreads = getNumThreadsFromProcStatus(taskProcess, dockerPid);
        break;
    }
    return numThreads;
  }

  private Optional<Integer> getNumThreadsFromCommand(SingularityExecutorTaskProcessCallable taskProcess, Optional<Integer> dockerPid, String commandFormat) throws InterruptedException, ProcessFailedException {
    SimpleProcessManager checkThreadsProcessManager = new SimpleProcessManager(NOPLogger.NOP_LOGGER);
    List<String> cmd = ImmutableList.of("/bin/sh", "-c", String.format(commandFormat, dockerPid.or(taskProcess.getCurrentPid().get())));
    List<String> output = checkThreadsProcessManager.runCommandWithOutput(cmd);
    if (output.isEmpty()) {
      LOG.warn("Output from ls was empty ({})", cmd);
      return Optional.absent();
    } else {
      return Optional.of(Integer.parseInt(output.get(0)));
    }
  }

  private Optional<Integer> getNumThreadsFromProcStatus(SingularityExecutorTaskProcessCallable taskProcess, Optional<Integer> dockerPid) throws InterruptedException, IOException {
    final Path procStatusPath = Paths.get(String.format("/proc/%s/status", dockerPid.or(taskProcess.getCurrentPid().get())));
    if (Files.exists(procStatusPath)) {
      for (String line : Files.readAllLines(procStatusPath, Charsets.UTF_8)) {
        final Matcher matcher = PROC_STATUS_THREADS_REGEX.matcher(line);
        if (matcher.matches()) {
          return Optional.of(Integer.parseInt(matcher.group(1)));
        }
      }
      LOG.warn("Unable to parse threads from proc status file {}", procStatusPath);
      return Optional.absent();
    } else {
      LOG.warn("Proc status file does not exist for pid {}", dockerPid.or(taskProcess.getCurrentPid().get()));
      return Optional.absent();
    }
  }

  private Optional<Integer> getNumThreadsFromCgroup(SingularityExecutorTaskProcessCallable taskProcess, Optional<Integer> dockerPid) throws InterruptedException, IOException {
    final Path procCgroupPath = Paths.get(String.format(configuration.getProcCgroupFormat(), dockerPid.or(taskProcess.getCurrentPid().get())));
    if (Files.exists(procCgroupPath)) {
      for (String line : Files.readAllLines(procCgroupPath, Charsets.UTF_8)) {
        final Matcher matcher = CGROUP_CPU_REGEX.matcher(line);
        if (matcher.matches()) {
          return Optional.of(Files.readAllLines(Paths.get(String.format(configuration.getCgroupsMesosCpuTasksFormat(), matcher.group(1))), Charsets.UTF_8).size());
        }
      }
      LOG.warn("Unable to parse cgroup container from {}", procCgroupPath.toString());
      return Optional.absent();
    } else {
      LOG.warn("cgroup {} does not exist", procCgroupPath.toString());
      return Optional.absent();
    }
  }

}
