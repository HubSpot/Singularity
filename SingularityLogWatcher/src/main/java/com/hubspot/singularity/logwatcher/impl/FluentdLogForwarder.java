package com.hubspot.singularity.logwatcher.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.fluentd.logger.FluentLogger;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherConfiguration;
import com.hubspot.singularity.runner.base.shared.TailMetadata;

public class FluentdLogForwarder implements LogForwarder {

  private final List<FluentLogger> fluentLoggers;
  private final SingularityLogWatcherConfiguration configuration;

  @Inject
  public FluentdLogForwarder(List<FluentLogger> fluentLoggers, SingularityLogWatcherConfiguration configuration) {
    this.fluentLoggers = fluentLoggers;
    this.configuration = configuration;
  }

  @Override
  public void forwardMessage(TailMetadata tailMetadata, String line) {
    Collections.shuffle(fluentLoggers);

    for (FluentLogger logger : fluentLoggers) {
      if (logger.log(getTag(tailMetadata.getTag()), getData(tailMetadata, line), getTimestamp(tailMetadata, line))) {
        return;
      }
    }

    throw new LogForwarderException();
  }

  // TODO
  private long getTimestamp(TailMetadata tailMetadata, String line) {
    return System.currentTimeMillis();
  }

  private String getTag(String tailTag) {
    return String.format("%s.%s", configuration.getFluentdTagPrefix(), tailTag);
  }

  private Map<String, Object> getData(TailMetadata tailMetadata, String line) {
    return
        ImmutableMap.<String, Object> builder()
        .putAll(tailMetadata.getExtraFields())
        .put("message", line)
        .build();
  }


}
