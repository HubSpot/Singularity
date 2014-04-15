package com.hubspot.singularity.executor.utils;

import java.io.IOException;
import java.lang.reflect.Field;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

public class ProcessUtils {

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
  
  public static int gracefulKillUnixProcess(Process process) {
    final int pid = getUnixPID(process);
    final String killCmd = String.format("kill -15 %s", pid); 
    
    try {
      return Runtime.getRuntime().exec(killCmd).waitFor();
    } catch (InterruptedException | IOException e) {
      throw Throwables.propagate(e);
    }
  }

}
