package com.hubspot.singularity.logwatcher.config;

import java.util.Properties;

import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseConfigurationLoader;

public class SingularityLogWatcherConfigurationLoader extends SingularityRunnerBaseConfigurationLoader {

  public static final String BYTE_BUFFER_CAPACITY = "logwatcher.bytebuffer.capacity";
  public static final String POLL_MILLIS = "logwatcher.poll.millis";
  public static final String FLUENTD_HOSTS = "logwatcher.fluentd.comma.separated.hosts.and.ports";

  public static final String STORE_DIRECTORY = "logwatcher.store.directory";
  public static final String STORE_SUFFIX = "logwatcher.store.suffix";
  
  public static final String RETRY_DELAY_SECONDS = "logwatcher.retry.delay.seconds";
  
  public static final String FLUENTD_TAG_PREFIX = "logwatcher.fluentd.tag.prefix";
  
  public static final String LOGROTATE_AFTER_BYTES = "logwatcher.logrotate.after.bytes";
  public static final String LOGROTATE_DIRECTORY = "logwatcher.logrotate.to.directory";
  public static final String LOGROTATE_MAXAGE_DAYS = "logwatcher.logrotate.maxage.days";
  public static final String LOGROTATE_COUNT = "logwatcher.logrotate.count";
  public static final String LOGROTATE_DATEFORMAT = "logwatcher.logrotate.dateformat";
  
  @Override
  protected void bindDefaults(Properties properties) {
    super.bindDefaults(properties);

    properties.put(LOGROTATE_AFTER_BYTES, "104857600"); // 100MB
    properties.put(LOGROTATE_DIRECTORY, "logs");
    properties.put(LOGROTATE_MAXAGE_DAYS, "7");
    properties.put(LOGROTATE_COUNT, "20");
    properties.put(LOGROTATE_DATEFORMAT, "-%s");
    
    properties.put(BYTE_BUFFER_CAPACITY, "8192");
    properties.put(POLL_MILLIS, "1000");
    properties.put(FLUENTD_HOSTS, "localhost:24224");

    properties.put(RETRY_DELAY_SECONDS, "60");
    
    properties.put(STORE_SUFFIX, ".store");
    properties.put(FLUENTD_TAG_PREFIX, "forward");
  }
  
}
