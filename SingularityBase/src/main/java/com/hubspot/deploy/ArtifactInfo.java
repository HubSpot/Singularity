package com.hubspot.deploy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class ArtifactInfo {
  
  private final String url;
  private final long filesize;
  private final Optional<String> md5sum;

  @JsonCreator
  public ArtifactInfo(@JsonProperty("url") String url, @JsonProperty("filesize") long filesize, @JsonProperty("md5sum") Optional<String> md5sum) {
    this.url = url;
    this.filesize = filesize;
    this.md5sum = md5sum;
  }

  public String getUrl() {
    return url;
  }

  public long getFilesize() {
    return filesize;
  }

  public Optional<String> getMd5sum() {
    return md5sum;
  }

  @Override
  public String toString() {
    return "ArtifactInfo [url=" + url + ", filesize=" + filesize + ", md5sum=" + md5sum + "]";
  }
  
}
