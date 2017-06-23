package com.hubspot.singularity.executor;

import org.apache.mesos.Executor;
import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.TaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.executor.SingularityExecutorMonitor.KillState;
import com.hubspot.singularity.executor.SingularityExecutorMonitor.SubmitState;
import com.hubspot.singularity.executor.config.SingularityExecutorTaskBuilder;
import com.hubspot.singularity.executor.task.SingularityExecutorTask;
import com.hubspot.singularity.executor.utils.ExecutorUtils;

public class SingularityExecutor implements Executor {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityExecutor.class);

  private final SingularityExecutorTaskBuilder taskBuilder;
  private final SingularityExecutorMesosFrameworkMessageHandler messageHandler;
  private final SingularityExecutorMonitor monitor;
  private final ExecutorUtils executorUtils;

  @Inject
  public SingularityExecutor(SingularityExecutorMonitor monitor, ExecutorUtils executorUtils, SingularityExecutorTaskBuilder taskBuilder,
      SingularityExecutorMesosFrameworkMessageHandler messageHandler) {
    this.taskBuilder = taskBuilder;
    this.monitor = monitor;
    this.executorUtils = executorUtils;
    this.messageHandler = messageHandler;
  }

  /**
   * Invoked once the executor driver has been able to successfully
   * connect with Mesos. In particular, a scheduler can pass some
   * data to it's executors through the FrameworkInfo.ExecutorInfo's
   * data field.
   */
  @Override
  public void registered(ExecutorDriver executorDriver, Protos.ExecutorInfo executorInfo, Protos.FrameworkInfo frameworkInfo, Protos.SlaveInfo slaveInfo) {
    LOG.debug("Registered {} with Mesos slave {} for framework {}", executorInfo.getExecutorId().getValue(), slaveInfo.getId().getValue(), frameworkInfo.getId().getValue());
    LOG.trace("Registered {} with Mesos slave {} for framework {}", MesosUtils.formatForLogging(executorInfo), MesosUtils.formatForLogging(slaveInfo), MesosUtils.formatForLogging(frameworkInfo));
  }

  /**
   * Invoked when the executor re-registers with a restarted slave.
   */
  @Override
  public void reregistered(ExecutorDriver executorDriver, Protos.SlaveInfo slaveInfo) {
    LOG.debug("Re-registered with Mesos slave {}", slaveInfo.getId().getValue());
    LOG.info("Re-registered with Mesos slave {}", MesosUtils.formatForLogging(slaveInfo));
  }

  /**
   * Invoked when the executor becomes "disconnected" from the slave
   * (e.g., the slave is being restarted due to an upgrade).
   */
  @Override
  public void disconnected(ExecutorDriver executorDriver) {
    LOG.warn("Disconnected from Mesos slave"); // TODO do we need to handle anything here?
  }

  /**
   * Invoked when a task has been launched on this executor (initiated
   * via Scheduler::launchTasks). Note that this task can be realized
   * with a thread, a process, or some simple computation, however, no
   * other callbacks will be invoked on this executor until this
   * callback has returned.
   */
  @Override
  public void launchTask(final ExecutorDriver executorDriver, final Protos.TaskInfo taskInfo) {
    final String taskId = taskInfo.getTaskId().getValue();

    LOG.info("Asked to launch task {}", taskId);

    try {
      final ch.qos.logback.classic.Logger taskLog = taskBuilder.buildTaskLogger(taskId, taskInfo.getExecutor().getExecutorId().getValue());
      final SingularityExecutorTask task = taskBuilder.buildTask(taskId, executorDriver, taskInfo, taskLog);

      SubmitState submitState = monitor.submit(task);

      switch (submitState) {
        case REJECTED:
          LOG.warn("Can't launch task {}, it was rejected (probably due to shutdown)", taskInfo);
          break;
        case TASK_ALREADY_EXISTED:
          LOG.error("Can't launch task {}, already had a task with that ID", taskInfo);
          break;
        case SUBMITTED:
          task.getLog().info("Launched task {} with data {}", taskId, task.getExecutorData());
          break;
      }

    } catch (Throwable t) {
      LOG.error("Unexpected exception starting task {}", taskId, t);

      executorUtils.sendStatusUpdate(executorDriver, taskInfo.getTaskId(), TaskState.TASK_LOST, String.format("Unexpected exception while launching task %s - %s", taskId, t.getMessage()), LOG);
    }
  }

  /**
   * Invoked when a task running within this executor has been killed
   * (via SchedulerDriver::killTask). Note that no status update will
   * be sent on behalf of the executor, the executor is responsible
   * for creating a new TaskStatus (i.e., with TASK_KILLED) and
   * invoking ExecutorDriver::sendStatusUpdate.
   */
  @Override
  public void killTask(ExecutorDriver executorDriver, Protos.TaskID taskID) {
    final String taskId = taskID.getValue();

    LOG.info("Asked to kill task {}", taskId);

    KillState killState = monitor.requestKill(taskId);

    switch (killState) {
      case DIDNT_EXIST:
      case INCONSISTENT_STATE:
        LOG.warn("Couldn't kill task {} due to killState {}", taskId, killState);
        break;
      case DESTROYING_PROCESS:
      case INTERRUPTING_PRE_PROCESS:
      case KILLING_PROCESS:
        LOG.info("Requested kill of task {} with killState {}", taskId, killState);
        break;
    }
  }

  @Override
  public void frameworkMessage(ExecutorDriver executorDriver, byte[] bytes) {
    try {
      messageHandler.handleMessage(bytes);
    } catch (Throwable t) {
      LOG.warn("Unexpected exception while handling framework message", t);
    }
  }

  /**
   * Invoked when the executor should terminate all of it's currently
   * running tasks. Note that after a Mesos has determined that an
   * executor has terminated any tasks that the executor did not send
   * terminal status updates for (e.g., TASK_KILLED, TASK_FINISHED,
   * TASK_FAILED, etc) a TASK_LOST status update will be created.
   */
  @Override
  public void shutdown(ExecutorDriver executorDriver) {
    LOG.info("Asked to shutdown executor...");

    monitor.shutdown(executorDriver);
  }

  /**
   * Invoked when a fatal error has occured with the executor and/or
   * executor driver. The driver will be aborted BEFORE invoking this
   * callback.
   */
  @Override
  public void error(ExecutorDriver executorDriver, String s) {
    LOG.error("Executor error: {}", s);
  }
}
