package com.hubspot.singularity.executor.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.spotify.docker.client.ContainerNotFoundException;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.ContainerInfo;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.runner.base.shared.SimpleProcessManager;

public class SingularityExecutorTaskCleanup {

  private final SingularityExecutorTaskDefinition taskDefinition;
  private final SingularityExecutorTaskLogManager taskLogManager;
  private final SingularityExecutorConfiguration configuration;
  private final Logger log;
  private final DockerClient dockerClient;

  public SingularityExecutorTaskCleanup(SingularityExecutorTaskLogManager taskLogManager, SingularityExecutorConfiguration configuration, SingularityExecutorTaskDefinition taskDefinition, Logger log, DockerClient dockerClient) {
    this.configuration = configuration;
    this.taskLogManager = taskLogManager;
    this.taskDefinition = taskDefinition;
    this.log = log;
    this.dockerClient = dockerClient;
  }

  public boolean cleanup(boolean cleanupTaskAppDirectory, boolean isDocker) {
    final Path taskDirectory = Paths.get(taskDefinition.getTaskDirectory());

    boolean dockerCleanSuccess = true;
    if (isDocker) {
      dockerCleanSuccess = cleanDocker();
    }

    if (!Files.exists(taskDirectory)) {
      log.info("Directory {} didn't exist for cleanup", taskDirectory);
      taskLogManager.removeLogrotateFile();
      return (cleanTaskDefinitionFile() && dockerCleanSuccess);
    }

    boolean logTearDownSuccess = taskLogManager.teardown();
    boolean cleanupTaskAppDirectorySuccess = false;

    if (cleanupTaskAppDirectory) {
      cleanupTaskAppDirectorySuccess = cleanupTaskAppDirectory();
    }

    log.info("Cleaned up logs ({}) and task app directory ({})", logTearDownSuccess, cleanupTaskAppDirectorySuccess);

    if (logTearDownSuccess && cleanupTaskAppDirectorySuccess) {
      return (cleanTaskDefinitionFile() && dockerCleanSuccess);
    }

    return false;
  }

  public boolean cleanTaskDefinitionFile() {
    Path taskDefinitionPath = configuration.getTaskDefinitionPath(taskDefinition.getTaskId());

    log.info("Successful cleanup, deleting file {}", taskDefinitionPath);

    try {
      boolean deleted = Files.deleteIfExists(taskDefinitionPath);

      log.info("File deleted ({})", deleted);

      return true;
    } catch (IOException e) {
      log.error("Failed deleting {}", taskDefinitionPath, e);
      return false;
    }
  }

  private boolean cleanupTaskAppDirectory() {
    final String pathToDelete = taskDefinition.getTaskAppDirectory();

    log.info("Deleting: {}", pathToDelete);

    try {
      final List<String> cmd = ImmutableList.of(
          "rm",
          "-rf",
          pathToDelete
          );

      new SimpleProcessManager(log).runCommand(cmd);

      return true;
    } catch (Throwable t) {
      log.error("While deleting directory {}", pathToDelete, t);
    }

    return false;
  }

  private boolean cleanDocker() {
    String containerName = String.format("%s%s", configuration.getDockerPrefix(), taskDefinition.getTaskId());
    try {
      ContainerInfo containerInfo = dockerClient.inspectContainer(containerName);
      if (containerInfo.state().running()) {
        dockerClient.stopContainer(containerName, configuration.getDockerStopTimeout());
      }
      dockerClient.removeContainer(containerName);
      log.info(String.format("Removed container %s", containerName));
      return true;
    } catch (ContainerNotFoundException e) {
      log.info(String.format("Container %s was already removed", containerName));
      return true;
    } catch (Exception e) {
      log.info(String.format("Could not ensure removal of docker container due to error %s", e));
    }
    return false;
  }

}
