package com.hubspot.singularity.executor.utils;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.print.Doc;

import jdk.nashorn.internal.codegen.CompilerConstants.Call;

import org.apache.mesos.Protos.Parameter;
import org.slf4j.Logger;

import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskDefinition;
import com.spotify.docker.client.ContainerNotFoundException;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerInfo;

public class DockerUtils {
  private final SingularityExecutorTaskDefinition taskDefinition;
  private final Logger log;
  private final SingularityExecutorConfiguration configuration;
  private final DockerClient dockerClient;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  @Inject
  public DockerUtils(SingularityExecutorTaskDefinition taskDefinition, Logger log, SingularityExecutorConfiguration configuration, DockerClient dockerClient) {
    this.taskDefinition = taskDefinition;
    this.log = log;
    this.configuration = configuration;
    this.dockerClient = dockerClient;
  }


  public boolean cleanDocker() {
    Callable<Boolean> callable = new Callable<Boolean>() {
      @Override public Boolean call() throws Exception {
        String containerName = String.format("%s%s", configuration.getDockerPrefix(), taskDefinition.getTaskId());
        try {
          ContainerInfo containerInfo = dockerClient.inspectContainer(containerName);
          if (containerInfo.state().running()) {
            dockerClient.stopContainer(containerName, configuration.getDockerStopTimeout());
          }
          dockerClient.removeContainer(containerName);
          log.info("Removed container {}", containerName);
          return true;
        } catch (ContainerNotFoundException e) {
          log.info("Container {} was already removed", containerName);
          return true;
        } catch (UncheckedTimeoutException te) {
          log.error("Timed out trying to reach docker daemon after {} seconds", configuration.getDockerClientTimeLimitSeconds(), te);
        } catch (Exception e) {
          log.info("Could not ensure removal of docker container", e);
        }
        return false;
      }
    };

    try {
      return callWithTimeout(callable);
    } catch (Exception e) {
      log.error("Caught exception while cleaning docker containers", e);
      return false;
    }
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
      log.error("Caught exception while getting container status", e);
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
      log.error("Could not pull image due to error", e);
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
      log.error("Caught exception attempting to list containers", e);
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
      log.error("Caught exception attempting to stop container", e);
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
      log.error("Caught exception attempting to stop container", e);
      throw new DockerException(e);
    }
  }

  private <T> T callWithTimeout(Callable<T> callable) throws Exception {
    FutureTask<T> task = new FutureTask<T>(callable);
    executor.execute(task);
    return task.get(configuration.getDockerClientTimeLimitSeconds(), TimeUnit.SECONDS);
  }
}
