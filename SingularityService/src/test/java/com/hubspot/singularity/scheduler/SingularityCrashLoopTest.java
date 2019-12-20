package com.hubspot.singularity.scheduler;

import java.util.Optional;

import org.apache.mesos.v1.Protos.TaskState;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.Protos.TaskStatus.Reason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.TaskFailureType;
import com.hubspot.singularity.helpers.MesosProtosUtils;

public class SingularityCrashLoopTest extends SingularitySchedulerTestBase {

  public SingularityCrashLoopTest() {
    super(false);
  }

  @Test
  public void itDetectsFastFailureLoops() {

  }

  @Test
  public void itDetectsSlowConsistentFailureLoops() {

  }

  @Test
  public void itDetectsStartupFailureLoops() {

  }

  @Test
  public void itDetectsTooManyOoms() {

  }

  @Test
  public void itDetectsASingleInstanceFailureLoop() {

  }

  @Test
  public void itDetectsTooManyMultiInstanceFailures() {

  }

  private void createTaskFailure(int instanceNo, long timestamp, TaskFailureType failureType) {
    SingularityTask task = startTask(firstDeploy, instanceNo);
    switch (failureType) {
      case OOM:
        statusUpdateWithReason(timestamp, task, TaskState.TASK_FAILED, Reason.REASON_CONTAINER_LIMITATION_MEMORY);
        break;
      case STARTUP_FAILURE:
        taskManager.createTaskCleanup(new SingularityTaskCleanup(
            Optional.empty(),
            TaskCleanupType.UNHEALTHY_NEW_TASK,
            timestamp,
            task.getTaskId(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        ));
        statusUpdate(task, TaskState.TASK_KILLED, Optional.of(timestamp));
        break;
      case BAD_EXIT_CODE:
        statusUpdate(task, TaskState.TASK_FAILED, Optional.of(timestamp));
        break;
      case UNEXPECTED_EXIT:
        statusUpdate(task, TaskState.TASK_FINISHED, Optional.of(timestamp));
        break;
      case LOST_SLAVE:
        statusUpdateWithReason(timestamp, task, TaskState.TASK_LOST, Reason.REASON_AGENT_DISCONNECTED);
        break;
      case MESOS_ERROR:
        statusUpdateWithReason(timestamp, task, TaskState.TASK_LOST, Reason.REASON_INVALID_OFFERS);
        break;
      case OUT_OF_DISK_SPACE:
        statusUpdateWithReason(timestamp, task, TaskState.TASK_LOST, Reason.REASON_CONTAINER_LIMITATION_DISK);
        break;
      default:
        break;
    }
  }

  private void statusUpdateWithReason(long timestampMillis, SingularityTask task, TaskState state, Reason reason) {
    TaskStatus.Builder bldr = TaskStatus.newBuilder()
        .setTaskId(MesosProtosUtils.toTaskId(task.getMesosTask().getTaskId()))
        .setAgentId(MesosProtosUtils.toAgentId(task.getAgentId()))
        .setReason(reason)
        .setState(state);

    bldr.setTimestamp(timestampMillis / 1000);

    sms.statusUpdate(bldr.build()).join();
  }

}
