package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityMachineChangeRequest {

  private final Optional<String> message;

  @JsonCreator
  public SingularityMachineChangeRequest(@JsonProperty("message") Optional<String> message) {
    this.message = message;
  }

  @ApiModelProperty(required=false, value="A message to show to users about why this action was taken")
  public Optional<String> getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return "SingularityMachineChangeRequest [message=" + message + "]";
  }

}
