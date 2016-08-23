package com.hubspot.singularity.config;

import java.util.List;

import com.google.common.base.Optional;

public class SingularityTaskMetadataConfiguration {

  private long taskPersistAfterFinishBufferMillis = 0;

  private long waitToSendTaskCompletedMailBufferMillis = 0;

  private Optional<List<String>> allowedMetadataTypes = Optional.absent();

  private Optional<String> sendTaskCompletedMailOnceMetadataTypeIsAvailable = Optional.absent();

  private long maxMetadataMessageLength = 10000;

  private long maxMetadataTitleLength = 2000;

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

  public long getMaxMetadataMessageLength() {
    return maxMetadataMessageLength;
  }

  public void setMaxMetadataMessageLength(long maxMetadataMessageLength) {
    this.maxMetadataMessageLength = maxMetadataMessageLength;
  }

  public long getMaxMetadataTitleLength() {
    return maxMetadataTitleLength;
  }

  public void setMaxMetadataTitleLength(long maxMetadataTitleLength) {
    this.maxMetadataTitleLength = maxMetadataTitleLength;
  }
}
