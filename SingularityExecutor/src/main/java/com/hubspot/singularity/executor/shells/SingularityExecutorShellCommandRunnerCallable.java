package com.hubspot.singularity.executor.shells;

import java.util.concurrent.Callable;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityTaskShellCommandUpdate.UpdateType;
import com.hubspot.singularity.runner.base.shared.SafeProcessManager;

public class SingularityExecutorShellCommandRunnerCallable extends SafeProcessManager implements Callable<Integer> {

  private final SingularityExecutorShellCommandRunner runner;

  public SingularityExecutorShellCommandRunnerCallable(SingularityExecutorShellCommandRunner runner) {
    super(runner.getTask().getLog());
    this.runner = runner;
  }

  @Override
  public Integer call() throws Exception {
    Process process = startProcess(runner.buildProcessBuilder());

    Optional<Integer> pid = getCurrentPid();

    runner.sendUpdate(UpdateType.STARTED, Optional.of(String.format("pid - %s", pid.orNull())));

    try {
      return process.waitFor();
    } catch (InterruptedException ie) {
      signalKillToProcessIfActive();
      throw ie;
    }
  }

}
