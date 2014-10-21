package com.hubspot.deploy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class ExternalArtifact extends RemoteArtifact {

  private final String url;

  @JsonCreator
  public ExternalArtifact(@JsonProperty("name") String name, @JsonProperty("filename") String filename, @JsonProperty("md5sum") Optional<String> md5sum, @JsonProperty("url") String url, @JsonProperty("filesize") Optional<Long> filesize) {
    super(name, filename, md5sum, filesize);
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public String toString() {
    return "ExternalArtifact [url=" + url + ", getFilesize()=" + getFilesize() + ", getName()=" + getName() + ", getFilename()=" + getFilename() + ", getMd5sum()=" + getMd5sum() + "]";
  }

}
