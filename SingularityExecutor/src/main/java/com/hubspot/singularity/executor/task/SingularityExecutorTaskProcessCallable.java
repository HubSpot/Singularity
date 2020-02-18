package com.hubspot.singularity.executor.task;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.TaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.hubspot.deploy.HealthcheckOptions;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.utils.ExecutorUtils;
import com.hubspot.singularity.runner.base.shared.SafeProcessManager;

public class SingularityExecutorTaskProcessCallable extends SafeProcessManager implements Callable<Integer> {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityExecutorTaskProcessCallable.class);

  private final ProcessBuilder processBuilder;
  private final ExecutorUtils executorUtils;
  private final SingularityExecutorConfiguration configuration;
  private final SingularityExecutorTask task;

  enum HealthCheckResult {
    PASSED,
    FAILED,
    WAITING;
  }

  public SingularityExecutorTaskProcessCallable(SingularityExecutorConfiguration configuration,
                                                SingularityExecutorTask task,
                                                ProcessBuilder processBuilder,
                                                ExecutorUtils executorUtils) {
    super(task.getLog());

    this.executorUtils = executorUtils;
    this.processBuilder = processBuilder;
    this.configuration = configuration;
    this.task = task;
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
      Retryer<HealthCheckResult> retryer = RetryerBuilder.<HealthCheckResult>newBuilder()
          .retryIfResult(result -> result == HealthCheckResult.WAITING)
          .withWaitStrategy(WaitStrategies.fixedWait(retryInterval, TimeUnit.SECONDS))
          .withStopStrategy(StopStrategies.stopAfterDelay(maxDelay, TimeUnit.SECONDS))
          .build();

      HealthCheckResult result = retryer.call(() -> {
        if (fullHealthcheckPath.exists()) {
          return HealthCheckResult.PASSED;
        } else if (process.isAlive()) {
          return HealthCheckResult.WAITING;
        }
        return HealthCheckResult.FAILED;
      });

      if (result == HealthCheckResult.PASSED) {
        executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), Protos.TaskState.TASK_RUNNING, String.format("Task running process %s (health check file found successfully).", getCurrentProcessToString()), task.getLog());
        return true;
      } else {
        if (!process.isAlive() && process.exitValue() == 0) {
          LOG.info("Task already exited with code 0, considering healthcheck a success and sending running/finished update");
          executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), Protos.TaskState.TASK_RUNNING, String.format("Task running process %s (health check file found successfully).", getCurrentProcessToString()), task.getLog());
          return true;
        } else {
          executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo()
              .getTaskId(), TaskState.TASK_FAILED, String.format("Task timed out on health checks after %d seconds (health check file not found).", maxDelay), task.getLog());
          return false;
        }
      }
    } catch (ExecutionException | RetryException e) {
      executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), TaskState.TASK_FAILED, String.format("Task timed out on health checks after %d seconds (health check file not found).", maxDelay), task.getLog());
      return false;
    }
  }
}
