package com.hubspot.singularity.executor.shells;

import java.util.concurrent.Callable;

import org.slf4j.Logger;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityTaskShellCommandUpdate.UpdateType;
import com.hubspot.singularity.runner.base.shared.SafeProcessManager;

public class SingularityExecutorShellCommandRunnerCallable extends SafeProcessManager implements Callable<Integer> {

  private final SingularityExecutorShellCommandUpdater updater;
  private final ProcessBuilder processBuilder;

  public SingularityExecutorShellCommandRunnerCallable(Logger log, SingularityExecutorShellCommandUpdater updater, ProcessBuilder processBuilder) {
    super(log);
    this.processBuilder = processBuilder;
    this.updater = updater;
  }

  @Override
  public Integer call() throws Exception {
    Process process = startProcess(processBuilder);

    Optional<Integer> pid = getCurrentPid();

    updater.sendUpdate(UpdateType.STARTED, Optional.of(String.format("pid - %s", pid.orNull())));

    try {
      return process.waitFor();
    } catch (InterruptedException ie) {
      signalKillToProcessIfActive();
      throw ie;
    }
  }

}
