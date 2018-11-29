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

  private void runHealthcheck() {
    task.getLog().info("Running health check for {}", task.getTaskDefinition());
    Optional<HealthcheckOptions> maybeOptions = task.getTaskDefinition().getHealthcheckOptions();
    task.getLog().info("HC options: {}", maybeOptions);

    Optional<String> expectedHealthcheckResultFilePath = task.getTaskDefinition().getHealthcheckResultFilePath();
    task.getLog().info("Expected result file path: {}", expectedHealthcheckResultFilePath);

    if (expectedHealthcheckResultFilePath.isPresent()) {
      task.getLog().info("Expected result file abs path: {}", new File(expectedHealthcheckResultFilePath.get()).getAbsolutePath());
    }
    task.getLog().info("Curdir: {}", new File(".").getAbsolutePath());
    task.getLog().info("Files: {}", new File(".").listFiles());
    task.getLog().info("All Files: {}", new File(".").list());


    if (maybeOptions.isPresent() && expectedHealthcheckResultFilePath.isPresent()) {
      try {
        Integer healthcheckMaxRetries = maybeOptions.get().getMaxRetries().or(configuration.getDefaultHealthcheckMaxRetries());

        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
            .retryIfResult(bool -> !bool)
            .withWaitStrategy(WaitStrategies.fixedWait(1L, TimeUnit.SECONDS))
            .withStopStrategy(StopStrategies.stopAfterAttempt(healthcheckMaxRetries))
            .build();

        retryer.call(() -> {
          task.getLog().info("files: {}", new File(task.getTaskDefinition().getTaskAppDirectory()).listFiles());
          return Paths.get(task.getTaskDefinition().getTaskAppDirectory(), expectedHealthcheckResultFilePath.get()).toFile().exists();
        });

        executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), Protos.TaskState.TASK_RUNNING, String.format("Task running process %s", getCurrentProcessToString()), task.getLog());

      } catch (ExecutionException | RetryException e) {
        executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo().getTaskId(), TaskState.TASK_FAILED, String.format("Task timed out on health checks."), task.getLog());
      }
    }
  }
}
