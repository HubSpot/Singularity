package com.hubspot.deploy;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddedArtifact extends Artifact {

  private final byte[] content;

  @JsonCreator
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public EmbeddedArtifact(@JsonProperty("name") String name, @JsonProperty("filename") String filename, @JsonProperty("md5sum") Optional<String> md5sum,
      @JsonProperty("content") byte[] content, @JsonProperty("targetFolderRelativeToTask") Optional<String> targetFolderRelativeToTask) {
    super(name, filename, md5sum, targetFolderRelativeToTask);
    this.content = content;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getContent() {
    return content;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), content);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    EmbeddedArtifact that = (EmbeddedArtifact) o;
    return Arrays.equals(content, that.content);
  }

  @Override
  public String toString() {
    return "EmbeddedArtifact [parent=" + super.toString() + "]";
  }

}
