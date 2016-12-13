package com.hubspot.singularity.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityPriorityFreeze {
  private final double minimumPriorityLevel;
  private final boolean killTasks;
  private final Optional<String> message;
  private final Optional<String> actionId;

  @JsonCreator
  public SingularityPriorityFreeze(@JsonProperty("minimumPriorityLevel") double minimumPriorityLevel, @JsonProperty("killTasks") boolean killTasks, @JsonProperty("message") Optional<String> message, @JsonProperty("actionId") Optional<String> actionId) {
    this.minimumPriorityLevel = minimumPriorityLevel;
    this.killTasks = killTasks;
    this.message = message;
    this.actionId = actionId;
  }

  @ApiModelProperty(required=true, value="Kill (if killTasks is true) or do not launch (if killTasks is false) tasks below this priority level")
  public double getMinimumPriorityLevel() {
    return minimumPriorityLevel;
  }

  @ApiModelProperty(required=true, value="If true, kill currently running tasks, and do not launch new tasks below the minimumPriorityLevel. If false, do not launch new tasks below minimumPriorityLevel")
  public boolean isKillTasks() {
    return killTasks;
  }

  @ApiModelProperty(required=false, value="An optional message/reason for creating the priority kill")
  public Optional<String> getMessage() {
    return message;
  }

  @ApiModelProperty(required=false, value="A unique ID for this priority kill")
  public Optional<String> getActionId() {
    return actionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityPriorityFreeze that = (SingularityPriorityFreeze) o;
    return Double.compare(that.minimumPriorityLevel, minimumPriorityLevel) == 0 &&
      Objects.equals(killTasks, that.killTasks) &&
      Objects.equals(message, that.message) &&
      Objects.equals(actionId, that.actionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(minimumPriorityLevel, message, actionId);
  }

  @Override
  public String toString() {
    return "SingularityPriorityFreeze[" +
      "minimumPriorityLevel=" + minimumPriorityLevel +
      ", killTasks=" + killTasks +
      ", message=" + message +
      ", actionId=" + actionId +
      ']';
  }
}
