package com.hubspot.singularity.logwatcher;

public interface LogForwarder {

  public void forwardMessage(TailMetadata tailMetadata, String line);
  
}
