package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityDeployPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDeployPoller.class);

  private final SingularityDeployChecker deployChecker;

  @Inject
  public SingularityDeployPoller(SingularityExceptionNotifier exceptionNotifier, SingularityDeployChecker deployChecker, SingularityConfiguration configuration, SingularityAbort abort, SingularityCloser closer) {
    super(exceptionNotifier, abort, closer, configuration.getCheckDeploysEverySeconds(), TimeUnit.SECONDS, SchedulerLockType.LOCK);

    this.deployChecker = deployChecker;
  }

  @Override
  public void runActionOnPoll(SingularityMesosSchedulerDelegator mesosScheduler, SchedulerDriver driver) {
    final long start = System.currentTimeMillis();

    final int numDeploys = deployChecker.checkDeploys();

    if (numDeploys == 0) {
      LOG.trace("No pending deploys");
    } else {
      LOG.info("Checked {} deploys in {}", numDeploys, JavaUtils.duration(start));
    }
  }

}
