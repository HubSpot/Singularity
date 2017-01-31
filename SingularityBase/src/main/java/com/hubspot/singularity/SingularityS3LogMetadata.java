package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityS3LogMetadata {
  public static final String LOG_START_S3_ATTR = "starttime";
  public static final String LOG_END_S3_ATTR = "endtime";

  private final String key;
  private final long lastModified;
  private final long size;
  private final Optional<Long> startTime;
  private final Optional<Long> endTime;

  @JsonCreator
  public SingularityS3LogMetadata(@JsonProperty("key") String key, @JsonProperty("lastModified") long lastModified, @JsonProperty("size") long size,
                                  @JsonProperty("startTime") Optional<Long> startTime, @JsonProperty("endTime") Optional<Long> endTime) {
    this.key = key;
    this.lastModified = lastModified;
    this.size = size;
    this.startTime = startTime;
    this.endTime = endTime;
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

  @ApiModelProperty("Time the log file started being written to")
  public Optional<Long> getStartTime() {
    return startTime;
  }

  @ApiModelProperty("Time the log file was finished being written to")
  public Optional<Long> getEndTime() {
    return endTime;
  }

  @Override
  public String toString() {
    return "SingularityS3Log{" +
        "key='" + key + '\'' +
        ", lastModified=" + lastModified +
        ", size=" + size +
        ", startTime=" + startTime +
        ", endTime=" + endTime +
        '}';
  }
}
