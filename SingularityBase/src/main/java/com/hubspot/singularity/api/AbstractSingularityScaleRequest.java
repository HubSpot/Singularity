package com.hubspot.singularity.api;

import java.util.UUID;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityScaleRequest extends SingularityExpiringRequestParent {

  @ApiModelProperty(required=false, value="If set to true, healthchecks will be skipped while scaling this request (only)")
  public abstract Optional<Boolean> getSkipHealthchecks();

  @ApiModelProperty(required=false, value="The number of instances to scale to")
  public abstract Optional<Integer> getInstances();

  @ApiModelProperty(required=false, value="Bounce the request to get to the new scale")
  public abstract Optional<Boolean> getBounce();

  @ApiModelProperty(required=false, value="If present and set to true, old tasks will be killed as soon as replacement tasks are available, instead of waiting for all replacement tasks to be healthy")
  public abstract Optional<Boolean> getIncremental();

  @ApiModelProperty(required=false, value="A message to show to users about why this action was taken")
  public abstract Optional<String> getMessage();

  @Default
  @ApiModelProperty(required=false, value="An id to associate with this action for metadata purposes")
  public String getActionId() {
    return UUID.randomUUID().toString();
  }

  @ApiModelProperty(required=false, value="The number of milliseconds to wait before reversing the effects of this action (letting it expire)")
  public abstract Optional<Long> getDurationMillis();

}
