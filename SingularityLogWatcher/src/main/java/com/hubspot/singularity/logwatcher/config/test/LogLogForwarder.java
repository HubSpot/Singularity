package com.hubspot.singularity.logwatcher.config.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.singularity.logwatcher.LogForwarder;

public class LogLogForwarder implements LogForwarder {

  private final static Logger LOG = LoggerFactory.getLogger(LogLogForwarder.class);

  @Override
  public void forwardMessage(String tag, String line) {
    LOG.info("--> {}, line: {}", tag, line);
  }

}
