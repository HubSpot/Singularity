package com.hubspot.singularity.executor.shells;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.concurrent.Callable;

import org.slf4j.Logger;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityTaskShellCommandUpdate.UpdateType;
import com.hubspot.singularity.runner.base.shared.SafeProcessManager;

public class SingularityExecutorShellCommandRunnerCallable extends SafeProcessManager implements Callable<Integer> {
  private final SingularityExecutorShellCommandUpdater updater;
  private final ProcessBuilder processBuilder;
  private final File outputFile;

  public SingularityExecutorShellCommandRunnerCallable(Logger log, SingularityExecutorShellCommandUpdater updater, ProcessBuilder processBuilder, File outputFile) {
    super(log);
    this.processBuilder = processBuilder;
    this.updater = updater;
    this.outputFile = outputFile;
  }

  @Override
  public Integer call() throws Exception {
    try (final PrintWriter outputWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile, true), Charsets.UTF_8))) {
      outputWriter.println("#");
      outputWriter.println(String.format("# %s -- Launching %s", new Date(), JavaUtils.SPACE_JOINER.join(processBuilder.command())));
      outputWriter.println("#");
    }

    Process process = startProcess(processBuilder);

    Optional<Integer> pid = getCurrentPid();

    updater.sendUpdate(UpdateType.STARTED, Optional.of(String.format("pid - %s", pid.orNull())), Optional.<String>absent());

    try {
      return process.waitFor();
    } catch (InterruptedException ie) {
      signalKillToProcessIfActive();
      throw ie;
    }
  }

}
