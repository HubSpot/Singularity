package com.hubspot.singularity.executor.utils;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.github.rholder.retry.AttemptTimeLimiters;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.RemoveContainerParam;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerInfo;

public class DockerUtils {
  private final SingularityExecutorConfiguration configuration;
  private final DockerClient dockerClient;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  @Inject
  public DockerUtils(SingularityExecutorConfiguration configuration, DockerClient dockerClient) {
    this.configuration = configuration;
    this.dockerClient = dockerClient;
  }

  public int getPid(final String containerName) throws DockerException {
    return inspectContainer(containerName).state().pid();
  }

  public boolean isContainerRunning(final String containerName) throws DockerException {
    return inspectContainer(containerName).state().running();
  }

  public ContainerInfo inspectContainer(final String containerName) throws DockerException {
    Callable<ContainerInfo> callable = new Callable<ContainerInfo>() {
      @Override public ContainerInfo call() throws Exception {
        return dockerClient.inspectContainer(containerName);
      }
    };

    try {
      return callWithRetriesAndTimeout(callable);
    } catch (Exception e) {
      throw new DockerException(e);
    }
  }

  public void pull(final String imageName) throws DockerException {
    Callable<Void> callable = new Callable<Void>() {
      @Override public Void call() throws Exception {
        dockerClient.pull(imageName);
        return null;
      }
    };

    try {
      callWithRetriesAndTimeout(callable, Optional.of(configuration.getMaxDockerPullAttempts()));
    } catch (Exception e) {
      throw new DockerException(e);
    }
  }

  public List<Container> listContainers() throws DockerException {
    Callable<List<Container>> callable = new Callable<List<Container>>() {
      @Override public List<Container> call() throws Exception {
        return dockerClient.listContainers();
      }
    };

    try {
      return callWithRetriesAndTimeout(callable);
    } catch (Exception e) {
      throw new DockerException(e);
    }
  }

  public void stopContainer(final String containerId, final int timeout) throws DockerException {
    Callable<Void> callable = new Callable<Void>() {
      @Override public Void call() throws Exception {
        dockerClient.stopContainer(containerId, timeout);
        return null;
      }
    };

    try {
      callWithRetriesAndTimeout(callable);
    } catch (Exception e) {
      throw new DockerException(e);
    }
  }

  public void removeContainer(final String containerId, final boolean removeRunning) throws DockerException {
    Callable<Void> callable = new Callable<Void>() {
      @Override public Void call() throws Exception {
        dockerClient.removeContainer(containerId, RemoveContainerParam.removeVolumes(removeRunning));
        return null;
      }
    };

    try {
      callWithRetriesAndTimeout(callable);
    } catch (Exception e) {
      throw new DockerException(e);
    }
  }

  private <T> T callWithRetriesAndTimeout(Callable<T> callable) throws Exception {
    return callWithRetriesAndTimeout(callable, Optional.<Integer>absent());
  }

  private <T> T callWithRetriesAndTimeout(Callable<T> callable, Optional<Integer> retryCount) throws Exception {
    RetryerBuilder<T> retryerBuilder = RetryerBuilder.<T>newBuilder()
      .withAttemptTimeLimiter(AttemptTimeLimiters.<T>fixedTimeLimit(configuration.getDockerClientTimeLimitSeconds(), TimeUnit.SECONDS, executor));
    if (retryCount.isPresent()) {
      retryerBuilder.withStopStrategy(StopStrategies.stopAfterAttempt(retryCount.get()));
    }
    return retryerBuilder.build().call(callable);
  }
}
