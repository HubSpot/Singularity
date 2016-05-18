package com.hubspot.singularity.config;

import java.util.List;

import com.google.common.base.Optional;

public class SingularityTaskMetadataConfiguration {

  private long taskPersistAfterFinishBufferMillis = 0;

  private long waitToSendTaskCompletedMailBufferMillis = 0;

  private Optional<List<String>> allowedMetadataTypes = Optional.absent();

  private Optional<String> sendTaskCompletedMailOnceMetadataTypeIsAvailable = Optional.absent();

  private long maxMetadataMessageBytes = 10000;

  public long getTaskPersistAfterFinishBufferMillis() {
    return taskPersistAfterFinishBufferMillis;
  }

  public void setTaskPersistAfterFinishBufferMillis(long taskPersistAfterFinishBufferMillis) {
    this.taskPersistAfterFinishBufferMillis = taskPersistAfterFinishBufferMillis;
  }

  public long getWaitToSendTaskCompletedMailBufferMillis() {
    return waitToSendTaskCompletedMailBufferMillis;
  }

  public void setWaitToSendTaskCompletedMailBufferMillis(long waitToSendTaskCompletedMailBufferMillis) {
    this.waitToSendTaskCompletedMailBufferMillis = waitToSendTaskCompletedMailBufferMillis;
  }

  public Optional<List<String>> getAllowedMetadataTypes() {
    return allowedMetadataTypes;
  }

  public void setAllowedMetadataTypes(Optional<List<String>> allowedMetadataTypes) {
    this.allowedMetadataTypes = allowedMetadataTypes;
  }

  public Optional<String> getSendTaskCompletedMailOnceMetadataTypeIsAvailable() {
    return sendTaskCompletedMailOnceMetadataTypeIsAvailable;
  }

  public void setSendTaskCompletedMailOnceMetadataTypeIsAvailable(Optional<String> sendTaskCompletedMailOnceMetadataTypeIsAvailable) {
    this.sendTaskCompletedMailOnceMetadataTypeIsAvailable = sendTaskCompletedMailOnceMetadataTypeIsAvailable;
  }

  public long getMaxMetadataMessageBytes() {
    return maxMetadataMessageBytes;
  }

  public void setMaxMetadataMessageBytes(long maxMetadataMessageBytes) {
    this.maxMetadataMessageBytes = maxMetadataMessageBytes;
  }
}
