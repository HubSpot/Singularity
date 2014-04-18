package com.hubspot.singularity.logwatcher;

public interface LogForwarder {

  public void forwardMessage(String tag, String line);
  
}
