package com.hubspot.singularity.logwatcher;

public interface TailMetadataListener {

  public void addedTailMetadata(TailMetadata newTailMetadata);
  
  public void stopTail(TailMetadata tailMetadataToStop);
  
}
