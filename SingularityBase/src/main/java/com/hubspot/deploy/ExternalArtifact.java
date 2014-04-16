package com.hubspot.deploy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class ExternalArtifact extends Artifact {
  
  private final String url;
  private final long filesize;  

  @JsonCreator
  public ExternalArtifact(@JsonProperty("name") String name, @JsonProperty("filename") String filename, @JsonProperty("md5sum") Optional<String> md5sum, @JsonProperty("url") String url, @JsonProperty("filesize") long filesize) {
    super(name, filename, md5sum);
    this.url = url;
    this.filesize = filesize;
  }

  public String getUrl() {
    return url;
  }

  public long getFilesize() {
    return filesize;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("name", getName())
        .add("filaname", getFilename())
        .add("md5sum", getMd5sum())
        .add("url", url)
        .add("filesize", filesize)
        .toString();
  }
  
}
