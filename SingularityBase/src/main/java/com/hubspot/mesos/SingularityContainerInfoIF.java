package com.hubspot.mesos;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
@JsonDeserialize(as = SingularityContainerInfo.class)
public interface SingularityContainerInfoIF {
  @ApiModelProperty(required = true, value = "Container type, can be MESOS or DOCKER. Default is MESOS")
  SingularityContainerType getType();

  @ApiModelProperty(required = false, value = "List of volumes to mount. Applicable only to DOCKER container type")
  Optional<List<SingularityVolume>> getVolumes();

  @ApiModelProperty(required = false, value = "Information specific to docker runtime settings")
  Optional<SingularityDockerInfo> getDocker();
}
