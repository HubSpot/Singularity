package com.hubspot.singularity.logwatcher.config;

import java.util.Properties;

import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseConfigurationLoader;

public class SingularityLogWatcherConfigurationLoader extends SingularityRunnerBaseConfigurationLoader {

  public static final String BYTE_BUFFER_CAPACITY = "logwatcher.bytebuffer.capacity";
  public static final String POLL_MILLIS = "logwatcher.poll.millis";
  public static final String FLUENTD_HOSTS = "logwatcher.fluentd.comma.separated.hosts.and.ports";

  public static final String STORE_DIRECTORY = "logwatcher.store.directory";
  public static final String STORE_SUFFIX = "logwatcher.store.suffix";
  
  @Override
  protected void bindDefaults(Properties properties) {
    super.bindDefaults(properties);

    properties.put(BYTE_BUFFER_CAPACITY, "8192");
    properties.put(POLL_MILLIS, "1000");
    properties.put(FLUENTD_HOSTS, "localhost:24224");

    properties.put(STORE_SUFFIX, ".store");
  }
  
}
