package com.hubspot.singularity.api;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
public interface SingularityPriorityFreezeIF {

  @ApiModelProperty(required=true, value="Kill (if killTasks is true) or do not launch (if killTasks is false) tasks below this priority level")
  double getMinimumPriorityLevel();

  @ApiModelProperty(required=true, value="If true, kill currently running tasks, and do not launch new tasks below the minimumPriorityLevel. If false, do not launch new tasks below minimumPriorityLevel")
  boolean isKillTasks();

  @ApiModelProperty(required=false, value="An optional message/reason for creating the priority kill")
  Optional<String> getMessage();

  @ApiModelProperty(required=false, value="A unique ID for this priority kill")
  Optional<String> getActionId();
}
