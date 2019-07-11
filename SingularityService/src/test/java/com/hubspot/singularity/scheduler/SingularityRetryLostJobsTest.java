package com.hubspot.singularity.scheduler;

import java.util.concurrent.ExecutionException;

import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskState;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.Protos.TaskStatus.Reason;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.inject.Inject;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.mesos.SingularityMesosStatusUpdateHandler;

public class SingularityRetryLostJobsTest extends SingularitySchedulerTestBase {

  @Inject
  SingularityMesosStatusUpdateHandler updateHandler;

  public SingularityRetryLostJobsTest() {
    super(false);
  }

  @Test
  public void itRetriesLostShortRunningRequests() {
    runTest(RequestType.ON_DEMAND, Reason.REASON_AGENT_RESTARTED, true);
  }

  @Test
  public void itDoesNotRetryLostLongRunningRequests() {
    runTest(RequestType.SERVICE, Reason.REASON_AGENT_RESTARTED, false);
  }

  @Test
  public void itDoesNotRetryLostRequestsDueToNonAgentFailures() {
    runTest(RequestType.ON_DEMAND, Reason.REASON_CONTAINER_LIMITATION_DISK, false);
  }

  private void runTest(RequestType requestType, Reason reason, boolean shouldRetry) {
    initRequestWithType(requestType, false);
    initFirstDeploy();

    SingularityTask task = startTask(firstDeploy);
    Assertions.assertEquals(0, taskManager.getPendingTaskIds().size());
    Assertions.assertEquals(0, requestManager.getPendingRequests().size());

    try {
      updateHandler.processStatusUpdateAsync(TaskStatus.newBuilder()
          .setState(TaskState.TASK_LOST)
          .setReason(reason)
          .setTaskId(TaskID.newBuilder().setValue(task.getTaskId().getId()))
          .build()).get();
    } catch (InterruptedException | ExecutionException e) {
      Assertions.assertTrue(false);
    }

    if (shouldRetry) {
      Assertions.assertEquals(requestManager.getPendingRequests().size(), 1);
      Assertions.assertEquals(requestManager.getPendingRequests().get(0).getPendingType(), PendingType.RETRY);
    } else {
      if (requestManager.getPendingRequests().size() > 0) {
        Assertions.assertEquals(requestManager.getPendingRequests().get(0).getPendingType(), PendingType.TASK_DONE);
      }
    }
    scheduler.drainPendingQueue();
  }
}
