package com.hubspot.singularity;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityTaskDestroyFrameworkMessage extends SingularityFrameworkMessage {
  private final SingularityTaskId taskId;
  private final Optional<String> user;

  @JsonCreator
  public SingularityTaskDestroyFrameworkMessage(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("user") Optional<String> user) {
    this.taskId = taskId;
    this.user = user;
  }

  public SingularityTaskId getTaskId() {
    return taskId;
  }

  public Optional<String> getUser() {
    return user;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityTaskDestroyFrameworkMessage that = (SingularityTaskDestroyFrameworkMessage) o;
    return Objects.equals(taskId, that.taskId) &&
        Objects.equals(user, that.user);
  }

  @Override
  public int hashCode() {
    return Objects.hash(taskId, user);
  }

  @Override
  public String toString() {
    return "SingularityTaskDestroyFrameworkMessage{" +
        "taskId=" + taskId +
        ", user=" + user +
        "} " + super.toString();
  }
}
