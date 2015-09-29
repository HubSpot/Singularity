package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class SlaveManager extends AbstractMachineManager<SingularitySlave> {

  private static final String SLAVE_ROOT = "/slaves";

  @Inject
  public SlaveManager(CuratorFramework curator, SingularityConfiguration configuration, Transcoder<SingularitySlave> slaveTranscoder, Transcoder<SingularityMachineStateHistoryUpdate> stateHistoryTranscoder) {
    super(curator, configuration, slaveTranscoder, stateHistoryTranscoder);
  }

  @Override
  protected String getRoot() {
    return SLAVE_ROOT;
  }

}
