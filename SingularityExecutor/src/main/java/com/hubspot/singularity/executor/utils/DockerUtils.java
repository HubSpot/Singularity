package com.hubspot.singularity.executor.utils;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.DockerRequestException;
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
      return callWithTimeout(callable);
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
      callWithTimeout(callable);
    } catch (Exception e) {
      if (e.getCause() != null && e.getCause() instanceof DockerRequestException) {
        try {
          callWithTimeout(callable);
        } catch (Exception de) {
          throw new DockerException(de);
        }
      }
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
      return callWithTimeout(callable);
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
      callWithTimeout(callable);
    } catch (Exception e) {
      throw new DockerException(e);
    }
  }

  public void removeContainer(final String containerId, final boolean removeRunning) throws DockerException {
    Callable<Void> callable = new Callable<Void>() {
      @Override public Void call() throws Exception {
        dockerClient.removeContainer(containerId, removeRunning);
        return null;
      }
    };

    try {
      callWithTimeout(callable);
    } catch (Exception e) {
      throw new DockerException(e);
    }
  }

  private <T> T callWithTimeout(Callable<T> callable) throws Exception {
    FutureTask<T> task = new FutureTask<T>(callable);
    executor.execute(task);
    return task.get(configuration.getDockerClientTimeLimitSeconds(), TimeUnit.SECONDS);
  }
}
