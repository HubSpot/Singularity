package com.hubspot.singularity.logwatcher.config.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.runner.base.config.TailMetadata;

public class LogLogForwarder implements LogForwarder {

  private final static Logger LOG = LoggerFactory.getLogger(LogLogForwarder.class);

  @Override
  public void forwardMessage(TailMetadata tailMetadata, String line) {
    LOG.info("--> {}, line: {}", tailMetadata, line);
  }

}
