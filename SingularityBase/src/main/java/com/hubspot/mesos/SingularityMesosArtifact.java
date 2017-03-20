package com.hubspot.mesos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityMesosArtifact {
  private final String uri;
  private final boolean cache;
  private final boolean executable;
  private final boolean extract;

  @JsonCreator
  public static SingularityMesosArtifact fromString(String uri) {
    return new SingularityMesosArtifact(uri, Optional.<Boolean>absent(), Optional.<Boolean>absent(), Optional.<Boolean>absent());
  }

  @JsonCreator
  public SingularityMesosArtifact(@JsonProperty("uri") String uri,
                                  @JsonProperty("cache") Optional<Boolean> cache,
                                  @JsonProperty("executable") Optional<Boolean> executable,
                                  @JsonProperty("extract") Optional<Boolean> extract) {
    this.uri = uri;
    this.cache = cache.or(false);
    this.executable = executable.or(false);
    this.extract = extract.or(true);
  }

  public String getUri() {
    return uri;
  }

  public boolean isCache() {
    return cache;
  }

  public boolean isExecutable() {
    return executable;
  }

  public boolean isExtract() {
    return extract;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityMesosArtifact that = (SingularityMesosArtifact) o;
    return cache == that.cache &&
        executable == that.executable &&
        extract == that.extract &&
        Objects.equals(uri, that.uri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri, cache, executable, extract);
  }

  @Override
  public String toString() {
    return "SingularityMesosArtifact{" +
        "uri='" + uri + '\'' +
        ", cache=" + cache +
        ", executable=" + executable +
        ", extract=" + extract +
        '}';
  }
}
