package com.hubspot.singularity;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;

import com.google.common.io.Closeables;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SingularityAbort {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityAbort.class);

  private final CuratorFramework curator;
  private final LeaderLatch leaderLatch;
  private final SingularityDriverManager driverManager;
  private final SingularityCloser closer;
  
  @Inject
  public SingularityAbort(@Named(SingularityModule.UNDERLYING_CURATOR) CuratorFramework curator, LeaderLatch leaderLatch, SingularityDriverManager driverManager, SingularityCloser closer) {
    this.curator = curator;
    this.leaderLatch = leaderLatch;
    this.driverManager = driverManager;
    this.closer = closer;
  }

  public void abort() {
    stop();
    
    flushLogs();
    
    exit();
  }
  
  public void exit() {
    System.exit(1);
  }

  public void stop() {
    closeCloseables();
    
    closeDriver();
    
    closeLeader();
  
    closeCurator();
  }
  
  private void closeCloseables() {
    closer.closeAllCloseables();
  }
  
  private void closeDriver() {
    try {
      driverManager.stop();
    } catch (Throwable t) {
      LOG.warn("While stopping driver", t);
    }
  }
  
  public void closeCurator() {
    try {
      Closeables.close(curator, false);
    } catch (Throwable t) {
      LOG.warn("While closing curator", t);
    }
  }

  public void closeLeader() {
    try {
      Closeables.close(leaderLatch, false);
    } catch (Throwable t) {
      LOG.warn("While closing leader latch", t);
    }
  }
  
  private void flushLogs() {
    LOG.info("Attempting to flush logs and wait for 5 seconds...");

    ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
    if (loggerFactory instanceof LoggerContext) {
      LoggerContext context = (LoggerContext) loggerFactory;
      context.stop();
    }

    try {
      Thread.sleep(5000);
    } catch (Exception e) {

    }
  }

}
