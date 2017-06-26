package com.hubspot.singularity.api;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.mesos.Resources;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
public interface SingularityRunNowRequestIF {

  @ApiModelProperty(required=false, value="A message to show to users about why this action was taken")
  Optional<String> getMessage();

  @ApiModelProperty(required=false, value="If set to true, healthchecks will be skipped for this task run")
  Optional<Boolean> getSkipHealthchecks();

  @ApiModelProperty(required=false, value="An id to associate with this request which will be associated with the corresponding launched tasks")
  Optional<String> getRunId();

  @ApiModelProperty(required=false, value="Command line arguments to be passed to the task")
  Optional<List<String>> getCommandLineArgs();

  @ApiModelProperty(required=false, value="Override the resources from the active deploy for this run")
  Optional<Resources> getResources();

  @ApiModelProperty(required=false, value="Schedule this task to run at a specified time")
  Optional<Long> getRunAt();
}
