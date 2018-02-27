package com.hubspot.mesos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes an artifact to be downloaded")
public class SingularityMesosArtifact {
  private final String uri;
  private final boolean cache;
  private final boolean executable;
  private final boolean extract;
  private final Optional<String> outputFile;

  @JsonCreator
  public static SingularityMesosArtifact fromString(String uri) {
    return new SingularityMesosArtifact(uri, Optional.<Boolean>absent(), Optional.<Boolean>absent(), Optional.<Boolean>absent(), Optional.absent());
  }

  @JsonCreator
  public SingularityMesosArtifact(@JsonProperty("uri") String uri,
                                  @JsonProperty("cache") Optional<Boolean> cache,
                                  @JsonProperty("executable") Optional<Boolean> executable,
                                  @JsonProperty("extract") Optional<Boolean> extract,
                                  @JsonProperty("outputFile") Optional<String> outputFile) {
    this.uri = uri;
    this.cache = cache.or(false);
    this.executable = executable.or(false);
    this.extract = extract.or(true);
    this.outputFile = outputFile;
  }

  @Schema(required = true, description = "The uri of the artifact to download")
  public String getUri() {
    return uri;
  }

  @Schema(
      description = "Cache this artifact to avoid multiple downloads on the same host",
      defaultValue = "false"
  )
  public boolean isCache() {
    return cache;
  }

  @Schema(
      description = "If true, chmod the download file so it is executable",
      defaultValue = "false"
  )
  public boolean isExecutable() {
    return executable;
  }

  @Schema(
      title = "If true, unpack the downloaded artifact in teh sandbox directory",
      defaultValue = "true",
      description = "Recognized file types: .tar, .tar.gz, .tar.bz2, .tar.xz, .gz, .tgz, .tbz2, .txz, .zip"
  )
  public boolean isExtract() {
    return extract;
  }

  @Schema(
      title = "The destination filename for the download",
      nullable = true,
      defaultValue = "original name of the file"
  )
  public Optional<String> getOutputFile() {
    return outputFile;
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
