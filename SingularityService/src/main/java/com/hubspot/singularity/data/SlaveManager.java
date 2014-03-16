package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.SingularitySlaveTranscoder;

public class SlaveManager extends AbstractMachineManager<SingularitySlave> {

  private static final String SLAVE_ROOT = "slaves";
  
  @Inject
  public SlaveManager(CuratorFramework curator, ObjectMapper objectMapper, SingularityConfiguration configuration, SingularitySlaveTranscoder slaveTranscoder) {
    super(curator, configuration.getZookeeperAsyncTimeout(), objectMapper, slaveTranscoder);
  }
  
  @Override
  public String getRoot() {
    return SLAVE_ROOT;
  }
  
}
