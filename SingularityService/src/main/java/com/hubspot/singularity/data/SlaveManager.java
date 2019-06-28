package com.hubspot.singularity.data;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.data.usage.UsageManager;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;
import com.hubspot.singularity.cache.SingularityCache;

@Singleton
public class SlaveManager extends AbstractMachineManager<SingularitySlave> {
  private static final Logger LOG = LoggerFactory.getLogger(SlaveManager.class);

  private static final String SLAVE_ROOT = "/slaves";
  private final SingularityCache cache;
  private final UsageManager usageManager;

  @Inject
  public SlaveManager(CuratorFramework curator,
                      SingularityConfiguration configuration,
                      MetricRegistry metricRegistry,
                      Transcoder<SingularitySlave> slaveTranscoder,
                      Transcoder<SingularityMachineStateHistoryUpdate> stateHistoryTranscoder,
                      Transcoder<SingularityExpiringMachineState> expiringMachineStateTranscoder,
                      SingularityCache cache,
                      UsageManager usageManager) {
    super(curator, configuration, metricRegistry, slaveTranscoder, stateHistoryTranscoder, expiringMachineStateTranscoder);
    this.cache = cache;
    this.usageManager = usageManager;
  }

  @Override
  protected String getRoot() {
    return SLAVE_ROOT;
  }

  public void activateLeaderCache() {
    cache.cacheSlaves(getObjectsNoCache(getRoot()));
  }

  @Override
  public Optional<SingularitySlave> getObject(String slaveId) {
    return cache.getSlave(slaveId);
  }

  @Override
  public List<SingularitySlave> getObjects() {
    return cache.getSlaves();
  }

  @Override
  public void saveObjectToCache(SingularitySlave singularitySlave) {
    cache.putSlave(singularitySlave);
  }

  @Override
  public void deleteFromCache(String slaveId) {
    cache.removeSlave(slaveId);
  }

  @Override
  public StateChangeResult changeState(SingularitySlave singularitySlave, MachineState newState, Optional<String> message, Optional<String> user) {
    if (newState == MachineState.DEAD) {
      usageManager.deleteSlaveUsage(singularitySlave.getId());
    }
    return super.changeState(singularitySlave, newState, message, user);
  }
}
