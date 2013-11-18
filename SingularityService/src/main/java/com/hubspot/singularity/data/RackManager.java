package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;

import com.google.inject.Inject;

public class RackManager extends AbstractMachineManager {
  
  private static final String RACK_ROOT = "racks";
  
  @Inject
  public RackManager(CuratorFramework curator) {
    super(curator);
  }

  @Override
  public String getRoot() {
    return RACK_ROOT;
  }  
  
}
