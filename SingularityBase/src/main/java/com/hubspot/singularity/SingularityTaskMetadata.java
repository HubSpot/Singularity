package com.hubspot.singularity;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityTaskMetadata that = (SingularityTaskMetadata) o;
    return timestamp == that.timestamp &&
        Objects.equals(type, that.type) &&
        Objects.equals(title, that.title) &&
        level == that.level &&
        Objects.equals(message, that.message) &&
        Objects.equals(user, that.user);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, type, title, level, message, user);
  }

  @Override
  public String toString() {
    return "SingularityTaskMetadata{" +
        "timestamp=" + timestamp +
        ", type='" + type + '\'' +
        ", title='" + title + '\'' +
        ", level=" + level +
        ", message=" + message +
        ", user=" + user +
        "} " + super.toString();
  }
}
