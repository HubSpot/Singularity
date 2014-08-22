package com.hubspot.deploy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Artifact {

  private final String name;
  private final String filename;
  private final Optional<String> md5sum;

  public Artifact(String name, String filename, Optional<String> md5sum) {
    this.name = name;
    this.filename = filename;
    this.md5sum = md5sum;
  }

  public String getName() {
    return name;
  }

  public String getFilename() {
    return filename;
  }

  public Optional<String> getMd5sum() {
    return md5sum;
  }

}
