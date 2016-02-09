package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class SingularityTaskMetadataRequest {

  private final String type;
  private final String title;
  private final Optional<String> message;

  @JsonCreator
  public SingularityTaskMetadataRequest(@JsonProperty("type") String type, @JsonProperty("title") String title, @JsonProperty("message") Optional<String> message) {
    Preconditions.checkNotNull(type);
    Preconditions.checkState(!type.contains("/"));
    this.type = type;
    this.title = title;
    this.message = message;
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

  @Override
  public String toString() {
    return "SingularityTaskMetadataRequest [type=" + type + ", title=" + title + ", message=" + message + "]";
  }

}
