package com.hubspot.singularity.executor.shells;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.hubspot.singularity.SingularityTaskShellCommandRequest;
import com.hubspot.singularity.SingularityTaskShellCommandUpdate;
import com.hubspot.singularity.SingularityTaskShellCommandUpdate.UpdateType;
import com.hubspot.singularity.executor.task.SingularityExecutorTask;

public class SingularityExecutorShellCommandRunner {

  private final SingularityTaskShellCommandRequest shellRequest;
  private final SingularityExecutorTask task;
  private final ListeningExecutorService shellCommandExecutorService;
  private final ObjectMapper objectMapper;

  public SingularityExecutorShellCommandRunner(SingularityTaskShellCommandRequest shellRequest, SingularityExecutorTask task, ObjectMapper objectMapper, ListeningExecutorService shellCommandExecutorService) {
    this.shellRequest = shellRequest;
    this.task = task;
    this.objectMapper = objectMapper;
    this.shellCommandExecutorService = shellCommandExecutorService;
  }

  public SingularityTaskShellCommandRequest getShellRequest() {
    return shellRequest;
  }

  public SingularityExecutorTask getTask() {
    return task;
  }

  public ProcessBuilder buildProcessBuilder() {
    return new ProcessBuilder(buildCommand());
  }

  public void start() {
    sendUpdate(UpdateType.ACKED, Optional.<String> absent());

    SingularityExecutorShellCommandRunnerCallable callable = new SingularityExecutorShellCommandRunnerCallable(this);

    ListenableFuture<Integer> shellFuture = shellCommandExecutorService.submit(callable);

    Futures.addCallback(shellFuture, new FutureCallback<Integer>() {

      @Override
      public void onSuccess(Integer result) {
        task.getLog().info("ShellRequest {} finished with {}", shellRequest, result);

        sendUpdate(UpdateType.FINISHED, Optional.of(String.format("Finished with code %s", result)));
      }

      @Override
      public void onFailure(Throwable t) {
        task.getLog().warn("ShellRequest {} failed", shellRequest, t);

        sendUpdate(UpdateType.FAILED, Optional.of(String.format("Failed - %s (%s)", t.getClass().getSimpleName(), t.getMessage())));
      }

    });
  }

  private List<String> buildCommand() {
    return Arrays.asList(shellRequest.getShellCommand().getName());
  }

  // TODO thread?
  public void sendUpdate(UpdateType updateType, Optional<String> message) {
    SingularityTaskShellCommandUpdate update = new SingularityTaskShellCommandUpdate(shellRequest.getId(), System.currentTimeMillis(), message, updateType);

    try {
      byte[] data = objectMapper.writeValueAsBytes(update);

      task.getLog().info("Sending update {} ({}) for shell command {}", updateType, message.or(""), shellRequest.getId());

      task.getDriver().sendFrameworkMessage(data);

    } catch (JsonProcessingException e) {
      task.getLog().error("Unable to serialize update {}", update, e);
    }
  }

}
