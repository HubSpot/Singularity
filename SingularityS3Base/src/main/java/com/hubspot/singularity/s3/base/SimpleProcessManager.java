package com.hubspot.singularity.s3.base;

import java.lang.ProcessBuilder.Redirect;
import java.util.List;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

public class SimpleProcessManager extends SafeProcessManager {

  public SimpleProcessManager(Logger log) {
    super(log);
  }

  protected void runCommand(final List<String> command) {
    runCommand(command, Redirect.INHERIT);
  }

  protected void runCommand(final List<String> command, final Redirect redirectOutput) {
    final ProcessBuilder processBuilder = new ProcessBuilder(command);

    Process process = null;

    try {
      processBuilder.redirectError(Redirect.INHERIT);
      processBuilder.redirectOutput(redirectOutput);

      process = startProcess(processBuilder);
    } catch (Throwable t) {
      getLog().error("SimpleProcessManager caught unexpected exception while starting {}", command, t);

      destroyProcessIfActive();

      throw Throwables.propagate(t);
    }

    int exitCode = 0;

    try {
      exitCode = runLoop(process, command.get(0));

      processFinished(exitCode);
    } catch (Throwable t) {
      getLog().error("SimpleProcessManager caught unexpected exception while running {}", command, t);

      destroyProcessIfActive();

      throw Throwables.propagate(t);
    }

    Preconditions.checkState(exitCode == 0, "Got non-zero exit code %s while running %s", exitCode, command);
  }

  private int runLoop(Process process, String command) {
    try {
      return process.waitFor();
    } catch (InterruptedException ie) {
      getLog().warn("SimpleProcessManager runLoop() for {} caught interrupted exception", command);
      Thread.currentThread().interrupt();
      return 1;
    }
  }

}
