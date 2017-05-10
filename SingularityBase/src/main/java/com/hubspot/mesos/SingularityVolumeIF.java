package com.hubspot.mesos;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
@JsonDeserialize(as = SingularityVolume.class)
public interface SingularityVolumeIF {
  String getContainerPath();

  Optional<String> getHostPath();

  Optional<SingularityDockerVolumeMode> getMode();
}
