package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRack;

public class RackManager extends AbstractMachineManager<SingularityRack> {
  
  private static final String RACK_ROOT = "racks";

  private final ObjectMapper objectMapper;
  
  @Inject
  public RackManager(CuratorFramework curator, ObjectMapper objectMapper) {
    super(curator, objectMapper);
    this.objectMapper = objectMapper;
  }
  
  @Override
  public SingularityRack fromBytes(byte[] bytes) {
    try {
      return SingularityRack.fromBytes(bytes, objectMapper);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public String getRoot() {
    return RACK_ROOT;
  }  
  
}
