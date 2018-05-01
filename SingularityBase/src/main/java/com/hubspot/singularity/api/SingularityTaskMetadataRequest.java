package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.hubspot.singularity.MetadataLevel;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to add custom metadata to a task")
public class SingularityTaskMetadataRequest {

  private final String type;
  private final String title;
  private final Optional<String> message;
  private final Optional<MetadataLevel> level;

  @JsonCreator
  public SingularityTaskMetadataRequest(@JsonProperty("type") String type, @JsonProperty("title") String title, @JsonProperty("message") Optional<String> message, @JsonProperty("level") Optional<MetadataLevel> level) {
    Preconditions.checkNotNull(type);
    Preconditions.checkState(!type.contains("/"));
    this.type = type;
    this.title = title;
    this.message = message;
    this.level = level;
  }

  @Schema(required = true, description = "A type to be associated with this metadata")
  public String getType() {
    return type;
  }

  @Schema(required = true, description = "A title to be associated with this metadata")
  public String getTitle() {
    return title;
  }

  @Schema(description = "An optional message")
  public Optional<String> getMessage() {
    return message;
  }

  @Schema(description = "Level of metadata, can be INFO, WARN, or ERROR", nullable = true)
  public Optional<MetadataLevel> getLevel() { return level; }

  @Override
  public String toString() {
    return "SingularityTaskMetadataRequest{" +
        "type='" + type + '\'' +
        ", title='" + title + '\'' +
        ", message=" + message +
        ", level=" + level +
        '}';
  }
}
