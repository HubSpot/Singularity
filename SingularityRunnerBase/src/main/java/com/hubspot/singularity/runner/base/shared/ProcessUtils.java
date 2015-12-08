package com.hubspot.singularity.runner.base.shared;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.hubspot.mesos.JavaUtils;

public class ProcessUtils {

  private static final Logger LOG = LoggerFactory.getLogger(ProcessUtils.class);

  private final Optional<Logger> log;

  public ProcessUtils() {
    this(null);
  }

  public ProcessUtils(@Nullable Logger log) {
    this.log = Optional.fromNullable(log);
  }

  public static class ProcessResult {

    private final int exitCode;
    private final String output;

    public ProcessResult(int exitCode, String output) {
      this.exitCode = exitCode;
      this.output = output;
    }

    public int getExitCode() {
      return exitCode;
    }

    public String getOutput() {
      return output;
    }

    @Override
    public String toString() {
      return "ProcessResult [exitCode=" + exitCode + ", output=" + output + "]";
    }

  }

  public ProcessResult sendSignal(Signal signal, int pid) {
    final long start = System.currentTimeMillis();

    if (log.isPresent()) {
      final String logLine = String.format("Signaling %s (%s) to process %s", signal, signal.getCode(), pid);
      if (signal == Signal.CHECK) {
        log.get().trace(logLine);
      } else {
        log.get().info(logLine);
      }
    }

    if (signal != Signal.CHECK) {
      LOG.debug("Signaling {} ({}) to process {}", signal, signal.getCode(), pid);
    }

    try {
      final ProcessBuilder pb = new ProcessBuilder("kill", String.format("-%s", signal.getCode()), Integer.toString(pid));
      pb.redirectErrorStream(true);

      final Process p = pb.start();

      final int exitCode = p.waitFor();

      final String output = CharStreams.toString(new InputStreamReader(p.getInputStream(), Charsets.UTF_8));

      Closeables.closeQuietly(p.getInputStream());

      if (log.isPresent()) {
        log.get().trace("Kill signal process for {} got exit code {} after {}", pid, exitCode, JavaUtils.duration(start));
      }

      return new ProcessResult(exitCode, output.trim());
    } catch (InterruptedException | IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public int getUnixPID(Process process) {
    Preconditions.checkArgument(process.getClass().getName().equals("java.lang.UNIXProcess"));

    Class<?> clazz = process.getClass();

    try {
      Field field = clazz.getDeclaredField("pid");
      field.setAccessible(true);
      Object pidObject = field.get(process);
      return (Integer) pidObject;
    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
      throw Throwables.propagate(e);
    }
  }

  public boolean doesProcessExist(int pid) {
    ProcessResult processResult = sendSignal(Signal.CHECK, pid);
    if (processResult.getExitCode() != 0 && processResult.output.contains("No such process")) {
      return false;
    }
    return true;
  }


}
