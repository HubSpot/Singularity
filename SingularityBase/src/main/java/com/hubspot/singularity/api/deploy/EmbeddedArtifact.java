package com.hubspot.singularity.api.deploy;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Raw content used to create an artifact in the task sandbox")
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
  @Schema(description = "Raw content for the file")
  public byte[] getContent() {
    return content;
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
  public int hashCode() {
    return Objects.hash(super.hashCode(), content);
  }

  @Override
  public String toString() {
    return "EmbeddedArtifact{" +
        "content=" + Arrays.toString(content) +
        "} " + super.toString();
  }
}
