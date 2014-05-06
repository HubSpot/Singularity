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
  private final long logrotateAfterBytes;
  private final long retryDelaySeconds;
  
  private final String logrotateCommand;
  private final String logrotateToDirectory;
  private final String logrotateMaxageDays;
  private final String logrotateCount; 
  private final String logrotateDateformat;
  
  private final Path logMetadataDirectory;
  private final String logMetadataSuffix;
  
  private final String s3MetadataSuffix;
  private final Path s3MetadataDirectory;
  
  private final String s3KeyPattern;
  private final String s3Bucket;
  
  @Inject
  public SingularityLogWatcherConfiguration(
      @Named(SingularityLogWatcherConfigurationLoader.BYTE_BUFFER_CAPACITY) String byteBufferCapacity, 
      @Named(SingularityLogWatcherConfigurationLoader.FLUENTD_TAG_PREFIX) String fluentdTagPrefix,
      @Named(SingularityLogWatcherConfigurationLoader.POLL_MILLIS) String pollMillis, 
      @Named(SingularityLogWatcherConfigurationLoader.FLUENTD_HOSTS) String fluentdHosts, 
      @Named(SingularityLogWatcherConfigurationLoader.STORE_DIRECTORY) String storeDirectory, 
      @Named(SingularityLogWatcherConfigurationLoader.STORE_SUFFIX) String storeSuffix, 
      @Named(SingularityLogWatcherConfigurationLoader.LOGROTATE_AFTER_BYTES) String logrotateAfterBytes,
      @Named(SingularityLogWatcherConfigurationLoader.RETRY_DELAY_SECONDS) String retryDelaySeconds,
      @Named(SingularityLogWatcherConfigurationLoader.LOGROTATE_COMMAND) String logrotateCommand, 
      @Named(SingularityLogWatcherConfigurationLoader.LOGROTATE_COUNT) String logrotateCount, 
      @Named(SingularityLogWatcherConfigurationLoader.LOGROTATE_MAXAGE_DAYS) String logrotateMaxageDays, 
      @Named(SingularityLogWatcherConfigurationLoader.LOGROTATE_DATEFORMAT) String logrotateDateformat, 
      @Named(SingularityLogWatcherConfigurationLoader.LOGROTATE_DIRECTORY) String logrotateToDirectory,
      @Named(SingularityLogWatcherConfigurationLoader.LOG_METADATA_DIRECTORY) String logMetadataDirectory,
      @Named(SingularityLogWatcherConfigurationLoader.LOG_METADATA_SUFFIX) String logMetadataSuffix,
      @Named(SingularityLogWatcherConfigurationLoader.S3_UPLOADER_BUCKET) String s3Bucket,
      @Named(SingularityLogWatcherConfigurationLoader.S3_UPLOADER_PATTERN) String s3KeyPattern,
      @Named(SingularityLogWatcherConfigurationLoader.S3_METADATA_DIRECTORY) String s3MetadataDirectory,
      @Named(SingularityLogWatcherConfigurationLoader.S3_METADATA_SUFFIX) String s3MetadataSuffix
      ) {
    this.byteBufferCapacity = Integer.parseInt(byteBufferCapacity);
    this.pollMillis = Long.parseLong(pollMillis);
    this.fluentdHosts = parseFluentdHosts(fluentdHosts);
    this.storeSuffix = storeSuffix;
    this.fluentdTagPrefix = fluentdTagPrefix;
    this.storeDirectory = JavaUtils.getValidDirectory(storeDirectory, SingularityLogWatcherConfigurationLoader.STORE_DIRECTORY);
    this.logrotateAfterBytes = Long.parseLong(logrotateAfterBytes);
    this.retryDelaySeconds = Long.parseLong(retryDelaySeconds);
    this.logrotateCommand = logrotateCommand;
    this.logrotateToDirectory = logrotateToDirectory;
    this.logrotateCount = logrotateCount;
    this.logrotateMaxageDays = logrotateMaxageDays;
    this.logrotateDateformat = logrotateDateformat;
    this.logMetadataSuffix = logMetadataSuffix;
    this.logMetadataDirectory = JavaUtils.getValidDirectory(logMetadataDirectory, SingularityLogWatcherConfigurationLoader.LOG_METADATA_DIRECTORY);
    this.s3Bucket = s3Bucket;
    this.s3KeyPattern = s3KeyPattern;
    this.s3MetadataSuffix = s3MetadataSuffix;
    this.s3MetadataDirectory = JavaUtils.getValidDirectory(s3MetadataDirectory, SingularityLogWatcherConfigurationLoader.S3_METADATA_DIRECTORY);
  }
    
  public String getLogrotateCommand() {
    return logrotateCommand;
  }
  
  public String getS3MetadataSuffix() {
    return s3MetadataSuffix;
  }

  public Path getS3MetadataDirectory() {
    return s3MetadataDirectory;
  }

  public String getS3KeyPattern() {
    return s3KeyPattern;
  }

  public String getS3Bucket() {
    return s3Bucket;
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
  
  public String getLogrotateToDirectory() {
    return logrotateToDirectory;
  }

  public long getLogrotateAfterBytes() {
    return logrotateAfterBytes;
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

  public String getLogrotateMaxageDays() {
    return logrotateMaxageDays;
  }

  public String getLogrotateCount() {
    return logrotateCount;
  }

  public String getLogrotateDateformat() {
    return logrotateDateformat;
  }

  @Override
  public String toString() {
    return "SingularityLogWatcherConfiguration [byteBufferCapacity=" + byteBufferCapacity + ", pollMillis=" + pollMillis + ", fluentdHosts=" + fluentdHosts + ", storeDirectory=" + storeDirectory + ", storeSuffix=" + storeSuffix
        + ", fluentdTagPrefix=" + fluentdTagPrefix + ", logrotateAfterBytes=" + logrotateAfterBytes + ", retryDelaySeconds=" + retryDelaySeconds + ", logrotateCommand=" + logrotateCommand + ", logrotateToDirectory=" + logrotateToDirectory
        + ", logrotateMaxageDays=" + logrotateMaxageDays + ", logrotateCount=" + logrotateCount + ", logrotateDateformat=" + logrotateDateformat + ", logMetadataDirectory=" + logMetadataDirectory + ", logMetadataSuffix="
        + logMetadataSuffix + ", s3MetadataSuffix=" + s3MetadataSuffix + ", s3MetadataDirectory=" + s3MetadataDirectory + ", s3KeyPattern=" + s3KeyPattern + ", s3Bucket=" + s3Bucket + "]";
  }

}
