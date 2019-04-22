package com.hubspot.singularity.data;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;

@Singleton
public class RackManager extends AbstractMachineManager<SingularityRack> {

  private static final String RACK_ROOT = "/racks";
  private final SingularityLeaderCache leaderCache;

  @Inject
  public RackManager(CuratorFramework curator,
                     SingularityConfiguration configuration,
                     MetricRegistry metricRegistry,
                     Transcoder<SingularityRack> rackTranscoder,
                     Transcoder<SingularityMachineStateHistoryUpdate> stateHistoryTranscoder,
                     Transcoder<SingularityExpiringMachineState> expiringMachineStateTranscoder,
                     SingularityLeaderCache leaderCache) {
    super(curator, configuration, metricRegistry, rackTranscoder, stateHistoryTranscoder, expiringMachineStateTranscoder);

    this.leaderCache = leaderCache;
  }

  @Override
  protected String getRoot() {
    return RACK_ROOT;
  }

  public void activateLeaderCache() {
    leaderCache.cacheRacks(getObjectsNoCache(getRoot()));
  }

  public Optional<SingularityRack> getRack(String rackName) {
    if (leaderCache.active()) {
      return leaderCache.getRack(rackName);
    }

    return getObject(rackName);
  }

  @Override
  public List<SingularityRack> getObjectsFromLeaderCache() {
    return leaderCache.getRacks();
  }

  @Override
  public void saveObjectToLeaderCache(SingularityRack rack) {
    leaderCache.putRack(rack);
  }

  @Override
  public void deleteFromLeaderCache(String rackId) {
    leaderCache.removeRack(rackId);
  }

}
