package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import java.util.Optional;

@Schema(description = "Warning to users users about potential destructive actions")
public class SingularityFireAlarm {
  private final String title;
  private final String message;
  private final Optional<String> url;

  @JsonCreator
  public SingularityFireAlarm(
    @JsonProperty("title") String title,
    @JsonProperty("message") String message,
    @JsonProperty("url") Optional<String> url
  ) {
    this.title = title;
    this.message = message;
    this.url = url;
  }

  @Schema(required = true, description = "Fire alarm title")
  public String getTitle() {
    return title;
  }

  @Schema(required = true, description = "Fire alarm message")
  public String getMessage() {
    return message;
  }

  @Schema(required = true, description = "Fire alarm url to link to for more information")
  public Optional<String> getUrl() {
    return url;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityFireAlarm that = (SingularityFireAlarm) o;
    return (
      Objects.equals(title, that.title) &&
      Objects.equals(message, that.message) &&
      Objects.equals(url, that.url)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, message, url);
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("title", title)
      .add("message", message)
      .add("url", url)
      .toString();
  }
}
