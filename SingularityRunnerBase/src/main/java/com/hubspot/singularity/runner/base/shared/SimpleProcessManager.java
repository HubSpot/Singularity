package com.hubspot.singularity.runner.base.shared;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;

public class SimpleProcessManager extends SafeProcessManager {

  public SimpleProcessManager(Logger log) {
    super(log);
  }

  public void runCommand(final List<String> command) throws InterruptedException, ProcessFailedException {
    runCommand(command, Redirect.INHERIT);
  }

  public List<String> runCommandWithOutput(final List<String> command) throws InterruptedException, ProcessFailedException {
    return runCommand(command, Redirect.PIPE);
  }

  public List<String> runCommand(final List<String> command, final Redirect redirectOutput) throws InterruptedException, ProcessFailedException {
    final ProcessBuilder processBuilder = new ProcessBuilder(command);

    Optional<Integer> exitCode = Optional.absent();

    Optional<OutputReader> reader = Optional.absent();

    try {
      processBuilder.redirectError(Redirect.INHERIT);
      processBuilder.redirectOutput(redirectOutput);

      final Process process = startProcess(processBuilder);

      if (redirectOutput == Redirect.PIPE) {
        reader = Optional.of(new OutputReader(process.getInputStream()));
        reader.get().start();
      }

      exitCode = Optional.of(process.waitFor());

      if (reader.isPresent()) {
        reader.get().join();

        if (reader.get().error.isPresent()) {
          throw reader.get().error.get();
        }
      }

    } catch (InterruptedException ie) {
      destroyProcessIfActive();

      throw ie;
    } catch (Throwable t) {
      getLog().error("Unexpected exception while running {}", getCurrentProcessToString(), t);

      destroyProcessIfActive();

      throw Throwables.propagate(t);
    } finally {
      processFinished(exitCode);
    }

    if (exitCode.isPresent() && exitCode.get() != 0) {
      throw new ProcessFailedException(String.format("Got non-zero exit code %s while running %s", exitCode, getCurrentProcessToString()));
    }

    if (!reader.isPresent()) {
      return Collections.emptyList();
    }

    return reader.get().output;
  }

  private static class OutputReader extends Thread {

    private final List<String> output;
    private final InputStream inputStream;
    private Optional<Throwable> error;

    public OutputReader(InputStream inputStream) {
      this.output = new ArrayList<>();
      this.inputStream = inputStream;
      this.error = Optional.absent();
    }

    @Override
    public void run() {
      try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
        String line = br.readLine();
        while (line != null) {
          output.add(line);
          line = br.readLine();
        }
      } catch (Throwable t) {
        this.error = Optional.of(t);
      }
    }

  }


}
