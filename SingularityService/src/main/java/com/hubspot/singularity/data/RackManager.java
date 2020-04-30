package com.hubspot.singularity.data;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;
import java.util.List;
import java.util.Optional;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class RackManager extends AbstractMachineManager<SingularityRack> {
  private static final Logger LOG = LoggerFactory.getLogger(RackManager.class);

  private static final String RACK_ROOT = "/racks";
  private final SingularityLeaderCache leaderCache;

  @Inject
  public RackManager(
    CuratorFramework curator,
    SingularityConfiguration configuration,
    MetricRegistry metricRegistry,
    Transcoder<SingularityRack> rackTranscoder,
    Transcoder<SingularityMachineStateHistoryUpdate> stateHistoryTranscoder,
    Transcoder<SingularityExpiringMachineState> expiringMachineStateTranscoder,
    SingularityLeaderCache leaderCache
  ) {
    super(
      curator,
      configuration,
      metricRegistry,
      rackTranscoder,
      stateHistoryTranscoder,
      expiringMachineStateTranscoder
    );
    this.leaderCache = leaderCache;
  }

  @Override
  protected String getRoot() {
    return RACK_ROOT;
  }

  public void activateLeaderCache() {
    leaderCache.cacheRacks(getObjectsNoCache(getRoot()));
  }

  @Override
  public Optional<SingularityRack> getObjectFromLeaderCache(String rackId) {
    if (leaderCache.active()) {
      return leaderCache.getRack(rackId);
    }

    return Optional.empty(); // fallback to zk
  }

  @Override
  public List<SingularityRack> getObjectsFromLeaderCache() {
    if (leaderCache.active()) {
      return leaderCache.getRacks();
    }
    return null; // fallback to zk
  }

  @Override
  public void saveObjectToLeaderCache(SingularityRack rackId) {
    if (leaderCache.active()) {
      leaderCache.putRack(rackId);
    } else {
      LOG.info("Asked to save slaves to leader cache when not active");
    }
  }

  @Override
  public void deleteFromLeaderCache(String rackId) {
    if (leaderCache.active()) {
      leaderCache.removeRack(rackId);
    } else {
      LOG.info("Asked to remove slave from leader cache when not active");
    }
  }
}
