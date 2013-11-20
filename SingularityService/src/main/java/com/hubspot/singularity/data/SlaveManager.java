package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularitySlave;

public class SlaveManager extends AbstractMachineManager<SingularitySlave> {

  private static final String SLAVE_ROOT = "slaves";
  
  private final ObjectMapper objectMapper;
  
  @Inject
  public SlaveManager(CuratorFramework curator, ObjectMapper objectMapper) {
    super(curator, objectMapper);
  
    this.objectMapper = objectMapper;
  }
  
  @Override
  public String getRoot() {
    return SLAVE_ROOT;
  }
  
  @Override
  public SingularitySlave fromBytes(byte[] bytes) {
    try {
      return SingularitySlave.fromBytes(bytes, objectMapper);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
 
}
