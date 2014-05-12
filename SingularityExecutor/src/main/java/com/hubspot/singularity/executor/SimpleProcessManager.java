package com.hubspot.singularity.executor;

import java.util.List;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;

public class SimpleProcessManager extends SafeProcessManager {
  
  public SimpleProcessManager(Logger log) {
    super(log);
  }

  protected void runCommand(final List<String> command) {
    final ProcessBuilder processBuilder = new ProcessBuilder(command);

    try {
      processBuilder.inheritIO();
      
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
