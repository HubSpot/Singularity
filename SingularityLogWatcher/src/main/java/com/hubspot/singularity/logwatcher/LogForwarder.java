package com.hubspot.singularity.logwatcher;

import com.hubspot.singularity.runner.base.shared.TailMetadata;

public interface LogForwarder {

  @SuppressWarnings("serial")
  class LogForwarderException extends RuntimeException {

  }

  void forwardMessage(TailMetadata tailMetadata, String line);

}
