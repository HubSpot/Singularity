package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;

public class SingularityTaskMetadata extends SingularityTaskIdHolder implements Comparable<SingularityTaskMetadata> {

  private static final MetadataLevel DEFAULT_METADATA_LEVEL = MetadataLevel.INFO;
  private final long timestamp;
  private final String type;
  private final String title;
  private final MetadataLevel level;
  private final Optional<String> message;
  private final Optional<String> user;

  @JsonCreator
  public SingularityTaskMetadata(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("timestamp") long timestamp, @JsonProperty("type") String type, @JsonProperty("title") String title,
      @JsonProperty("message") Optional<String> message, @JsonProperty("user") Optional<String> user, @JsonProperty("level") Optional<MetadataLevel> level) {
    super(taskId);
    Preconditions.checkNotNull(type);
    Preconditions.checkState(!type.contains("/"));
    this.timestamp = timestamp;
    this.type = type;
    this.title = title;
    this.message = message;
    this.user = user;
    this.level = level.or(DEFAULT_METADATA_LEVEL);
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public Optional<String> getMessage() {
    return message;
  }

  public Optional<String> getUser() {
    return user;
  }

  public MetadataLevel getLevel() { return level; }

  @Override
  public int compareTo(SingularityTaskMetadata o) {
    return ComparisonChain.start()
        .compare(timestamp, o.getTimestamp())
        .compare(type, o.getType())
        .compare(level, o.getLevel())
        .compare(getTaskId().getId(), o.getTaskId().getId())
        .result();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(user, type, title, message, timestamp, getTaskId());
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
        return true;
    }
    if (other == null || other.getClass() != this.getClass()) {
        return false;
    }

    SingularityTaskMetadata that = (SingularityTaskMetadata) other;

    return Objects.equal(this.user, that.user)
            && Objects.equal(this.type, that.type)
            && Objects.equal(this.title, that.title)
            && Objects.equal(this.message, that.message)
            && Objects.equal(this.timestamp, that.timestamp)
            && Objects.equal(this.getTaskId(), that.getTaskId())
            && Objects.equal(this.level, that.level);
  }

  @Override
  public String toString() {
    return "SingularityTaskMetadata [timestamp=" + timestamp + ", type=" + type + ", title=" + title + ", message=" + message + ", user=" + user + ", taskId=" + getTaskId() + ", level=" + getLevel() + "]";
  }

}
