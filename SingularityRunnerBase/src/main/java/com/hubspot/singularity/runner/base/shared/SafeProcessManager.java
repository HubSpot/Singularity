package com.hubspot.singularity.runner.base.shared;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.hubspot.mesos.JavaUtils;

public abstract class SafeProcessManager {

  private final Logger log;
  private final Lock processLock;

  // unnecessary code here : we shouldn't really subclass this when all we need is the following
  // (1) artifact manager needs to kill to ensure it doesn't start something after killing.
  // (2) process lock is unnecessary since we don't need to run multiple processes except in artifact manager
  // (3) we could probably hand off these process variables to make them final in some other object.

  private volatile Optional<String> currentProcessCmd;
  private volatile Optional<Process> currentProcess;
  private volatile Optional<Integer> currentProcessPid;
  private volatile Optional<Long> currentProcessStart;

  private final AtomicBoolean killed;
  private final ProcessUtils processUtils;

  public SafeProcessManager(Logger log) {
    this.log = log;
    this.processUtils = new ProcessUtils(log);

    this.currentProcessCmd = Optional.absent();
    this.currentProcess = Optional.absent();
    this.currentProcessPid = Optional.absent();

    this.processLock = new ReentrantLock();

    this.killed = new AtomicBoolean(false);
  }

  public Logger getLog() {
    return log;
  }

  public boolean wasKilled() {
    return killed.get();
  }

  public void markKilled() {
    this.processLock.lock();

    try {
      killed.set(true);
    } finally {
      this.processLock.unlock();
    }
  }

  private void lockInterruptibly() {
    try {
      this.processLock.lockInterruptibly();
    } catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
  }

  public Process startProcess(ProcessBuilder builder) {
    final String cmd = builder.command().get(0);

    log.debug("Starting process {}", Joiner.on(" ").join(builder.command()));

    processLock.lock();

    Process process = null;

    try {
      Preconditions.checkState(!killed.get(), "Can not start new process, killed is set");
      Preconditions.checkState(!currentProcess.isPresent(), "Can not start new process, already had process");

      currentProcessStart = Optional.of(System.currentTimeMillis());

      process = builder.start();

      currentProcessPid = Optional.of(processUtils.getUnixPID(process));
      currentProcess = Optional.of(process);
      currentProcessCmd = Optional.of(cmd);

      log.debug("Started process {}", getCurrentProcessToString());

    } catch (IOException e) {
      throw Throwables.propagate(e);
    } finally {
      processLock.unlock();
    }

    return process;
  }

  private void resetCurrentVariables() {
    currentProcess = Optional.absent();
    currentProcessPid = Optional.absent();
    currentProcessCmd = Optional.absent();
    currentProcessStart = Optional.absent();
  }

  protected void processFinished(Optional<Integer> exitCode) {
    lockInterruptibly();

    try {
      if (currentProcessCmd.isPresent() && currentProcessStart.isPresent()) {
        if (exitCode.isPresent()) {
          log.debug("Process {} exited with {} after {}", currentProcessCmd.get(), exitCode.get(), JavaUtils.duration(currentProcessStart.get()));
        } else {
          log.debug("Process {} abandoned after {}", currentProcessCmd.get(), JavaUtils.duration(currentProcessStart.get()));
        }
      } else {
        log.warn("Process finished called on an empty process manager");
      }

      resetCurrentVariables();
    } finally {
      processLock.unlock();
    }
  }

  public Optional<Integer> getCurrentPid() {
    return currentProcessPid;
  }

  public String getCurrentProcessToString() {
    return String.format("%s - (pid: %s)", currentProcessCmd.or("<none>"), currentProcessPid.or(0));
  }

  public void signalTermToProcessIfActive() {
    this.processLock.lock();

    try {
      if (currentProcessPid.isPresent()) {
        processUtils.sendSignal(Signal.SIGTERM, currentProcessPid.get());
      }
    } finally {
      this.processLock.unlock();
    }
  }

  public void signalKillToProcessIfActive() {
    this.processLock.lock();

    try {
      if (currentProcess.isPresent()) {
        processUtils.sendSignal(Signal.SIGKILL, currentProcessPid.get());
      }
    } finally {
      this.processLock.unlock();
    }
  }


}
