package com.hubspot.singularity.logwatcher.config.test;

import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.runner.base.shared.TailMetadata;

public class NoopLogForwarder implements LogForwarder {

  @Override
  public void forwardMessage(TailMetadata tailMetadata, String line) {}

}
