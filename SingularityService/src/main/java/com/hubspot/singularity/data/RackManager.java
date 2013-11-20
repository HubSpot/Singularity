package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityMachineAbstraction;

public class RackManager extends AbstractMachineManager {
  
  private static final String RACK_ROOT = "racks";
  
  @Override
  public SingularityMachineAbstraction fromBytes(byte[] bytes) {
    return null;
  }

  @Inject
  public RackManager(CuratorFramework curator) {
    super(curator, null);
  }

  @Override
  public String getRoot() {
    return RACK_ROOT;
  }  
  
}
