package com.hubspot.singularity.executor.task;

import java.util.concurrent.Callable;

import org.apache.mesos.Protos;

import com.hubspot.singularity.executor.utils.ExecutorUtils;
import com.hubspot.singularity.runner.base.shared.SafeProcessManager;

public class SingularityExecutorTaskProcessCallable extends SafeProcessManager implements Callable<Integer> {

  private final ProcessBuilder processBuilder;
  private final ExecutorUtils executorUtils;
  private final SingularityExecutorTask task;

  public SingularityExecutorTaskProcessCallable(SingularityExecutorTask task, ProcessBuilder processBuilder, ExecutorUtils executorUtils) {
    super(task.getLog());

    this.executorUtils = executorUtils;
    this.processBuilder = processBuilder;
    this.task = task;
  }

  @Override
  public Integer call() throws Exception {
    Process process = startProcess(processBuilder);

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

}
