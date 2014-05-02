package com.hubspot.singularity.logwatcher.config.test;

import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.runner.base.config.TailMetadata;

public class NoopLogForwarder implements LogForwarder {

  @Override
  public void forwardMessage(TailMetadata tailMetadata, String line) {}

}
