package com.hubspot.singularity.logwatcher.config;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SingularityLogWatcherConfiguration {

  private final int byteBufferCapacity;
  private final long minimimReadSizeBytes;
  private final long pollMillis;
  
  @Inject
  public SingularityLogWatcherConfiguration(@Named(SingularityLogWatcherConfigurationLoader.BYTE_BUFFER_CAPACITY) String byteBufferCapacity, @Named(SingularityLogWatcherConfigurationLoader.MINIMUM_READ_SIZE_BYTES) String minimimReadSizeBytes,
      @Named(SingularityLogWatcherConfigurationLoader.POLL_MILLIS) String pollMillis) {
    this.byteBufferCapacity = Integer.parseInt(byteBufferCapacity);
    this.minimimReadSizeBytes = Long.parseLong(minimimReadSizeBytes);
    this.pollMillis = Long.parseLong(pollMillis);
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

  @Override
  public String toString() {
    return "SingularityLogWatcherConfiguration [byteBufferCapacity=" + byteBufferCapacity + ", minimimReadSizeBytes=" + minimimReadSizeBytes + ", pollMillis=" + pollMillis + "]";
  }
  
}
