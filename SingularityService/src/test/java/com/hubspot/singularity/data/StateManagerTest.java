package com.hubspot.singularity.data;

import org.apache.mesos.v1.Protos.TaskState;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.scheduler.SingularitySchedulerTestBase;

public class StateManagerTest extends SingularitySchedulerTestBase{

  protected SingularityRequest request;

  @Inject
  private StateManager stateManager;

  public StateManagerTest() {
    super(false);
  }

  @Test
  public void itDoesntCountCleaningTasks() {
    initRequest();
    initFirstDeploy();

    SingularityRequest request = requestResource.getRequest(requestId).getRequest();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(3)));
    resourceOffers();

    Assert.assertEquals(3, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(0, stateManager.getState(true, false).getOverProvisionedRequests());
    Assert.assertEquals(0, stateManager.getState(true, false).getUnderProvisionedRequests());


    SingularityTask task = taskManager.getActiveTasks().get(0);
    statusUpdate(task, TaskState.TASK_KILLED);
    taskManager.createTaskCleanup(new SingularityTaskCleanup(Optional.absent(), TaskCleanupType.BOUNCING, 1L, task.getTaskId(), Optional.absent(), Optional.absent(), Optional.absent()));
    Assert.assertEquals(2, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(0, stateManager.getState(true, false).getOverProvisionedRequests());
    Assert.assertEquals(1, stateManager.getState(true, false).getUnderProvisionedRequests());


    launchTask(request, firstDeploy, 4, TaskState.TASK_RUNNING);
    launchTask(request, firstDeploy, 5, TaskState.TASK_RUNNING);
    Assert.assertEquals(4, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(0, stateManager.getState(true, false).getUnderProvisionedRequests());
    Assert.assertEquals(1, stateManager.getState(true, false).getOverProvisionedRequests());
  }
}
