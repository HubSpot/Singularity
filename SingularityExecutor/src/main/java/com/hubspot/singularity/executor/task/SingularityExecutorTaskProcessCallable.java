package com.hubspot.singularity.executor.task;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.TaskState;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Optional;
import com.hubspot.deploy.HealthcheckOptions;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.utils.ExecutorUtils;
import com.hubspot.singularity.runner.base.shared.SafeProcessManager;

public class SingularityExecutorTaskProcessCallable extends SafeProcessManager implements Callable<Integer> {

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
    Process process = startProcess(processBuilder);

    runHealthCheck();

    executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), Protos.TaskState.TASK_RUNNING, String.format("Task running process %s", getCurrentProcessToString()), task.getLog());

    return process.waitFor();
  }

  public SingularityExecutorTask getTask() {
    return task;
  }

  @Override
  public String toString() {
    return "SingularityExecutorTaskProcessCallable [task=" + task + "]";
  }

  private void runHealthCheck() {

    SingularityTaskId taskId = SingularityTaskId.valueOf(task.getTaskDefinition().getTaskId());

    Optional<HealthcheckOptions> maybeOptions = task.getTaskDefinition().getHealthCheckOptions();
    //                deployManager.getDeploy(taskId.getRequestId(), taskId.getDeployId()).get().getHealthcheck().get();

    Optional<String> expectedHealthCheckResultFilePath = task.getTaskDefinition().getHealthCheckResultFilePath();
    if (maybeOptions.isPresent() && expectedHealthCheckResultFilePath.isPresent()) {
      try {
        Integer healthcheckMaxRetries = maybeOptions.get().getMaxRetries().or(configuration.getHealthcheckMaxRetries());

        Retryer<String> retryer = RetryerBuilder.<String>newBuilder()
            .retryIfResult(path -> !new File(path).exists())
            .withWaitStrategy(WaitStrategies.fixedWait(1L, TimeUnit.SECONDS))
            .withStopStrategy(StopStrategies.stopAfterAttempt(healthcheckMaxRetries))
            .build();

        retryer.call(() -> expectedHealthCheckResultFilePath.get());

        executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), Protos.TaskState.TASK_RUNNING, String.format("Task running process %s", getCurrentProcessToString()), task.getLog());

      } catch (ExecutionException | RetryException e) {
        executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), TaskState.TASK_FAILED, String.format("Task timed out on health checks."), task.getLog());
      }
    }
  }
}
