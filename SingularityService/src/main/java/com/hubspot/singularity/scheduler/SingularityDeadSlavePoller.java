package com.hubspot.singularity.scheduler;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.SlaveManager;

@Singleton
public class SingularityDeadSlavePoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDeadSlavePoller.class);

  private final SlaveManager slaveManager;
  private final SingularityConfiguration configuration;

  @Inject
  SingularityDeadSlavePoller(SingularityConfiguration configuration, SlaveManager slaveManager) {
    super(1, TimeUnit.HOURS);

    this.slaveManager = slaveManager;
    this.configuration = configuration;
  }

  @Override
  public void runActionOnPoll() {
    final long start = System.currentTimeMillis();

    final List<SingularitySlave> deadSlaves = slaveManager.getObjectsFiltered(MachineState.DEAD);

    if (deadSlaves.isEmpty()) {
      LOG.trace("No dead slaves");
      return;
    }

    int deleted = 0;
    final long maxDuration = TimeUnit.HOURS.toMillis(configuration.getDeleteDeadSlavesAfterHours());

    for (SingularitySlave deadSlave : slaveManager.getObjectsFiltered(MachineState.DEAD)) {
      final long duration = System.currentTimeMillis() - deadSlave.getCurrentState().getTimestamp();

      if (duration > maxDuration) {
        SingularityDeleteResult result = slaveManager.deleteObject(deadSlave.getId());

        deleted++;

        LOG.info("Removing dead slave {} ({}) after {} (max {})", deadSlave.getId(), result, JavaUtils.durationFromMillis(duration), JavaUtils.durationFromMillis(maxDuration));
      }
    }

    LOG.debug("Checked {} dead slaves, deleted {} in {}", deadSlaves.size(), deleted, JavaUtils.duration(start));
  }

}
