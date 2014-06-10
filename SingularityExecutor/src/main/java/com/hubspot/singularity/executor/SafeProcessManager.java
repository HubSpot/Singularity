package com.hubspot.singularity.executor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.hubspot.mesos.JavaUtils;

public abstract class SafeProcessManager {
  
  private final Logger log;
  private final Lock processLock;
  
  private volatile Optional<String> currentProcessCmd;
  private volatile Optional<Process> currentProcess;
  private volatile Optional<Integer> currentProcessPid;
  private volatile Optional<Long> currentProcessStart;
  
  private final AtomicBoolean killed;
  
  public enum Signal {
    SIGTERM(15), SIGKILL(9);
    
    private final int code;

    private Signal(int code) {
      this.code = code;
    }

    public int getCode() {
      return code;
    }
    
  }
  
  public SafeProcessManager(Logger log) {
    this.log = log;
    
    this.currentProcessCmd = Optional.absent();
    this.currentProcess = Optional.absent();
    this.currentProcessPid = Optional.absent();
    
    this.processLock = new ReentrantLock();
  
    this.killed = new AtomicBoolean(false);
  }
    
  public boolean wasKilled() {
    return killed.get();
  }
  
  public void markKilled() {
    this.processLock.lock();
    
    try {
      killed.set(true);
    } finally {
      this.processLock.unlock();
    }
  }
  
  private void lockInterruptibly() {
    try {
      this.processLock.lockInterruptibly();
    } catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
  }
  
  public Process startProcess(ProcessBuilder builder) {
    String cmd = builder.command().get(0);
    
    log.debug("Starting process {}", cmd);
    
    processLock.lock();
    
    Preconditions.checkState(!killed.get(), "Can not start new process, killed is set");
    Preconditions.checkState(!currentProcess.isPresent(), "Can not start new process, already had process");
    
    Process process = null;
    
    try {
      currentProcessStart = Optional.of(System.currentTimeMillis());
      
      process = builder.start();
      
      currentProcessPid = Optional.of(getUnixPID(process));
      currentProcess = Optional.of(process);
      currentProcessCmd = Optional.of(cmd);
      
      log.debug("Started process {}", getCurrentProcessToString());
      
    } catch (IOException e) {
      throw Throwables.propagate(e);
    } finally {
      processLock.unlock();
    }
    
    return process;
  }
  
  private void resetCurrentVariables() {
    currentProcess = Optional.absent();
    currentProcessPid = Optional.absent();
    currentProcessCmd = Optional.absent();
    currentProcessStart = Optional.absent();
  }
  
  public void processFinished() {
    lockInterruptibly();
    
    if (currentProcessCmd.isPresent() && currentProcessStart.isPresent()) {
      log.debug("Process {} finished after {}", currentProcessCmd.get(), JavaUtils.duration(currentProcessStart.get()));
    }
    
    try {
      resetCurrentVariables();
    } finally {
      processLock.unlock();
    }
  }
  
  private int getUnixPID(Process process) {
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
  
  private void sendSignal(Signal signal) {
    final long start = System.currentTimeMillis();
    
    log.info("Signaling {} to process {}", signal, getCurrentProcessToString());
    
    final String killCmd = String.format("kill -%s %s", signal.getCode(), currentProcessPid.get()); 
    
    try {
      int signalCode = Runtime.getRuntime().exec(killCmd).waitFor();

      log.debug("Kill signal process got exit code {} after {}", signalCode, JavaUtils.duration(start));
    } catch (InterruptedException | IOException e) {
      throw Throwables.propagate(e);
    }
  }
  
  public String getCurrentProcessToString() {
    return String.format("%s - (pid: %s)", currentProcessCmd.or("<none>"), currentProcessPid.or(0));
  }
  
  public void signalProcessIfActive() {
    this.processLock.lock();
    
    try {
      if (currentProcessPid.isPresent()) {
        sendSignal(Signal.SIGTERM);
      }
    } finally {
      this.processLock.unlock();
    }
  }
  
  public void destroyProcessIfActive() {
    this.processLock.lock();
    
    try {
      if (currentProcess.isPresent()) {
        sendSignal(Signal.SIGKILL);
        
        resetCurrentVariables();
      }
    } finally {
      this.processLock.unlock();
    }
  }
  
  
}
