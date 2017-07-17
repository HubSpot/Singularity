package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.MachineState;
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
    leaderCache.cacheRacks(getObjects());
  }

  public Optional<SingularityRack> getRack(String rackName) {
    if (leaderCache.active()) {
      return leaderCache.getRack(rackName);
    }

    return getObject(rackName);
  }

  @Override
  public int getNumActive() {
    if (leaderCache.active()) {
      return Math.toIntExact(leaderCache.getRacks().stream().filter(x -> x.getCurrentState().getState().equals(MachineState.ACTIVE)).count());
    }

    return super.getNumActive();
  }

  @Override
  public void saveObject(SingularityRack rack) {
    if (leaderCache.active()) {
      leaderCache.putRack(rack);
    }

    super.saveObject(rack);
  }

}
