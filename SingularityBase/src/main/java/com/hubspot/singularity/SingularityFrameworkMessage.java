package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = Id.MINIMAL_CLASS, include = As.PROPERTY, property = "@class", defaultImpl = SingularityTaskShellCommandRequest.class)
@JsonSubTypes({
  @Type(value = SingularityTaskShellCommandRequest.class, name = "SHELL_COMMAND"),
  @Type(value = SingularityTaskDestroyFrameworkMessage.class, name = "TASK_KILL")
})
public abstract class SingularityFrameworkMessage {
  public abstract SingularityTaskId getTaskId();
}
