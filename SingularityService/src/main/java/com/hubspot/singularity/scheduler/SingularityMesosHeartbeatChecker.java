package com.hubspot.singularity.scheduler;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingularityMesosHeartbeatChecker extends SingularityLeaderOnlyPoller {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityMesosHeartbeatChecker.class
  );

  private final SingularityConfiguration configuration;
  private final SingularityMesosScheduler mesosScheduler;
  private final AtomicLong lastHeartbeatTime;

  @Inject
  public SingularityMesosHeartbeatChecker(
    SingularityConfiguration configuration,
    SingularityMesosScheduler mesosScheduler,
    @Named(
      SingularityMainModule.LAST_MESOS_MASTER_HEARTBEAT_TIME
    ) AtomicLong lastHeartbeatTime
  ) {
    super(configuration.getCheckMesosMasterHeartbeatEverySeconds(), TimeUnit.SECONDS);
    this.configuration = configuration;
    this.mesosScheduler = mesosScheduler;
    this.lastHeartbeatTime = lastHeartbeatTime;
  }

  @Override
  public void runActionOnPoll() {
    if (!mesosScheduler.isRunning()) {
      LOG.debug(
        "Not checking for a Mesos heartbeat because we haven't subscribed with the Mesos Master yet."
      );
      return;
    }
    if (!mesosScheduler.getHeartbeatIntervalSeconds().isPresent()) {
      LOG.debug(
        "Not checking for a Mesos heartbeat because the Mesos Master didn't advertise a heartbeat interval."
      );
      return;
    }

    long millisSinceLastHeartbeat = System.currentTimeMillis() - lastHeartbeatTime.get();
    double missedHeartbeats =
      millisSinceLastHeartbeat /
      (mesosScheduler.getHeartbeatIntervalSeconds().get() * 1000);

    if (missedHeartbeats > configuration.getMaxMissedMesosMasterHeartbeats()) {
      LOG.error(
        "I haven't received a Mesos heartbeat in {}ms! Restarting mesos connection",
        millisSinceLastHeartbeat
      );
      mesosScheduler.reconnectMesos();
    }
  }
}
