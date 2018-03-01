package com.hubspot.singularity.data;

import java.util.Optional;

import org.apache.mesos.v1.Protos.TaskState;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Inject;
import com.hubspot.singularity.api.request.SingularityRequest;
import com.hubspot.singularity.api.task.SingularityTask;
import com.hubspot.singularity.api.task.SingularityTaskCleanup;
import com.hubspot.singularity.api.task.TaskCleanupType;
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

    SingularityRequest request = requestResource.getRequest(requestId, singularityUser).getRequest();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(3)));
    resourceOffers();

    Assert.assertEquals(3, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(0, stateManager.getState(true, false).getOverProvisionedRequests());
    Assert.assertEquals(0, stateManager.getState(true, false).getUnderProvisionedRequests());


    SingularityTask task = taskManager.getActiveTasks().get(0);
    statusUpdate(task, TaskState.TASK_KILLED);
    scheduler.drainPendingQueue();

    taskManager.createTaskCleanup(new SingularityTaskCleanup(Optional.empty(), TaskCleanupType.BOUNCING, 1L, task.getTaskId(), Optional.empty(), Optional.empty(), Optional.empty()));
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
