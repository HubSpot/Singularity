package com.hubspot.singularity.executor;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskShellCommandRequest;

public class SingularityExecutorMesosFrameworkMessageHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityExecutorMesosFrameworkMessageHandler.class);

  private final ObjectMapper objectMapper;

  @Inject
  public SingularityExecutorMesosFrameworkMessageHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void handleMessage(byte[] data) {
    try {
      SingularityTaskShellCommandRequest shellRequest = objectMapper.readValue(data, SingularityTaskShellCommandRequest.class);

      // validate shell command + taskId

      LOG.info("Received shell request {}", shellRequest);
    } catch (IOException e) {
      LOG.warn("Framework message {} not a shell request", new String(data, UTF_8));
    }
  }

}
