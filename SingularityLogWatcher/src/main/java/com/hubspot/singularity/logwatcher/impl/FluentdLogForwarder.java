package com.hubspot.singularity.logwatcher.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.fluentd.logger.FluentLogger;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.runner.base.config.TailMetadata;

public class FluentdLogForwarder implements LogForwarder {

  private final List<FluentLogger> fluentLoggers;

  @Inject
  public FluentdLogForwarder(List<FluentLogger> fluentLoggers) {
    this.fluentLoggers = fluentLoggers;
  }

  @Override
  public void forwardMessage(TailMetadata tailMetadata, String line) {
    Collections.shuffle(fluentLoggers);
    
    for (FluentLogger logger : fluentLoggers) {
      if (logger.log(tailMetadata.getTag(), getData(tailMetadata, line), getTimestamp(tailMetadata, line))) {
        return;
      }
    }
    
    throw new LogForwarderException();
  }
  
  // TODO
  private long getTimestamp(TailMetadata tailMetadata, String line) {
    return System.currentTimeMillis();
  }
  
  private Map<String, Object> getData(TailMetadata tailMetadata, String line) {
    return 
        ImmutableMap.<String, Object> builder()
        .putAll(tailMetadata.getExtraFields())
        .put("message", line)
        .build();  
  }
  

}
