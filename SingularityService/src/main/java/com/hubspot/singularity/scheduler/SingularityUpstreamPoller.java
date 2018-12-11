package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityUpstreamPoller extends SingularityLeaderOnlyPoller {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityUpstreamPoller.class);

  private final SingularityUpstreamChecker upstreamChecker;

  @Inject
  SingularityUpstreamPoller(SingularityConfiguration configuration, SingularityUpstreamChecker upstreamChecker) {
    super(configuration.getCheckUpstreamsEverySeconds(), TimeUnit.SECONDS, true);

    this.upstreamChecker = upstreamChecker;

  }

  @Override
  public void runActionOnPoll() {
    LOG.info("Checking upstreams");
    upstreamChecker.syncUpstreams();
  }
}
