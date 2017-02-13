package com.hubspot.mesos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityMesosArtifact {
  private final String uri;
  private final boolean cache;
  private final boolean executable;
  private final boolean extract;

  @JsonCreator
  public static SingularityMesosArtifact fromString(String uri) {
    return new SingularityMesosArtifact(uri, false, false, false);
  }

  @JsonCreator
  public SingularityMesosArtifact(@JsonProperty("uri") String uri,
                                  @JsonProperty("cache") boolean cache,
                                  @JsonProperty("executable") boolean executable,
                                  @JsonProperty("extract") boolean extract) {
    this.uri = uri;
    this.cache = cache;
    this.executable = executable;
    this.extract = extract;
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

    if (cache != that.cache) {
      return false;
    }
    if (executable != that.executable) {
      return false;
    }
    if (extract != that.extract) {
      return false;
    }
    return uri != null ? uri.equals(that.uri) : that.uri == null;
  }

  @Override
  public int hashCode() {
    int result = uri != null ? uri.hashCode() : 0;
    result = 31 * result + (cache ? 1 : 0);
    result = 31 * result + (executable ? 1 : 0);
    result = 31 * result + (extract ? 1 : 0);
    return result;
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
