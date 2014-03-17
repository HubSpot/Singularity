package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.SingularityRackTranscoder;

public class RackManager extends AbstractMachineManager<SingularityRack> {
  
  private static final String RACK_ROOT = "racks";

  @Inject
  public RackManager(CuratorFramework curator, ObjectMapper objectMapper, SingularityConfiguration configuration, SingularityRackTranscoder rackTranscoder) {
    super(curator, configuration.getZookeeperAsyncTimeout(), objectMapper, rackTranscoder);
  }
  
  @Override
  public String getRoot() {
    return RACK_ROOT;
  }  
  
}
