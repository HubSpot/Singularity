package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema( title = "Represents a task sandbox file that was uploaded to S3" )
public class SingularityS3Log extends SingularityS3LogMetadata {
  private final String getUrl;
  private final String downloadUrl;

  @JsonCreator
  public SingularityS3Log(@JsonProperty("getUrl") String getUrl, @JsonProperty("key") String key, @JsonProperty("lastModified") long lastModified, @JsonProperty("size") long size, @JsonProperty("downloadUrl") String downloadUrl,
                          @JsonProperty("startTime") Optional<Long> startTime, @JsonProperty("endTime") Optional<Long> endTime) {
    super(key, lastModified, size, startTime, endTime);
    this.getUrl = getUrl;
    this.downloadUrl = downloadUrl;
  }

  @Schema(description = "URL to file in S3")
  public String getGetUrl() {
    return getUrl;
  }

  @Schema(description = "URL to file in S3 containing headers that will force file to be downloaded instead of viewed")
  public String getDownloadUrl() {
    return downloadUrl;
  }

  @Override
  public String toString() {
    return "SingularityS3Log{" +
        "getUrl='" + getUrl + '\'' +
        ", downloadUrl='" + downloadUrl + '\'' +
        "} " + super.toString();
  }
}
