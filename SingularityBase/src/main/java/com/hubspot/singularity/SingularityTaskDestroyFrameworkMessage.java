package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
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
    return Objects.equal(taskId, that.taskId) &&
      Objects.equal(user, that.user);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(taskId, user);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("taskId", taskId)
      .add("user", user)
      .toString();
  }
}
