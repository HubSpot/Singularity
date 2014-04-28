package com.hubspot.singularity.logwatcher.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SingularityLogWatcherConfiguration {

  private final int byteBufferCapacity;
  private final long pollMillis;
  private final List<FluentdHost> fluentdHosts;
  private final Path storeDirectory;
  private final String storeSuffix;
  
  @Inject
  public SingularityLogWatcherConfiguration(@Named(SingularityLogWatcherConfigurationLoader.BYTE_BUFFER_CAPACITY) String byteBufferCapacity,
      @Named(SingularityLogWatcherConfigurationLoader.POLL_MILLIS) String pollMillis, @Named(SingularityLogWatcherConfigurationLoader.FLUENTD_HOSTS) String fluentdHosts, 
      @Named(SingularityLogWatcherConfigurationLoader.STORE_DIRECTORY) String storeDirectory, @Named(SingularityLogWatcherConfigurationLoader.STORE_SUFFIX) String storeSuffix) {
    this.byteBufferCapacity = Integer.parseInt(byteBufferCapacity);
    this.pollMillis = Long.parseLong(pollMillis);
    this.fluentdHosts = parseFluentdHosts(fluentdHosts);
    this.storeSuffix = storeSuffix;
    this.storeDirectory = Paths.get(storeDirectory);
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

  @Override
  public String toString() {
    return "SingularityLogWatcherConfiguration [byteBufferCapacity=" + byteBufferCapacity + ", pollMillis=" + pollMillis + ", fluentdHosts=" + fluentdHosts + ", storeDirectory=" + storeDirectory + ", storeSuffix=" + storeSuffix + "]";
  }
  
}
