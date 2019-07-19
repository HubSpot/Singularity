package com.hubspot.singularity.s3uploader;

import java.util.Optional;

public class UploaderFileAttributes {
  Optional<Long> startTime;
  Optional<Long> endTime;

  public UploaderFileAttributes(Optional<Long> startTime, Optional<Long> endTime) {
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public Optional<Long> getStartTime() {
    return startTime;
  }

  public Optional<Long> getEndTime() {
    return endTime;
  }
}
