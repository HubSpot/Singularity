package com.hubspot.singularity.runner.base.shared;

public interface SingularityDriver {
  
  public abstract void startAndWait();
  
  public abstract void shutdown();
 
}
