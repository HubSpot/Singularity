package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;

@Singleton
public class SlaveManager extends AbstractMachineManager<SingularitySlave> {

  private static final String SLAVE_ROOT = "/slaves";

  @Inject
  public SlaveManager(CuratorFramework curator, SingularityConfiguration configuration,  MetricRegistry metricRegistry, Transcoder<SingularitySlave> slaveTranscoder,
      Transcoder<SingularityMachineStateHistoryUpdate> stateHistoryTranscoder, Transcoder<SingularityExpiringMachineState> expiringMachineStateTranscoder) {
    super(curator, configuration, metricRegistry, slaveTranscoder, stateHistoryTranscoder, expiringMachineStateTranscoder);
  }

  @Override
  protected String getRoot() {
    return SLAVE_ROOT;
  }

}
