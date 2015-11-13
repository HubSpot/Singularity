package com.hubspot.singularity.executor;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskShellCommandRequest;
import com.hubspot.singularity.SingularityTaskShellCommandUpdate.UpdateType;
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
      SingularityTaskShellCommandRequest shellRequest = objectMapper.readValue(data, SingularityTaskShellCommandRequest.class);

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
    } catch (IOException e) {
      LOG.warn("Framework message {} not a shell request", new String(data, UTF_8));
    }
  }

}
