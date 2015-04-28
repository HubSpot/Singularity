package com.hubspot.singularity.executor;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskShellCommandRequest;
import com.hubspot.singularity.executor.shells.SingularityExecutorShellCommandRunner;
import com.hubspot.singularity.executor.task.SingularityExecutorTask;

public class SingularityExecutorMesosFrameworkMessageHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityExecutorMesosFrameworkMessageHandler.class);

  private final SingularityExecutorMonitor monitor;
  private final ObjectMapper objectMapper;

  @Inject
  public SingularityExecutorMesosFrameworkMessageHandler(ObjectMapper objectMapper, SingularityExecutorMonitor monitor) {
    this.objectMapper = objectMapper;
    this.monitor = monitor;
  }

  public void handleMessage(byte[] data) {
    try {
      SingularityTaskShellCommandRequest shellRequest = objectMapper.readValue(data, SingularityTaskShellCommandRequest.class);

      Optional<SingularityExecutorTask> matchingTask = monitor.getTask(shellRequest.getTaskId().getId());

      if (!matchingTask.isPresent()) {
        LOG.warn("Missing task for {}, ignoring shell request", shellRequest.getTaskId());
        return;
      }

      SingularityExecutorShellCommandRunner shellRunner = new SingularityExecutorShellCommandRunner(shellRequest, matchingTask.get(), objectMapper, monitor.createExecutorService(shellRequest.getTaskId().getId()));

      shellRunner.start();

      LOG.info("Received shell request {}", shellRequest);
    } catch (IOException e) {
      LOG.warn("Framework message {} not a shell request", new String(data, UTF_8));
    }
  }

}
