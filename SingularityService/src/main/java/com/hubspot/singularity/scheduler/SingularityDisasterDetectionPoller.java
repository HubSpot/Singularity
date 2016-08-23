package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;

public class SingularityDisasterDetectionPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDisasterDetectionPoller.class);

  private final SingularityConfiguration configuration;
  private final TaskManager taskManager;
  private final SlaveManager slaveManager;

  @Inject
  public SingularityDisasterDetectionPoller(SingularityConfiguration configuration,  TaskManager taskManager, SlaveManager slaveManager) {
    super(configuration.getDisasterDetection().getRunEveryMillis(), TimeUnit.MILLISECONDS);
    this.configuration = configuration;
    this.taskManager = taskManager;
    this.slaveManager = slaveManager;
  }

  @Override
  protected boolean isEnabled() {
    return configuration.getDisasterDetection().isEnabled();
  }

  @Override
  protected boolean abortsOnError() {
    return false;
  }

  @Override
  public void runActionOnPoll() {
    
  }


}
