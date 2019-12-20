package com.hubspot.singularity.scheduler;

import java.util.Optional;
import java.util.Set;

import org.apache.mesos.v1.Protos.TaskState;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.Protos.TaskStatus.Reason;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.hubspot.singularity.CrashLoopInfo;
import com.hubspot.singularity.CrashLoopType;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.TaskFailureType;
import com.hubspot.singularity.helpers.MesosProtosUtils;

public class SingularityCrashLoopTest extends SingularitySchedulerTestBase {

  @Inject
  SingularityCrashLoops crashLoops;

  public SingularityCrashLoopTest() {
    super(false);
  }

  @Test
  public void itDetectsFastFailureLoopsForNonLongRunning() {
    initRequestWithType(RequestType.ON_DEMAND, false);
    initFirstDeploy();
    long now = System.currentTimeMillis();
    createTaskFailure(1, now - 1000, TaskFailureType.BAD_EXIT_CODE);
    createTaskFailure(1, now - 10000, TaskFailureType.BAD_EXIT_CODE);
    createTaskFailure(1, now - 20000, TaskFailureType.BAD_EXIT_CODE);
    createTaskFailure(1, now - 30000, TaskFailureType.BAD_EXIT_CODE);
    createTaskFailure(1, now - 45000, TaskFailureType.BAD_EXIT_CODE);
    SingularityDeployStatistics deployStatistics = deployManager.getDeployStatistics(requestId, firstDeployId).get();
    Set<CrashLoopInfo> active = crashLoops.getActiveCrashLoops(deployStatistics);
    Assertions.assertEquals(1, active.size());
    Assertions.assertEquals(CrashLoopType.FAST_FAILURE_LOOP, Iterables.getOnlyElement(active).getType());
  }

  @Test
  public void itDetectsSlowConsistentFailureLoops() {

  }

  @Test
  public void itDoesNotTriggerSlowFailureLoopForFailuresConfinedToASmallTimeRange() {
    initRequestWithType(RequestType.WORKER, false);
    initFirstDeploy();
    long now = System.currentTimeMillis();
    createTaskFailure(1, now - 1000 - 300000, TaskFailureType.BAD_EXIT_CODE);
    createTaskFailure(1, now - 10000 - 300000, TaskFailureType.BAD_EXIT_CODE);
    createTaskFailure(1, now - 20000 - 300000, TaskFailureType.BAD_EXIT_CODE);
    createTaskFailure(1, now - 30000 - 300000, TaskFailureType.BAD_EXIT_CODE);
    createTaskFailure(1, now - 45000 - 300000, TaskFailureType.BAD_EXIT_CODE);
    SingularityDeployStatistics deployStatistics = deployManager.getDeployStatistics(requestId, firstDeployId).get();
    Set<CrashLoopInfo> active = crashLoops.getActiveCrashLoops(deployStatistics);
    Assertions.assertEquals(0, active.size());
  }

  @Test
  public void itDetectsStartupFailureLoops() {
    initRequestWithType(RequestType.WORKER, false);
    initFirstDeploy();

    long now = System.currentTimeMillis();
    SingularityTask task = startTask(firstDeploy, 1);
    taskManager.createTaskCleanup(new SingularityTaskCleanup(
        Optional.empty(),
        TaskCleanupType.UNHEALTHY_NEW_TASK,
        now - 30000,
        task.getTaskId(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
    ));
    createTaskFailure(1, now - 10000, TaskFailureType.STARTUP_FAILURE);
    createTaskFailure(1, now - 15000, TaskFailureType.STARTUP_FAILURE);
    createTaskFailure(1, now - 20000, TaskFailureType.STARTUP_FAILURE);

    SingularityDeployStatistics deployStatistics = deployManager.getDeployStatistics(requestId, firstDeployId).get();
    Set<CrashLoopInfo> active = crashLoops.getActiveCrashLoops(deployStatistics);
    Assertions.assertEquals(1, active.size());
    Assertions.assertEquals(CrashLoopType.STARTUP_FAILURE_LOOP, Iterables.getOnlyElement(active).getType());
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
        statusUpdateWithReason(timestamp, task, TaskState.TASK_FAILED, Reason.REASON_CONTAINER_LIMITATION_MEMORY, Optional.empty());
        break;
      case STARTUP_FAILURE:
        statusUpdateWithReason(timestamp, task, TaskState.TASK_KILLED, Reason.REASON_CONTAINER_PREEMPTED, Optional.of("UNHEALTHY_NEW_TASK - Task is not healthy"));
        break;
      case BAD_EXIT_CODE:
        statusUpdate(task, TaskState.TASK_FAILED, Optional.of(timestamp));
        break;
      case UNEXPECTED_EXIT:
        statusUpdate(task, TaskState.TASK_FINISHED, Optional.of(timestamp));
        break;
      case LOST_SLAVE:
        statusUpdateWithReason(timestamp, task, TaskState.TASK_LOST, Reason.REASON_AGENT_DISCONNECTED, Optional.empty());
        break;
      case MESOS_ERROR:
        statusUpdateWithReason(timestamp, task, TaskState.TASK_LOST, Reason.REASON_INVALID_OFFERS, Optional.empty());
        break;
      case OUT_OF_DISK_SPACE:
        statusUpdateWithReason(timestamp, task, TaskState.TASK_LOST, Reason.REASON_CONTAINER_LIMITATION_DISK, Optional.empty());
        break;
      default:
        break;
    }
  }

  private void statusUpdateWithReason(long timestampMillis, SingularityTask task, TaskState state, Reason reason, Optional<String> message) {
    TaskStatus.Builder bldr = TaskStatus.newBuilder()
        .setTaskId(MesosProtosUtils.toTaskId(task.getMesosTask().getTaskId()))
        .setAgentId(MesosProtosUtils.toAgentId(task.getAgentId()))
        .setReason(reason)
        .setState(state);
    message.ifPresent(bldr::setMessage);

    bldr.setTimestamp(timestampMillis / 1000);

    sms.statusUpdate(bldr.build()).join();
  }

}
