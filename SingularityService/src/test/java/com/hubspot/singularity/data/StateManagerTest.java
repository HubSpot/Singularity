package com.hubspot.singularity.data;

import java.util.concurrent.TimeUnit;

import org.apache.mesos.v1.Protos.TaskState;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRunNowRequestBuilder;
import com.hubspot.singularity.SingularityState;
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

    SingularityRequest request = requestResource.getRequest(requestId, singularityUser).getRequest();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(3)));
    resourceOffers();

    Assert.assertEquals(3, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(0, stateManager.getState(true, false).getOverProvisionedRequests());
    Assert.assertEquals(0, stateManager.getState(true, false).getUnderProvisionedRequests());


    SingularityTask task = taskManager.getActiveTasks().get(0);
    statusUpdate(task, TaskState.TASK_KILLED);
    scheduler.drainPendingQueue();

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

  @Test
  public void itDoesntFlagPendingRequestsForUnderOrOverProvisioning() {
    initRequest();
    initFirstDeploy();

    SingularityRequest request = requestResource.getRequest(requestId, singularityUser).getRequest();

    requestManager.activate(request.toBuilder().setInstances(Optional.of(0)).build(), RequestHistoryType.UPDATED, System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent());
    requestManager.addToPendingQueue(new SingularityPendingRequest(request.getId(), firstDeployId, System.currentTimeMillis(), Optional.<String> absent(), PendingType.ONEOFF, Optional.<Boolean> absent(), Optional.<String> absent()));

    Assert.assertEquals(0, taskManager.getActiveTaskIds().size());

    SingularityState state = stateManager.getState(true, false);
    Assert.assertEquals(0, state.getOverProvisionedRequests());
    Assert.assertEquals(0, state.getUnderProvisionedRequests());
  }

  @Test
  public void itDoesntCountInstancesOverLimitInOnDemandLateTasks() {
    initOnDemandRequest();
    SingularityRequest request = requestResource.getRequest(requestId, singularityUser).getRequest();
    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(2)).build(), singularityUser);
    initFirstDeploy();

    configuration.setDeltaAfterWhichTasksAreLateMillis(0);

    requestResource.scheduleImmediately(
        singularityUser,
        request.getId(),
        new SingularityRunNowRequestBuilder()
            .setRunAt(System.currentTimeMillis())
            .build()
    );
    scheduler.drainPendingQueue();

    SingularityState state = stateManager.getState(true, false);
    System.out.println(state);

    Assert.assertEquals(0, state.getActiveTasks());
    Assert.assertEquals(1, state.getScheduledTasks());

    launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);
    state = stateManager.getState(true, false);
    System.out.println(state);

    Assert.assertEquals(1, state.getActiveTasks());
    Assert.assertEquals(1, state.getScheduledTasks());
    Assert.assertEquals(1, state.getOnDemandLateTasks());
    Assert.assertEquals(0, state.getLateTasks());

    launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);
    state = stateManager.getState(true, false);
    System.out.println(state);
    Assert.assertEquals(2, state.getActiveTasks());
    Assert.assertEquals(1, state.getScheduledTasks());
    Assert.assertEquals(0, state.getOnDemandLateTasks());
    Assert.assertEquals(0, state.getLateTasks());

    configuration.setDeltaAfterWhichTasksAreLateMillis(TimeUnit.SECONDS.toMillis(30));
  }
}
