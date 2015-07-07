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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((filename == null) ? 0 : filename.hashCode());
    result = prime * result + ((md5sum == null) ? 0 : md5sum.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Artifact other = (Artifact) obj;
    if (filename == null) {
      if (other.filename != null)
        return false;
    } else if (!filename.equals(other.filename))
      return false;
    if (md5sum == null) {
      if (other.md5sum != null)
        return false;
    } else if (!md5sum.equals(other.md5sum))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Artifact [name=" + name + ", filename=" + filename + ", md5sum=" + md5sum + "]";
  }

}
