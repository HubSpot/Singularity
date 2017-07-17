package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;

@Singleton
public class SlaveManager extends AbstractMachineManager<SingularitySlave> {

  private static final String SLAVE_ROOT = "/slaves";
  private final SingularityLeaderCache leaderCache;

  @Inject
  public SlaveManager(CuratorFramework curator,
                      SingularityConfiguration configuration,
                      MetricRegistry metricRegistry,
                      Transcoder<SingularitySlave> slaveTranscoder,
                      Transcoder<SingularityMachineStateHistoryUpdate> stateHistoryTranscoder,
                      Transcoder<SingularityExpiringMachineState> expiringMachineStateTranscoder,
                      SingularityLeaderCache leaderCache) {
    super(curator, configuration, metricRegistry, slaveTranscoder, stateHistoryTranscoder, expiringMachineStateTranscoder);
    this.leaderCache = leaderCache;
  }

  @Override
  protected String getRoot() {
    return SLAVE_ROOT;
  }

  public void activateLeaderCache() {
    leaderCache.cacheSlaves(getObjects());
  }

  public Optional<SingularitySlave> getSlave(String slaveId) {
    if (leaderCache.active()) {
      return leaderCache.getSlave(slaveId);
    }

    return getObject(slaveId);
  }

  @Override
  public int getNumActive() {
    if (leaderCache.active()) {
      return Math.toIntExact(leaderCache.getSlaves().stream().filter(x -> x.getCurrentState().getState().equals(MachineState.ACTIVE)).count());
    }

    return super.getNumActive();
  }

  @Override
  public void saveObject(SingularitySlave slave) {
    if (leaderCache.active()) {
      leaderCache.putSlave(slave);
    }

    super.saveObject(slave);
  }



}
