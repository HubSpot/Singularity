package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityPauseRequest extends SingularityExpiringRequestParent {

  private final Optional<Boolean> killTasks;

  @JsonCreator
  public SingularityPauseRequest(@JsonProperty("killTasks") Optional<Boolean> killTasks,@JsonProperty("durationMillis") Optional<Long> durationMillis,
      @JsonProperty("actionId") Optional<String> actionId, @JsonProperty("message") Optional<String> message) {
    super(durationMillis, actionId, message);

    this.killTasks = killTasks;
  }

  @ApiModelProperty(required=false, value="If set to false, tasks will be allowed to finish instead of killed immediately")
  public Optional<Boolean> getKillTasks() {
    return killTasks;
  }

  @Override
  public String toString() {
    return "SingularityPauseRequest [killTasks=" + killTasks + ", toString()=" + super.toString() + "]";
  }

}
