package com.hubspot.singularity.executor;

import java.lang.ProcessBuilder.Redirect;
import java.util.List;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;

public class SimpleProcessManager extends SafeProcessManager {
  
  public SimpleProcessManager(Logger log) {
    super(log);
  }

  protected void runCommand(final List<String> command) {
    runCommand(command, Redirect.INHERIT);
  }
  
  protected void runCommand(final List<String> command, final Redirect redirectOutput) {
    final ProcessBuilder processBuilder = new ProcessBuilder(command);

    int exitCode = 0;
    
    try {
      processBuilder.redirectError(Redirect.INHERIT);
      processBuilder.redirectOutput(redirectOutput);
      
      final Process process = startProcess(processBuilder);
      
      exitCode = process.waitFor();
    } catch (Throwable t) {
      throw new RuntimeException(String.format("While running %s", command), t);
    } finally {
      processFinished();
    }
    
    Preconditions.checkState(exitCode == 0, "Got exit code %s while running command %s", exitCode, command);
  }
  
}
