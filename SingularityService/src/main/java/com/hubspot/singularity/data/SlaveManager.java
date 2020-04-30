package com.hubspot.singularity.data;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.data.usage.UsageManager;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;
import java.util.List;
import java.util.Optional;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SlaveManager extends AbstractMachineManager<SingularitySlave> {
  private static final Logger LOG = LoggerFactory.getLogger(SlaveManager.class);

  private static final String SLAVE_ROOT = "/slaves";
  private final SingularityLeaderCache leaderCache;
  private final UsageManager usageManager;

  @Inject
  public SlaveManager(
    CuratorFramework curator,
    SingularityConfiguration configuration,
    MetricRegistry metricRegistry,
    Transcoder<SingularitySlave> slaveTranscoder,
    Transcoder<SingularityMachineStateHistoryUpdate> stateHistoryTranscoder,
    Transcoder<SingularityExpiringMachineState> expiringMachineStateTranscoder,
    SingularityLeaderCache leaderCache,
    UsageManager usageManager
  ) {
    super(
      curator,
      configuration,
      metricRegistry,
      slaveTranscoder,
      stateHistoryTranscoder,
      expiringMachineStateTranscoder
    );
    this.leaderCache = leaderCache;
    this.usageManager = usageManager;
  }

  @Override
  protected String getRoot() {
    return SLAVE_ROOT;
  }

  public void activateLeaderCache() {
    leaderCache.cacheSlaves(getObjectsNoCache(getRoot()));
  }

  @Override
  public Optional<SingularitySlave> getObjectFromLeaderCache(String slaveId) {
    if (leaderCache.active()) {
      return leaderCache.getSlave(slaveId);
    }

    return Optional.empty(); // fallback to zk
  }

  @Override
  public List<SingularitySlave> getObjectsFromLeaderCache() {
    if (leaderCache.active()) {
      return leaderCache.getSlaves();
    }
    return null; // fallback to zk
  }

  @Override
  public void saveObjectToLeaderCache(SingularitySlave singularitySlave) {
    if (leaderCache.active()) {
      leaderCache.putSlave(singularitySlave);
    } else {
      LOG.info("Asked to save slaves to leader cache when not active");
    }
  }

  @Override
  public void deleteFromLeaderCache(String slaveId) {
    if (leaderCache.active()) {
      leaderCache.removeSlave(slaveId);
    } else {
      LOG.info("Asked to remove slave from leader cache when not active");
    }
  }

  @Override
  public StateChangeResult changeState(
    SingularitySlave singularitySlave,
    MachineState newState,
    Optional<String> message,
    Optional<String> user
  ) {
    if (newState == MachineState.DEAD) {
      usageManager.deleteSlaveUsage(singularitySlave.getId());
    }
    return super.changeState(singularitySlave, newState, message, user);
  }
}
