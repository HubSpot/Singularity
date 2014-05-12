package com.hubspot.singularity.executor;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;

public class SimpleProcessManager extends SafeProcessManager {
  
  private final Path processOut;
  
  public SimpleProcessManager(Logger log, Path processOut) {
    super(log);
    this.processOut = processOut;
  }

  protected void runCommand(final List<String> command) {
    final ProcessBuilder processBuilder = new ProcessBuilder(command);

    try {
      final File outputFile = processOut.toFile();
      processBuilder.redirectError(outputFile);
      processBuilder.redirectOutput(outputFile);
      
      final Process process = startProcess(processBuilder);
      
      final int exitCode = process.waitFor();
        
      Preconditions.checkState(exitCode == 0, "Got exit code %s while running command %s", exitCode, command);
    } catch (Throwable t) {
      throw new RuntimeException(String.format("While running %s", command), t);
    } finally {
      processFinished();
    }
  }
  
}
