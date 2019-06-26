package com.hubspot.singularity.data;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;
import com.hubspot.singularity.cache.SingularityCache;

@Singleton
public class RackManager extends AbstractMachineManager<SingularityRack> {
  private static final Logger LOG = LoggerFactory.getLogger(RackManager.class);

  private static final String RACK_ROOT = "/racks";
  private final SingularityCache leaderCache;

  @Inject
  public RackManager(CuratorFramework curator,
                     SingularityConfiguration configuration,
                     MetricRegistry metricRegistry,
                     Transcoder<SingularityRack> rackTranscoder,
                     Transcoder<SingularityMachineStateHistoryUpdate> stateHistoryTranscoder,
                     Transcoder<SingularityExpiringMachineState> expiringMachineStateTranscoder,
                     SingularityCache leaderCache) {
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

  @Override
  public Optional<SingularityRack> getObject(String rackId) {
    return leaderCache.getRack(rackId);
  }

  @Override
  public List<SingularityRack> getObjects() {
    return leaderCache.getRacks();
  }

  @Override
  public void saveObjectToCache(SingularityRack rackId) {
    leaderCache.putRack(rackId);
  }

  @Override
  public void deleteFromCache(String rackId) {
    leaderCache.removeRack(rackId);
  }
}
