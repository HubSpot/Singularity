package com.hubspot.singularity.logwatcher;

import com.hubspot.singularity.runner.base.config.TailMetadata;

public interface LogForwarder {

  @SuppressWarnings("serial")
  public static class LogForwarderException extends RuntimeException {
    
  }
  
  public void forwardMessage(TailMetadata tailMetadata, String line);
  
}
