package com.hubspot.singularity.runner.base.shared;

import java.io.IOException;
import java.lang.reflect.Field;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.hubspot.mesos.JavaUtils;

public class ProcessUtils {

  public static void sendSignal(Signal signal, Logger log, int pid) {
    final long start = System.currentTimeMillis();

    log.info("Signaling {} to process {}", signal, pid);

    final String killCmd = String.format("kill -%s %s", signal.getCode(), pid);

    try {
      int signalCode = Runtime.getRuntime().exec(killCmd).waitFor();

      log.debug("Kill signal process got exit code {} after {}", signalCode, JavaUtils.duration(start));
    } catch (InterruptedException | IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public static int getUnixPID(Process process) {
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


}
