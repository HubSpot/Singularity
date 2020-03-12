package com.hubspot.singularity.executor.task;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.config.SingularityExecutorLogrotateAdditionalFile;
import com.hubspot.singularity.executor.utils.DockerUtils;
import com.hubspot.singularity.runner.base.shared.ExceptionChainParser;
import com.hubspot.singularity.runner.base.shared.SimpleProcessManager;
import com.spotify.docker.client.exceptions.ContainerNotFoundException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerInfo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class SingularityExecutorTaskCleanup {

  private final SingularityExecutorTaskDefinition taskDefinition;
  private final SingularityExecutorTaskLogManager taskLogManager;
  private final SingularityExecutorConfiguration configuration;
  private final Logger log;
  private final DockerUtils dockerUtils;

  public SingularityExecutorTaskCleanup(SingularityExecutorTaskLogManager taskLogManager, SingularityExecutorConfiguration configuration, SingularityExecutorTaskDefinition taskDefinition, Logger log, DockerUtils dockerUtils) {
    this.configuration = configuration;
    this.taskLogManager = taskLogManager;
    this.taskDefinition = taskDefinition;
    this.log = log;
    this.dockerUtils = dockerUtils;
  }

  public TaskCleanupResult cleanup(boolean cleanupTaskAppDirectory, boolean cleanupLogs, boolean isDocker) {
    final Path taskDirectory = Paths.get(taskDefinition.getTaskDirectory());

    boolean dockerCleanSuccess = true;
    if (isDocker) {
      try {
        String containerName = String.format("%s%s", configuration.getDockerPrefix(), taskDefinition.getTaskId());
        ContainerInfo containerInfo = dockerUtils.inspectContainer(containerName);
        if (containerInfo.state().running()) {
          dockerUtils.stopContainer(containerName, configuration.getDockerStopTimeout());
        }
        dockerUtils.removeContainer(containerName, true);
      } catch (DockerException e) {
        if (ExceptionChainParser.exceptionChainContains(e, ContainerNotFoundException.class)) {
          log.trace("Container for task {} was already removed", taskDefinition.getTaskId());
        } else {
          log.error("Could not ensure removal of container", e);
          dockerCleanSuccess = false;
        }
      } catch (Exception e) {
        log.error("Could not ensure removal of container", e);
        dockerCleanSuccess = false;
      }
    }

    if (!Files.exists(taskDirectory)) {
      log.info("Directory {} didn't exist for cleanup", taskDirectory);
      taskLogManager.removeLogrotateFile();
      return finishTaskCleanup(dockerCleanSuccess);
    }


    if (!cleanupTaskAppDirectory) {
      log.info("Not finishing cleanup because taskApp directory is being preserved");
      return TaskCleanupResult.WAITING;
    }

    boolean cleanupTaskAppDirectorySuccess = cleanupTaskAppDirectory();

    log.info("Cleaned up task app directory ({})", cleanupTaskAppDirectorySuccess);

    if (!cleanupLogs) {
      log.info("Not finishing cleanup because log files will be preserved for 15 minutes after task termination");
      return TaskCleanupResult.WAITING;
    }

    boolean logTearDownSuccess = taskLogManager.teardown();
    checkForLogrotateAdditionalFilesToDelete(taskDefinition);

    log.info("Cleaned up logs ({})", logTearDownSuccess);

    if (logTearDownSuccess && cleanupTaskAppDirectorySuccess) {
      return finishTaskCleanup(dockerCleanSuccess);
    } else {
      return TaskCleanupResult.ERROR;
    }
  }

  public void cleanUpLogs() {
    taskLogManager.teardown();
  }

  private TaskCleanupResult finishTaskCleanup(boolean dockerCleanSuccess) {
    boolean cleanTaskDefinitionFile = cleanTaskDefinitionFile();

    if (cleanTaskDefinitionFile && dockerCleanSuccess) {
      return TaskCleanupResult.SUCCESS;
    }

    return TaskCleanupResult.ERROR;
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

  private void checkForLogrotateAdditionalFilesToDelete(SingularityExecutorTaskDefinition taskDefinition) {
    configuration.getLogrotateAdditionalFiles()
        .stream()
        .filter(SingularityExecutorLogrotateAdditionalFile::isDeleteInExecutorCleanup)
        .forEach(toDelete -> {
          String glob = String.format("glob:%s/%s", taskDefinition.getTaskDirectoryPath().toAbsolutePath(), toDelete.getFilename());

          log.debug("Trying to delete {} for task {} using glob {}...", toDelete.getFilename(), taskDefinition.getTaskId(), glob);

          try {
            List<Path> matches = findGlob(taskDefinition.getTaskDirectoryPath().toAbsolutePath(), taskDefinition.getTaskDirectoryPath().getFileSystem().getPathMatcher(glob));
            for (Path match : matches) {
              Files.delete(match);
              log.debug("Deleted {}", match);
            }
          } catch (IOException e) {
            log.error("Unable to list files while trying to delete for {}", toDelete);
          }
        });
  }

  @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "https://github.com/spotbugs/spotbugs/issues/259")
  private List<Path> findGlob(Path path, PathMatcher matcher) throws IOException {
    Deque<Path> stack = new ArrayDeque<>();
    List<Path> matched = new ArrayList<>();

    stack.push(path);

    while (!stack.isEmpty()) {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(stack.pop())) {
        for (Path entry : stream) {
          if (Files.isDirectory(entry)) {
            stack.push(entry);
          } else if (matcher.matches(entry)) {
            matched.add(entry);
          }
        }
      }
    }

    return matched;
  }
}
