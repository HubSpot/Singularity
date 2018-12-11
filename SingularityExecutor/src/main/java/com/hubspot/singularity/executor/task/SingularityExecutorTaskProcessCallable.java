package com.hubspot.singularity.executor.task;

import java.io.File;
import java.nio.file.Paths;
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
import com.google.common.base.Optional;
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

    runHealthcheck();

    return process.waitFor();
  }

  public SingularityExecutorTask getTask() {
    return task;
  }

  @Override
  public String toString() {
    return "SingularityExecutorTaskProcessCallable [task=" + task + "]";
  }

  private void runHealthcheck() {
    Optional<HealthcheckOptions> maybeOptions = task.getTaskDefinition().getHealthcheckOptions();
    Optional<String> expectedHealthcheckResultFilePath = task.getTaskDefinition().getHealthcheckResultFilePath();

    if (maybeOptions.isPresent() && expectedHealthcheckResultFilePath.isPresent()) {
      LOG.debug("Checking for healthcheck file {}", expectedHealthcheckResultFilePath.get());
      String taskAppDirectory = task.getTaskDefinition().getTaskAppDirectory();
      File fullHealthcheckPath = Paths.get(taskAppDirectory, expectedHealthcheckResultFilePath.get()).toFile();

      try {
        Integer healthcheckMaxRetries = maybeOptions.get().getMaxRetries().or(configuration.getDefaultHealthcheckMaxRetries());
        Integer retryInterval = maybeOptions.get().getIntervalSeconds().or(1);

        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
            .retryIfResult(bool -> !bool)
            .withWaitStrategy(WaitStrategies.fixedWait(retryInterval, TimeUnit.SECONDS))
            .withStopStrategy(StopStrategies.stopAfterAttempt(healthcheckMaxRetries))
            .build();

        retryer.call(() -> fullHealthcheckPath.exists());
        executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), Protos.TaskState.TASK_RUNNING, String.format("Task running process %s (health check file found successfully).", getCurrentProcessToString()), task.getLog());
      } catch (ExecutionException | RetryException e) {
        executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), TaskState.TASK_FAILED, String.format("Task timed out on health checks (health check file not found)."), task.getLog());
      }
    } else {
      executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), Protos.TaskState.TASK_RUNNING, String.format("Task running process %s", getCurrentProcessToString()), task.getLog());
    }
  }
}
