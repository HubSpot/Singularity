package com.hubspot.deploy;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class ExternalArtifact extends RemoteArtifact {

  private final String url;

  @JsonCreator
  public ExternalArtifact(@JsonProperty("name") String name, @JsonProperty("filename") String filename, @JsonProperty("md5sum") Optional<String> md5sum,
      @JsonProperty("url") String url, @JsonProperty("filesize") Optional<Long> filesize, @JsonProperty("targetFolderRelativeToTask") Optional<String> targetFolderRelativeToTask) {
    super(name, filename, md5sum, filesize, targetFolderRelativeToTask);
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), url);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || other.getClass() != this.getClass()) {
      return false;
    }
    if (!super.equals(other)) {
      return false;
    }

    ExternalArtifact that = (ExternalArtifact) other;
    return Objects.equals(this.url, that.url);
  }

  @Override
  public String toString() {
    return "ExternalArtifact [url=" + url + ", parent=" + super.toString() + "]";
  }

}
