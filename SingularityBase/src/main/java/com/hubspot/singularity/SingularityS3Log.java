package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel( description = "Represents a task sandbox file that was uploaded to S3" )
public class SingularityS3Log {

  private final String getUrl;
  private final String key;
  private final long lastModified;
  private final long size;
  private final Optional<String> downloadUrl;

  @JsonCreator
  public SingularityS3Log(@JsonProperty("getUrl") String getUrl, @JsonProperty("key") String key, @JsonProperty("lastModified") long lastModified, @JsonProperty("size") long size, @JsonProperty("downloadUrl") Optional<String> downloadUrl) {
    this.getUrl = getUrl;
    this.key = key;
    this.lastModified = lastModified;
    this.size = size;
    this.downloadUrl = downloadUrl;
  }

  @ApiModelProperty("URL to file in S3")
  public String getGetUrl() {
    return getUrl;
  }

  @ApiModelProperty("S3 key")
  public String getKey() {
    return key;
  }

  @ApiModelProperty("Last modified time")
  public long getLastModified() {
    return lastModified;
  }

  @ApiModelProperty("File size (in bytes)")
  public long getSize() {
    return size;
  }

  @ApiModelProperty("URL to file in S3 to explicitly download")
  public Optional<String> getDownloadUrl() {
    return downloadUrl;
  }

  @Override
  public String toString() {
    return "SingularityS3Log [getUrl=" + getUrl + ", key=" + key + ", lastModified=" + lastModified + ", size=" + size + ", downloadUrl=" + downloadUrl + "]";
  }
}
