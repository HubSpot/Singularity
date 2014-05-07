package com.hubspot.singularity.logwatcher.config;

import java.nio.file.Path;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;

public class SingularityLogWatcherConfiguration {

  private final int byteBufferCapacity;
  private final long pollMillis;
  private final List<FluentdHost> fluentdHosts;
  private final Path storeDirectory;
  private final String storeSuffix;
  private final String fluentdTagPrefix;
  private final long retryDelaySeconds;
  
  private final Path logMetadataDirectory;
  private final String logMetadataSuffix;
  
  @Inject
  public SingularityLogWatcherConfiguration(
      @Named(SingularityLogWatcherConfigurationLoader.BYTE_BUFFER_CAPACITY) String byteBufferCapacity, 
      @Named(SingularityLogWatcherConfigurationLoader.FLUENTD_TAG_PREFIX) String fluentdTagPrefix,
      @Named(SingularityLogWatcherConfigurationLoader.POLL_MILLIS) String pollMillis, 
      @Named(SingularityLogWatcherConfigurationLoader.FLUENTD_HOSTS) String fluentdHosts, 
      @Named(SingularityLogWatcherConfigurationLoader.STORE_DIRECTORY) String storeDirectory, 
      @Named(SingularityLogWatcherConfigurationLoader.STORE_SUFFIX) String storeSuffix, 
      @Named(SingularityLogWatcherConfigurationLoader.RETRY_DELAY_SECONDS) String retryDelaySeconds,
      @Named(SingularityLogWatcherConfigurationLoader.LOG_METADATA_DIRECTORY) String logMetadataDirectory,
      @Named(SingularityLogWatcherConfigurationLoader.LOG_METADATA_SUFFIX) String logMetadataSuffix
      ) {
    this.byteBufferCapacity = Integer.parseInt(byteBufferCapacity);
    this.pollMillis = Long.parseLong(pollMillis);
    this.fluentdHosts = parseFluentdHosts(fluentdHosts);
    this.storeSuffix = storeSuffix;
    this.fluentdTagPrefix = fluentdTagPrefix;
    this.storeDirectory = JavaUtils.getValidDirectory(storeDirectory, SingularityLogWatcherConfigurationLoader.STORE_DIRECTORY);
    this.retryDelaySeconds = Long.parseLong(retryDelaySeconds);
    this.logMetadataSuffix = logMetadataSuffix;
    this.logMetadataDirectory = JavaUtils.getValidDirectory(logMetadataDirectory, SingularityLogWatcherConfigurationLoader.LOG_METADATA_DIRECTORY);
  }

  public Path getLogMetadataDirectory() {
    return logMetadataDirectory;
  }

  public String getLogMetadataSuffix() {
    return logMetadataSuffix;
  }
  
  public static class FluentdHost {
    
    private final String host;
    private final int port;
    
    public FluentdHost(String host, int port) {
      this.host = host;
      this.port = port;
    }
    
    public String getHost() {
      return host;
    }
    
    public int getPort() {
      return port;
    }

    @Override
    public String toString() {
      return "FluentdHost [host=" + host + ", port=" + port + "]";
    }
    
  }

  public Path getStoreDirectory() {
    return storeDirectory;
  }

  public String getStoreSuffix() {
    return storeSuffix;
  }
  
  public long getRetryDelaySeconds() {
    return retryDelaySeconds;
  }

  private List<FluentdHost> parseFluentdHosts(String fluentdHosts) {
    final String[] split = fluentdHosts.split(",");
    final List<FluentdHost> hosts = Lists.newArrayListWithCapacity(split.length);
    for (String subsplit : split) {
      final String[] hostAndPort = subsplit.split(":");
      hosts.add(new FluentdHost(hostAndPort[0], Integer.parseInt(hostAndPort[1])));
    }
    return hosts;
  }

  public long getPollMillis() {
    return pollMillis;
  }

  public int getByteBufferCapacity() {
    return byteBufferCapacity;
  }

  public List<FluentdHost> getFluentdHosts() {
    return fluentdHosts;
  }

  public String getFluentdTagPrefix() {
    return fluentdTagPrefix;
  }

  @Override
  public String toString() {
    return "SingularityLogWatcherConfiguration [byteBufferCapacity=" + byteBufferCapacity + ", pollMillis=" + pollMillis + ", fluentdHosts=" + fluentdHosts + ", storeDirectory=" + storeDirectory + ", storeSuffix=" + storeSuffix
        + ", fluentdTagPrefix=" + fluentdTagPrefix + ", retryDelaySeconds=" + retryDelaySeconds + ", logMetadataDirectory=" + logMetadataDirectory + ", logMetadataSuffix=" + logMetadataSuffix + "]";
  }

}
