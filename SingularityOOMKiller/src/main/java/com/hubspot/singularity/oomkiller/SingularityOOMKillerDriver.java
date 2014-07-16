package com.hubspot.singularity.oomkiller;

import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.singularity.oomkiller.config.SingularityOOMKillerConfiguration;
import com.hubspot.singularity.runner.base.shared.SingularityDriver;

public class SingularityOOMKillerDriver implements SingularityDriver {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityOOMKillerDriver.class);

  private final SingularityOOMKillerConfiguration configuration;
  private final ScheduledExecutorService scheduler;
  private final SingularityOOMKiller oomKiller;
  
  @Inject
  public SingularityOOMKillerDriver(SingularityOOMKillerConfiguration configuration, SingularityOOMKiller oomKiller) {
    this.configuration = configuration;
    this.oomKiller = oomKiller;
  
    this.scheduler = null;
  }

  @Override
  public void startAndWait() {
    
  }
  
  @Override
  public void shutdown() {
    
  }
  
}
