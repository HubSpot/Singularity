package com.hubspot.mesos;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public interface SingularityMesosArtifactIF {
  String getUri();

  @Default
  default boolean isCache() {
    return false;
  }

  @Default
  default boolean isExecutable() {
    return false;
  }

  @Default
  default boolean isExtract() {
    return false;
  }

  @JsonCreator
  static SingularityMesosArtifact fromString(String uri) {
    return SingularityMesosArtifact.builder().setUri(uri).build();
  }

  @Deprecated
  static SingularityMesosArtifact of(String uri,
                                            Optional<Boolean> cache,
                                            Optional<Boolean> executable,
                                            Optional<Boolean> extract) {
    return SingularityMesosArtifact.builder()
        .setUri(uri)
        .setCache(cache.or(false))
        .setExecutable(executable.or(false))
        .setExecutable(extract.or(false))
        .build();
  }
}
