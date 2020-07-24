package com.hubspot.singularity.mesos;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.scheduler.SingularitySchedulerTestBase;
import org.apache.mesos.v1.Protos.TaskState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SingularityStatusUpdateQueueTest extends SingularitySchedulerTestBase {
  @Inject
  StatusUpdateQueue statusUpdateQueue;

  public SingularityStatusUpdateQueueTest() {
    super(false);
  }

  @Test
  public void testUsesDiskOverflowToProcessStatusUpdates() throws Exception {
    try {
      configuration.getMesosConfiguration().setMaxStatusUpdateQueueSize(1);
      initRequest();
      initFirstDeploy();
      SingularityTask firstTask = launchTask(
        request,
        firstDeploy,
        1,
        TaskState.TASK_RUNNING
      );
      sms.pauseForDatastoreReconnect();
      statusUpdate(firstTask, TaskState.TASK_RUNNING);
      statusUpdate(firstTask, TaskState.TASK_FINISHED);
      Assertions.assertEquals(1, statusUpdateQueue.inMemorySize());
      Assertions.assertEquals(1, statusUpdateQueue.onDiskSize());
      sms.start();
      Assertions.assertEquals(0, statusUpdateQueue.size());
    } finally {
      configuration.getMesosConfiguration().setMaxStatusUpdateQueueSize(5000);
    }
  }
}
