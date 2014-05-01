package com.hubspot.singularity.logwatcher.config;

import java.util.Properties;

import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseConfigurationLoader;

public class SingularityLogWatcherConfigurationLoader extends SingularityRunnerBaseConfigurationLoader {

  public static final String BYTE_BUFFER_CAPACITY = "logwatcher.bytebuffer.capacity";
  public static final String POLL_MILLIS = "logwatcher.poll.millis";
  public static final String FLUENTD_HOSTS = "logwatcher.fluentd.comma.separated.hosts.and.ports";

  public static final String STORE_DIRECTORY = "logwatcher.store.directory";
  public static final String STORE_SUFFIX = "logwatcher.store.suffix";
  public static final String S3_QUEUE_DIRECTORY = "logwatcher.s3.queue.directory";
  
  public static final String FLUENTD_TAG_PREFIX = "logwatcher.fluentd.tag.prefix";
  
  public static final String LOGROTATE_AFTER_BYTES = "logwatcher.logrotate.after.bytes";
  
  @Override
  protected void bindDefaults(Properties properties) {
    super.bindDefaults(properties);

    properties.put(LOGROTATE_AFTER_BYTES, 104857600L); // 100MB
    
    properties.put(BYTE_BUFFER_CAPACITY, "8192");
    properties.put(POLL_MILLIS, "1000");
    properties.put(FLUENTD_HOSTS, "localhost:24224");

    properties.put(STORE_SUFFIX, ".store");
    properties.put(FLUENTD_TAG_PREFIX, "forward");
  }
  
}
