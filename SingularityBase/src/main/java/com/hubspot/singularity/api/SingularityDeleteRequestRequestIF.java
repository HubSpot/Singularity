package com.hubspot.singularity.api;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
@JsonDeserialize(as = SingularityDeleteRequestRequest.class)
public interface SingularityDeleteRequestRequestIF {

  @ApiModelProperty(required=false, value="A message to show to users about why this action was taken")
  Optional<String> getMessage();

  @ApiModelProperty(required=false, value="An id to associate with this action for metadata purposes")
  Optional<String> getActionId();

  @ApiModelProperty(required = false, value = "Should the service associated with the request be removed from the load balancer")
  Optional<Boolean> getDeleteFromLoadBalancer();
}
