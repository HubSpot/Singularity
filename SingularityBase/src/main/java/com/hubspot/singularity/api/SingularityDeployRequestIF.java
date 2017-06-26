package com.hubspot.singularity.api;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
public interface SingularityDeployRequestIF {

  @ApiModelProperty(required=true, value="The Singularity deploy object, containing all the required details about the Deploy")
  SingularityDeploy getDeploy();

  @ApiModelProperty(required=false, value="A message to show users about this deploy (metadata)")
  Optional<String> getMessage();

  @ApiModelProperty(required=false, value="use this request data for this deploy, and update the request on successful deploy")
  Optional<SingularityRequest> getUpdatedRequest();

  @JsonIgnore
  @Default
  @ApiModelProperty(required=false, value="If deploy is successful, also unpause the request")
  default boolean isUnpauseOnSuccessfulDeploy() {
    return false;
  }

}
