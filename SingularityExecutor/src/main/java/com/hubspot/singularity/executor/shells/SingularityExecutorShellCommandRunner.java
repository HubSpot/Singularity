package com.hubspot.singularity.executor.shells;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.hubspot.singularity.SingularityTaskShellCommandRequest;
import com.hubspot.singularity.SingularityTaskShellCommandUpdate.UpdateType;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.task.SingularityExecutorTask;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskProcessCallable;
import com.hubspot.singularity.executor.utils.MesosUtils;

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

  public static String convertCommandNameToLogfileName(String str) {
    return CharMatcher.WHITESPACE.or(CharMatcher.is('/')).replaceFrom(str, '-').toLowerCase();
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
      shellCommandUpdater.sendUpdate(UpdateType.INVALID, Optional.of(isce.getMessage()), Optional.<String>absent());
      return;
    }

    final String outputFilename = executorConfiguration.getShellCommandOutFile()
        .replace("{NAME}", shellRequest.getShellCommand().getLogfileName().or(convertCommandNameToLogfileName(shellRequest.getShellCommand().getName())))
        .replace("{TIMESTAMP}", Long.toString(shellRequest.getTimestamp()));

    shellCommandUpdater.sendUpdate(UpdateType.ACKED, Optional.of(Joiner.on(" ").join(command)), Optional.of(outputFilename));

    final File outputFile = MesosUtils.getTaskDirectoryPath(getTask().getTaskId()).resolve(outputFilename).toFile();

    SingularityExecutorShellCommandRunnerCallable callable = new SingularityExecutorShellCommandRunnerCallable(task.getLog(), shellCommandUpdater, buildProcessBuilder(command, outputFile), outputFile);

    ListenableFuture<Integer> shellFuture = shellCommandExecutorService.submit(callable);

    Futures.addCallback(shellFuture, new FutureCallback<Integer>() {

      @Override
      public void onSuccess(Integer result) {
        task.getLog().info("ShellRequest {} finished with {}", shellRequest, result);

        shellCommandUpdater.sendUpdate(UpdateType.FINISHED, Optional.of(String.format("Finished with code %s", result)), Optional.<String>absent());
      }

      @Override
      public void onFailure(Throwable t) {
        task.getLog().warn("ShellRequest {} failed", shellRequest, t);

        shellCommandUpdater.sendUpdate(UpdateType.FAILED, Optional.of(String.format("Failed - %s (%s)", t.getClass().getSimpleName(), t.getMessage())), Optional.<String>absent());
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

    List<String> command = new ArrayList<>();

    if (!shellCommandDescriptor.isSkipCommandPrefix()) {
      command.addAll(executorConfiguration.getShellCommandPrefix());
    }

    boolean isDocker = task.getTaskInfo().hasContainer() && task.getTaskInfo().getContainer().hasDocker();
    if (isDocker) {
      command.addAll(Arrays.asList("docker", "exec", String.format("%s%s", executorConfiguration.getDockerPrefix(), task.getTaskId())));
    }

    command.addAll(shellCommandDescriptor.getCommand());

    for (int i = 0; i < command.size(); i++) {
      if (command.get(i).equals(executorConfiguration.getShellCommandPidPlaceholder())) {
        int pid;
        Path pidFilePath = MesosUtils.getTaskDirectoryPath(getTask().getTaskId()).resolve(executorConfiguration.getShellCommandPidFile());
        if (Files.exists(pidFilePath)) {
          Scanner scanner = null;
          try {
            scanner = new Scanner(pidFilePath);
            scanner.useDelimiter("\\Z");
            pid = Integer.parseInt(scanner.next());
          } catch (Exception e) {
            throw new InvalidShellCommandException(String.format("No PID found due to exception reading pid file: %s", e.getMessage()));
          } finally {
            if (scanner != null) {
              try {
                scanner.close();
              } catch (Throwable t) {}
            }
          }
        } else if (isDocker) {
          pid = 1;
        } else {
          if (!taskProcess.getCurrentPid().isPresent()) {
            throw new InvalidShellCommandException("No PID found");
          }
          pid = taskProcess.getCurrentPid().get();
        }
        command.set(i, Integer.toString(pid));
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
