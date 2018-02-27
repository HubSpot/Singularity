package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    title = "Metadata about a log file stored in s3",
    subTypes = {
        SingularityS3Log.class
    }
)
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

  @Schema(description = "S3 key")
  public String getKey() {
    return key;
  }

  @Schema(description = "Last modified time")
  public long getLastModified() {
    return lastModified;
  }

  @Schema(description = "File size (in bytes)")
  public long getSize() {
    return size;
  }

  @Schema(description = "Time the log file started being written to", nullable = true)
  public Optional<Long> getStartTime() {
    return startTime;
  }

  @Schema(description = "Time the log file was finished being written to", nullable = true)
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
