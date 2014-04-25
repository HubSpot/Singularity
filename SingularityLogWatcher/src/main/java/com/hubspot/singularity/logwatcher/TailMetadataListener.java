package com.hubspot.singularity.logwatcher;

public interface TailMetadataListener {

  public void tailChanged(TailMetadata tailMetadata);
  
}
