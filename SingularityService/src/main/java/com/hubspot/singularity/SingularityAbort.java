package com.hubspot.singularity;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;

import com.google.common.io.Closeables;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.smtp.SingularityMailer;

public class SingularityAbort {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityAbort.class);

  private final CuratorFramework curator;
  private final LeaderLatch leaderLatch;
  private final SingularityDriverManager driverManager;
  private final SingularityCloser closer;
  private final SingularityMailer mailer;
  
  private volatile boolean aborting;
  private volatile boolean stopping;
  
  @Inject
  public SingularityAbort(@Named(SingularityServiceModule.UNDERLYING_CURATOR) CuratorFramework curator, LeaderLatch leaderLatch, SingularityDriverManager driverManager, SingularityCloser closer, SingularityMailer mailer) {
    this.curator = curator;
    this.leaderLatch = leaderLatch;
    this.driverManager = driverManager;
    this.closer = closer;
    this.mailer = mailer;
  
    this.aborting = false;
    this.stopping = false;
    
    this.curator.getConnectionStateListenable().addListener(new ConnectionStateListener() {
      
      @Override
      public void stateChanged(CuratorFramework client, ConnectionState newState) {
        if (newState == ConnectionState.LOST) {
          LOG.error("Aborting due to new connection state recieved from ZooKeeper: {}", newState);
          abort();
        }
      }
    });
  }

  private synchronized boolean checkAlreadyAborting() {
    if (!aborting) {
      aborting = true;
      return false;
    } else {
      LOG.warn("Abort asked to abort, but already aborting");
      return true;
    }
  }
  
  private synchronized boolean checkAlreadyStopping() {
    if (!stopping) {
      stopping = true;
      return false;
    } else {
      LOG.warn("Abort asked to stop, but already stopping");
      return true;
    }
  }
  
  public void abort() {
    if (checkAlreadyAborting()) {
      return;
    }
    
    mailer.sendAbortMail();
    
    stop();
    
    flushLogs();
    
    exit();
  }
  
  private void exit() {
    System.exit(1);
  }

  public void stop() {
    if (checkAlreadyStopping()) {
      return;
    }
    
    closeDriver();
    
    closeCloseables();
    
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
  
  private void closeCurator() {
    try {
      Closeables.close(curator, false);
    } catch (Throwable t) {
      LOG.warn("While closing curator", t);
    }
  }

  private void closeLeader() {
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
      LOG.info("While sleeping for log flush", e);
    }
  }

}
