package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;
import com.hubspot.singularity.data.DisasterManager;

@Singleton
public class SingularityDeployPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDeployPoller.class);

  private final SingularityDeployChecker deployChecker;
  private final DisasterManager disasterManager;

  @Inject
  SingularityDeployPoller(SingularityDeployChecker deployChecker, SingularityConfiguration configuration, SingularitySchedulerLock lock, DisasterManager disasterManager) {
    super(configuration.getCheckDeploysEverySeconds(), TimeUnit.SECONDS, lock);

    this.deployChecker = deployChecker;
    this.disasterManager = disasterManager;
  }

  @Override
  public void runActionOnPoll() {
    if (!disasterManager.isDisabled(SingularityAction.RUN_DEPLOY_POLLER)) {
      final long start = System.currentTimeMillis();

      final int numDeploys = deployChecker.checkDeploys();

      if (numDeploys == 0) {
        LOG.trace("No pending deploys");
      } else {
        LOG.info("Checked {} deploys in {}", numDeploys, JavaUtils.duration(start));
      }
    } else {
      LOG.warn("Deploy poller is currently disabled");
    }
  }
}
