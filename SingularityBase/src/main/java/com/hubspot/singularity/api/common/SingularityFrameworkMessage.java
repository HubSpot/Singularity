package com.hubspot.singularity.api.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.hubspot.singularity.api.task.SingularityTaskDestroyFrameworkMessage;
import com.hubspot.singularity.api.task.SingularityTaskId;
import com.hubspot.singularity.api.task.SingularityTaskShellCommandRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = Id.MINIMAL_CLASS, include = As.PROPERTY, property = "@class", defaultImpl = SingularityTaskShellCommandRequest.class)
@JsonSubTypes({
  @Type(value = SingularityTaskShellCommandRequest.class, name = "SHELL_COMMAND"),
  @Type(value = SingularityTaskDestroyFrameworkMessage.class, name = "TASK_KILL")
})
@Schema(
    subTypes = {
        SingularityTaskShellCommandRequest.class,
        SingularityTaskDestroyFrameworkMessage.class
    }
)
public abstract class SingularityFrameworkMessage {
  public abstract SingularityTaskId getTaskId();
}
