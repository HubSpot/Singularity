package com.hubspot.singularity.logwatcher;

public interface LogForwarder {

  @SuppressWarnings("serial")
  public static class LogForwarderException extends RuntimeException {
    
  }
  
  public void forwardMessage(TailMetadata tailMetadata, String line);
  
}
