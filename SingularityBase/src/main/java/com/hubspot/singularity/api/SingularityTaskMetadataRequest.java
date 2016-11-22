package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.hubspot.singularity.MetadataLevel;
import com.wordnik.swagger.annotations.ApiModelProperty;

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

  @ApiModelProperty(required=true, value="A type to be associated with this metadata")
  public String getType() {
    return type;
  }

  @ApiModelProperty(required=true, value="A title to be associated with this metadata")
  public String getTitle() {
    return title;
  }

  @ApiModelProperty(required=false, value="An optional message")
  public Optional<String> getMessage() {
    return message;
  }

  @ApiModelProperty(required=false, value="Level of metadata, can be INFO, WARN, or ERROR")
  public Optional<MetadataLevel> getLevel() { return level; }

  @Override
  public String toString() {
    return "SingularityTaskMetadataRequest [type=" + type + ", title=" + title + ", message=" + message + ", level=" + level + "]";
  }

}
