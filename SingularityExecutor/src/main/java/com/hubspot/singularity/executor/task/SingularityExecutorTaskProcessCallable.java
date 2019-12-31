package com.hubspot.singularity.executor.task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.TaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.hubspot.deploy.HealthCheckStatus;
import com.hubspot.deploy.HealthcheckConfig;
import com.hubspot.deploy.HealthcheckOptions;
import com.hubspot.deploy.HealthcheckResult;
import com.hubspot.deploy.HealthchecksV2;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.utils.ExecutorUtils;
import com.hubspot.singularity.runner.base.shared.SafeProcessManager;

public class SingularityExecutorTaskProcessCallable extends SafeProcessManager implements Callable<Integer> {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityExecutorTaskProcessCallable.class);

  private static final String HEALTHCHECK_CONFIG_FILE_PATH = "healthcheck-config.json";

  private final ProcessBuilder processBuilder;
  private final ExecutorUtils executorUtils;
  private final SingularityExecutorConfiguration configuration;
  private final SingularityExecutorTask task;
  private final ObjectMapper objectMapper;

  public SingularityExecutorTaskProcessCallable(SingularityExecutorConfiguration configuration,
                                                SingularityExecutorTask task,
                                                ProcessBuilder processBuilder,
                                                ExecutorUtils executorUtils,
                                                ObjectMapper objectMapper) {
    super(task.getLog());

    this.executorUtils = executorUtils;
    this.processBuilder = processBuilder;
    this.configuration = configuration;
    this.task = task;
    this.objectMapper = objectMapper;
  }

  @Override
  public Integer call() throws Exception {
    LOG.info("Process being started");
    Process process = startProcess(processBuilder);

    if (!runHealthcheck(process)) {
      task.getLog().info("Killing task {} that did not pass health checks", task.getTaskId());
      super.signalKillToProcessIfActive();
    }

    return process.waitFor();
  }

  public SingularityExecutorTask getTask() {
    return task;
  }

  @Override
  public String toString() {
    return "SingularityExecutorTaskProcessCallable [task=" + task + "]";
  }

  private boolean runHealthcheck(Process process) {
    Optional<HealthcheckOptions> maybeOptions = task.getTaskDefinition().getHealthcheckOptions();
    if (maybeOptions.isPresent() && maybeOptions.get().getExpectHealthcheckConfigFile()) {
      return runHealthcheckV2(process, maybeOptions.get());
    }
    return runHealthcheckV1(process, maybeOptions);
  }

  private boolean runHealthcheckV1(Process process, Optional<HealthcheckOptions> maybeOptions) {
    Optional<String> expectedHealthcheckResultFilePath = task.getTaskDefinition().getHealthcheckResultFilePath();

    if (!maybeOptions.isPresent() || !expectedHealthcheckResultFilePath.isPresent()) {
      executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), Protos.TaskState.TASK_RUNNING, String.format("Task running process %s", getCurrentProcessToString()), task.getLog());
      return true;
    }

    LOG.debug("Checking for healthcheck file {}", expectedHealthcheckResultFilePath.get());
    String taskAppDirectory = task.getTaskDefinition().getTaskAppDirectory();
    File fullHealthcheckPath = Paths.get(taskAppDirectory, expectedHealthcheckResultFilePath.get()).toFile();

    Integer healthcheckMaxRetries = maybeOptions.get().getMaxRetries().orElse(configuration.getDefaultHealthcheckMaxRetries());
    Integer retryInterval = maybeOptions.get().getIntervalSeconds().orElse(configuration.getDefaultHealthcheckInternvalSeconds());
    long maxDelay = configuration.getDefaultHealthcheckBaseTimeoutSeconds() + (retryInterval * healthcheckMaxRetries);

    try {
      Retryer<HealthCheckStatus> retryer = RetryerBuilder.<HealthCheckStatus>newBuilder()
          .retryIfResult(result -> result == HealthCheckStatus.WAITING)
          .withWaitStrategy(WaitStrategies.fixedWait(retryInterval, TimeUnit.SECONDS))
          .withStopStrategy(StopStrategies.stopAfterDelay(maxDelay, TimeUnit.SECONDS))
          .build();

      HealthCheckStatus result = retryer.call(() -> {
        if (fullHealthcheckPath.exists()) {
          return HealthCheckStatus.PASSED;
        } else if (process.isAlive()) {
          return HealthCheckStatus.WAITING;
        }
        return HealthCheckStatus.FAILED;
      });

      if (result == HealthCheckStatus.PASSED) {
        executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), Protos.TaskState.TASK_RUNNING, String.format("Task running process %s (health check file found successfully).", getCurrentProcessToString()), task.getLog());
        return true;
      } else {
        executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), TaskState.TASK_FAILED, String.format("Task timed out on health checks after %d seconds (health check file not found).", maxDelay), task.getLog());
        return false;
      }
    } catch (ExecutionException | RetryException e) {
      executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), TaskState.TASK_FAILED, String.format("Task timed out on health checks after %d seconds (health check file not found).", maxDelay), task.getLog());
      return false;
    }
  }

  private boolean runHealthcheckV2(Process process, HealthcheckOptions maybeOptions) {
    String taskAppDirectory = task.getTaskDefinition().getTaskAppDirectory();
    File configPath = Paths.get(taskAppDirectory, HEALTHCHECK_CONFIG_FILE_PATH).toFile();
    LOG.debug("Getting healthcheck config file path at {}", configPath);

    HealthchecksV2 healthchecks;
    try {
      Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
          .retryIfResult(x -> !x)
          .withWaitStrategy(WaitStrategies.fixedWait(500, TimeUnit.MILLISECONDS))
          .withStopStrategy(StopStrategies.stopAfterAttempt(20))
          .build();
      retryer.call(() -> configPath.exists());
      healthchecks = objectMapper.readValue(configPath, HealthchecksV2.class);
    } catch (IOException e) {
      executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), TaskState.TASK_FAILED, String.format("Failed to run healthchecks. Failed to read healthcheck config file. %s", e.getCause()), task.getLog());
      return false;
    } catch (ExecutionException | RetryException e) {
      executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), TaskState.TASK_FAILED, String.format("Failed to run healthchecks. Failed to find healthcheck config file. %s", e.getCause()), task.getLog());
      return false;
    }

    // Start all the futures, ordered by max possibe lifespan
    List<CompletableFuture<HealthcheckResult>> futures = healthchecks.getHealthchecks().stream()
        .filter(HealthcheckConfig::isEnabled)
        .sorted(Comparator.comparingInt(config -> config.getTimeout()))
        .map(healthcheckConfig -> startHealthcheck(process, healthcheckConfig))
        .collect(Collectors.toList());

    List<HealthcheckResult> results = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());

    for (HealthcheckResult result : results) {
      HealthCheckStatus status = result.getStatus();
      if (status != HealthCheckStatus.PASSED) {
        executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), TaskState.TASK_FAILED, String.format("Healthcheck failed: %s", result.getMessage()), task.getLog());
        return false;
      }
    }
    executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), Protos.TaskState.TASK_RUNNING, String.format("Task running process %s.", getCurrentProcessToString()), task.getLog());
    return true;
  }

  private CompletableFuture<HealthcheckResult> startHealthcheck(Process process, HealthcheckConfig healthcheckConfig) {
    Retryer<HealthcheckResult> retryer = buildHealthcheckRetryer(healthcheckConfig);
    return CompletableFuture.supplyAsync( () -> {
      try {
        return retryer.call(() -> getHealthcheckResult(process, healthcheckConfig));
      } catch (ExecutionException | RetryException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private Retryer<HealthcheckResult> buildHealthcheckRetryer(HealthcheckConfig healthcheckConfig) {
    return RetryerBuilder.<HealthcheckResult>newBuilder()
        .retryIfResult(result -> result.getStatus() == HealthCheckStatus.WAITING)
        .withWaitStrategy(WaitStrategies.fixedWait(healthcheckConfig.getIntervalSeconds(), TimeUnit.SECONDS))
        .withStopStrategy(StopStrategies.stopAfterAttempt(healthcheckConfig.getMaxRetries()))
        .build();
  }

  private HealthcheckResult getHealthcheckResult(Process process, HealthcheckConfig healthcheckConfig) {
    File resultPath = getFileInAppDir(healthcheckConfig.getResultFilePath());
    if (resultPath.exists()) {
      try {
        HealthcheckResult result = objectMapper.readValue(resultPath, HealthcheckResult.class);
        return result;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else if (process.isAlive()) {
      return HealthcheckResult.builder()
          .setName(healthcheckConfig.getName())
          .setStatus(HealthCheckStatus.WAITING)
          .setMessage("Process is still alive, health checks running...")
          .build();
    }
    return HealthcheckResult.builder()
        .setName(healthcheckConfig.getName())
        .setStatus(HealthCheckStatus.FAILED)
        .setMessage("Process is dead with no health check result file available.")
        .build();
  }

  private File getFileInAppDir(String filename) {
    String taskAppDirectory = task.getTaskDefinition().getTaskAppDirectory();
    return Paths.get(taskAppDirectory, HEALTHCHECK_CONFIG_FILE_PATH).toFile();
  }
}
