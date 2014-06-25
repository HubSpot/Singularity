package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityS3Log {
  
  private final String getUrl;
  private final String key;
  private final long lastModified;
  private final long size;
  
  @JsonCreator
  public SingularityS3Log(@JsonProperty("getUrl") String getUrl, @JsonProperty("key") String key, @JsonProperty("lastModified") long lastModified, @JsonProperty("size") long size) {
    this.getUrl = getUrl;
    this.key = key;
    this.lastModified = lastModified;
    this.size = size;
  }

  public String getGetUrl() {
    return getUrl;
  }

  public String getKey() {
    return key;
  }

  public long getLastModified() {
    return lastModified;
  }

  public long getSize() {
    return size;
  }

  @Override
  public String toString() {
    return "SingularityS3Log [getUrl=" + getUrl + ", key=" + key + ", lastModified=" + lastModified + ", size=" + size + "]";
  }
  
}
