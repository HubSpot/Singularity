package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosModule;

@Singleton
public class SingularityDeployPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDeployPoller.class);

  private final SingularityDeployChecker deployChecker;

  @Inject
  SingularityDeployPoller(SingularityDeployChecker deployChecker, SingularityConfiguration configuration, @Named(SingularityMesosModule.SCHEDULER_LOCK_NAME) final Lock lock) {
    super(configuration.getCheckDeploysEverySeconds(), TimeUnit.SECONDS, lock);

    this.deployChecker = deployChecker;
  }

  @Override
  public void runActionOnPoll() {
    final long start = System.currentTimeMillis();

    final int numDeploys = deployChecker.checkDeploys();

    if (numDeploys == 0) {
      LOG.trace("No pending deploys");
    } else {
      LOG.info("Checked {} deploys in {}", numDeploys, JavaUtils.duration(start));
    }
  }
}
