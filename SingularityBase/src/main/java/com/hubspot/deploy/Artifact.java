package com.hubspot.deploy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @Type(value = ExternalArtifact.class, name = "external"),
  @Type(value = EmbeddedArtifact.class, name = "embedded")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Artifact {
  
  private final String name;
  private final String filename;
  private final Optional<String> md5sum;
  
  public Artifact(String name, String filename, Optional<String> md5sum) {
    super();
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
