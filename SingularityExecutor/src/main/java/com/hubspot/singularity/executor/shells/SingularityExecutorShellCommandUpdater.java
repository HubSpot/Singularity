package com.hubspot.singularity.executor.shells;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityTaskShellCommandRequest;
import com.hubspot.singularity.SingularityTaskShellCommandUpdate;
import com.hubspot.singularity.SingularityTaskShellCommandUpdate.UpdateType;
import com.hubspot.singularity.executor.task.SingularityExecutorTask;

public class SingularityExecutorShellCommandUpdater {

  private final ObjectMapper objectMapper;
  private final SingularityTaskShellCommandRequest shellRequest;
  private final SingularityExecutorTask task;

  public SingularityExecutorShellCommandUpdater(ObjectMapper objectMapper, SingularityTaskShellCommandRequest shellRequest, SingularityExecutorTask task) {
    this.objectMapper = objectMapper;
    this.shellRequest = shellRequest;
    this.task = task;
  }

  // TODO thread?
  public void sendUpdate(UpdateType updateType, Optional<String> message, Optional<String> outputFilename) {
    SingularityTaskShellCommandUpdate update = new SingularityTaskShellCommandUpdate(shellRequest.getId(), System.currentTimeMillis(), message, outputFilename, updateType);

    try {
      byte[] data = objectMapper.writeValueAsBytes(update);

      task.getLog().info("Sending update {} ({}) for shell command {}", updateType, message.or(""), shellRequest.getId());

      task.getDriver().sendFrameworkMessage(data);

    } catch (JsonProcessingException e) {
      task.getLog().error("Unable to serialize update {}", update, e);
    }
  }


}
