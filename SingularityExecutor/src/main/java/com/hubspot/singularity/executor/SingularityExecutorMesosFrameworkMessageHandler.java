package com.hubspot.singularity.executor;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityFrameworkMessage;
import com.hubspot.singularity.SingularityTaskDestroyFrameworkMessage;
import com.hubspot.singularity.SingularityTaskShellCommandRequest;
import com.hubspot.singularity.SingularityTaskShellCommandUpdate.UpdateType;
import com.hubspot.singularity.executor.SingularityExecutorMonitor.KillState;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.shells.SingularityExecutorShellCommandRunner;
import com.hubspot.singularity.executor.shells.SingularityExecutorShellCommandUpdater;
import com.hubspot.singularity.executor.task.SingularityExecutorTask;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskProcessCallable;

public class SingularityExecutorMesosFrameworkMessageHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityExecutorMesosFrameworkMessageHandler.class);

  private final SingularityExecutorMonitor monitor;
  private final SingularityExecutorConfiguration executorConfiguration;
  private final ObjectMapper objectMapper;

  @Inject
  public SingularityExecutorMesosFrameworkMessageHandler(ObjectMapper objectMapper, SingularityExecutorMonitor monitor, SingularityExecutorConfiguration executorConfiguration) {
    this.objectMapper = objectMapper;
    this.monitor = monitor;
    this.executorConfiguration = executorConfiguration;
  }

  public void handleMessage(byte[] data) {
    try {
      SingularityFrameworkMessage message = objectMapper.readValue(data, SingularityFrameworkMessage.class);
      if (message.getClass().equals(SingularityTaskShellCommandRequest.class)) {
        handleShellRequest((SingularityTaskShellCommandRequest) message);
      } else if (message.getClass().equals(SingularityTaskDestroyFrameworkMessage.class)) {
        handleTaskDestroyMessage((SingularityTaskDestroyFrameworkMessage) message);
      } else {
        throw new IOException(String.format("Do not know how to handle framework message of class %s", message.getClass()));
      }
    } catch (IOException e) {
      LOG.error("Do not know how to handle framework message {}", new String(data, UTF_8), e);
    }
  }

  private void handleTaskDestroyMessage(SingularityTaskDestroyFrameworkMessage taskDestroyMessage) {
    KillState killState = monitor.requestKill(taskDestroyMessage.getTaskId().getId(), taskDestroyMessage.getUser(), true);

    switch (killState) {
      case DIDNT_EXIST:
      case INCONSISTENT_STATE:
        LOG.warn("Couldn't destroy task {} due to killState {}", taskDestroyMessage.getTaskId(), killState);
        break;
      case DESTROYING_PROCESS:
      case INTERRUPTING_PRE_PROCESS:
      case KILLING_PROCESS:
        LOG.info("Requested destroy of task {} with killState {}", taskDestroyMessage.getTaskId(), killState);
        break;
    }
  }

  private void handleShellRequest(SingularityTaskShellCommandRequest shellRequest) {
    Optional<SingularityExecutorTask> matchingTask = monitor.getTask(shellRequest.getTaskId().getId());

    if (!matchingTask.isPresent()) {
      LOG.warn("Missing task for {}, ignoring shell request", shellRequest.getTaskId());
      return;
    }

    matchingTask.get().getLog().info("Received shell request {}", shellRequest);

    SingularityExecutorShellCommandUpdater updater = new SingularityExecutorShellCommandUpdater(objectMapper, shellRequest, matchingTask.get());

    Optional<SingularityExecutorTaskProcessCallable> taskProcess = monitor.getTaskProcess(shellRequest.getTaskId().getId());

    if (!taskProcess.isPresent()) {
      updater.sendUpdate(UpdateType.INVALID, Optional.of("No task process found"), Optional.<String>absent());
      return;
    }

    SingularityExecutorShellCommandRunner shellRunner = new SingularityExecutorShellCommandRunner(shellRequest, executorConfiguration, matchingTask.get(),
      taskProcess.get(), monitor.getShellCommandExecutorServiceForTask(shellRequest.getTaskId().getId()), updater);

    shellRunner.start();
  }

}
