package com.hubspot.deploy;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddedArtifact extends Artifact {

  private final byte[] content;

  @JsonCreator
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public EmbeddedArtifact(@JsonProperty("name") String name, @JsonProperty("filename") String filename, @JsonProperty("md5sum") Optional<String> md5sum, @JsonProperty("content") byte[] content) {
    super(name, filename, md5sum);
    this.content = content;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getContent() {
    return content;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("name", getName())
        .add("filaname", getFilename())
        .add("md5sum", getMd5sum())
        .toString();
  }

}
