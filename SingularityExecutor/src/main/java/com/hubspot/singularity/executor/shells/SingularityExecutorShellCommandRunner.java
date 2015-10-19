package com.hubspot.singularity.executor.shells;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.SingularityTaskShellCommandRequest;
import com.hubspot.singularity.SingularityTaskShellCommandUpdate.UpdateType;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.task.SingularityExecutorTask;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskProcessCallable;

public class SingularityExecutorShellCommandRunner {

  private final SingularityTaskShellCommandRequest shellRequest;
  private final SingularityExecutorTask task;
  private final SingularityExecutorTaskProcessCallable taskProcess;
  private final ListeningExecutorService shellCommandExecutorService;
  private final SingularityExecutorShellCommandUpdater shellCommandUpdater;
  private final SingularityExecutorConfiguration executorConfiguration;

  @SuppressWarnings("serial")
  private static class InvalidShellCommandException extends RuntimeException {

    public InvalidShellCommandException(String message) {
      super(message);
    }

  }

  public SingularityExecutorShellCommandRunner(SingularityTaskShellCommandRequest shellRequest, SingularityExecutorConfiguration executorConfiguration, SingularityExecutorTask task,
      SingularityExecutorTaskProcessCallable taskProcess, ListeningExecutorService shellCommandExecutorService, SingularityExecutorShellCommandUpdater shellCommandUpdater) {
    this.shellRequest = shellRequest;
    this.executorConfiguration = executorConfiguration;
    this.task = task;
    this.taskProcess = taskProcess;
    this.shellCommandUpdater = shellCommandUpdater;
    this.shellCommandExecutorService = shellCommandExecutorService;
  }

  public SingularityTaskShellCommandRequest getShellRequest() {
    return shellRequest;
  }

  public SingularityExecutorTask getTask() {
    return task;
  }

  public ProcessBuilder buildProcessBuilder(List<String> command, File outputFile) {
    ProcessBuilder builder = new ProcessBuilder(command);

    builder.redirectOutput(ProcessBuilder.Redirect.appendTo(outputFile));
    builder.redirectError(ProcessBuilder.Redirect.appendTo(outputFile));

    return builder;
  }

  public void start() {
    List<String> command = null;

    try {
      command = buildCommand();
    } catch (InvalidShellCommandException isce) {
      shellCommandUpdater.sendUpdate(UpdateType.INVALID, Optional.of(isce.getMessage()));
      return;
    }

    shellCommandUpdater.sendUpdate(UpdateType.ACKED, Optional.of(Joiner.on(" ").join(command)));

    final File outputFile = MesosUtils.getTaskDirectoryPath(getTask().getTaskId()).resolve(executorConfiguration.getShellCommandOutFile()).toFile();

    SingularityExecutorShellCommandRunnerCallable callable = new SingularityExecutorShellCommandRunnerCallable(task.getLog(), shellCommandUpdater, buildProcessBuilder(command, outputFile), outputFile);

    ListenableFuture<Integer> shellFuture = shellCommandExecutorService.submit(callable);

    Futures.addCallback(shellFuture, new FutureCallback<Integer>() {

      @Override
      public void onSuccess(Integer result) {
        task.getLog().info("ShellRequest {} finished with {}", shellRequest, result);

        shellCommandUpdater.sendUpdate(UpdateType.FINISHED, Optional.of(String.format("Finished with code %s", result)));
      }

      @Override
      public void onFailure(Throwable t) {
        task.getLog().warn("ShellRequest {} failed", shellRequest, t);

        shellCommandUpdater.sendUpdate(UpdateType.FAILED, Optional.of(String.format("Failed - %s (%s)", t.getClass().getSimpleName(), t.getMessage())));
      }

    });
  }

  private List<String> buildCommand() {
    Optional<SingularityExecutorShellCommandDescriptor> matchingShellCommandDescriptor = Iterables.tryFind(executorConfiguration.getShellCommands(), new Predicate<SingularityExecutorShellCommandDescriptor>() {

      @Override
      public boolean apply(SingularityExecutorShellCommandDescriptor input) {
        return input.getName().equals(shellRequest.getShellCommand().getName());
      }

    });

    if (!matchingShellCommandDescriptor.isPresent()) {
      throw new InvalidShellCommandException(String.format("%s not found in matching commands %s", shellRequest.getShellCommand().getName(), executorConfiguration.getShellCommands()));
    }

    final SingularityExecutorShellCommandDescriptor shellCommandDescriptor = matchingShellCommandDescriptor.get();

    List<String> command = new ArrayList<>(shellCommandDescriptor.getCommand());

    for (int i = 0; i < command.size(); i++) {
      if (command.get(i).equals(executorConfiguration.getShellCommandPidPlaceholder())) {
        if (!taskProcess.getCurrentPid().isPresent()) {
          throw new InvalidShellCommandException("No PID found");
        }
        command.set(i, Integer.toString(taskProcess.getCurrentPid().get()));
      } else if (command.get(i).equals(executorConfiguration.getShellCommandUserPlaceholder())) {
        command.set(i, taskProcess.getTask().getExecutorData().getUser().or(executorConfiguration.getDefaultRunAsUser()));
      }
    }

    if (shellRequest.getShellCommand().getOptions().isPresent()) {
      for (SingularityExecutorShellCommandOptionDescriptor option : shellCommandDescriptor.getOptions()) {
        if (shellRequest.getShellCommand().getOptions().get().contains(option.getName())) {
          command.add(option.getFlag());
        }
      }
    }

    return command;
  }


}
