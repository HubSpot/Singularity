package com.hubspot.singularity.logwatcher.config;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SingularityLogWatcherConfiguration {

  private final int byteBufferCapacity;
  private final long minimimReadSizeBytes;
  private final long pollMillis;
  private final List<FluentdHost> fluentdHosts;
  
  @Inject
  public SingularityLogWatcherConfiguration(@Named(SingularityLogWatcherConfigurationLoader.BYTE_BUFFER_CAPACITY) String byteBufferCapacity, @Named(SingularityLogWatcherConfigurationLoader.MINIMUM_READ_SIZE_BYTES) String minimimReadSizeBytes,
      @Named(SingularityLogWatcherConfigurationLoader.POLL_MILLIS) String pollMillis, @Named(SingularityLogWatcherConfigurationLoader.FLUENTD_HOSTS) String fluentdHosts) {
    this.byteBufferCapacity = Integer.parseInt(byteBufferCapacity);
    this.minimimReadSizeBytes = Long.parseLong(minimimReadSizeBytes);
    this.pollMillis = Long.parseLong(pollMillis);
    this.fluentdHosts = parseFluentdHosts(fluentdHosts);
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

  public long getMinimimReadSizeBytes() {
    return minimimReadSizeBytes;
  }
  
  public List<FluentdHost> getFluentdHosts() {
    return fluentdHosts;
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
  
  @Override
  public String toString() {
    return "SingularityLogWatcherConfiguration [byteBufferCapacity=" + byteBufferCapacity + ", minimimReadSizeBytes=" + minimimReadSizeBytes + ", pollMillis=" + pollMillis + "]";
  }
  
}
