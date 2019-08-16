package com.hubspot.singularity.data;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.v1.Protos.TaskState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

    Assertions.assertEquals(3, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals(0, stateManager.getState(true, false).getOverProvisionedRequests());
    Assertions.assertEquals(0, stateManager.getState(true, false).getUnderProvisionedRequests());


    SingularityTask task = taskManager.getActiveTasks().get(0);
    statusUpdate(task, TaskState.TASK_KILLED);
    scheduler.drainPendingQueue();

    taskManager.createTaskCleanup(new SingularityTaskCleanup(Optional.empty(), TaskCleanupType.BOUNCING, 1L, task.getTaskId(), Optional.empty(), Optional.empty(), Optional.empty()));
    Assertions.assertEquals(2, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals(0, stateManager.getState(true, false).getOverProvisionedRequests());
    Assertions.assertEquals(1, stateManager.getState(true, false).getUnderProvisionedRequests());


    launchTask(request, firstDeploy, 4, TaskState.TASK_RUNNING);
    launchTask(request, firstDeploy, 5, TaskState.TASK_RUNNING);
    Assertions.assertEquals(4, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals(0, stateManager.getState(true, false).getUnderProvisionedRequests());
    Assertions.assertEquals(1, stateManager.getState(true, false).getOverProvisionedRequests());
  }

  @Test
  public void itDoesntFlagPendingRequestsForUnderOrOverProvisioning() {
    initRequest();
    initFirstDeploy();

    SingularityRequest request = requestResource.getRequest(requestId, singularityUser).getRequest();

    requestManager.activate(request.toBuilder().setInstances(Optional.of(0)).build(), RequestHistoryType.UPDATED, System.currentTimeMillis(), Optional.<String>empty(), Optional.<String>empty());
    requestManager.addToPendingQueue(new SingularityPendingRequest(request.getId(), firstDeployId, System.currentTimeMillis(), Optional.<String>empty(), PendingType.ONEOFF, Optional.<Boolean>empty(), Optional.<String>empty()));

    Assertions.assertEquals(0, taskManager.getActiveTaskIds().size());

    SingularityState state = stateManager.getState(true, false);
    Assertions.assertEquals(0, state.getOverProvisionedRequests());
    Assertions.assertEquals(0, state.getUnderProvisionedRequests());
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

    Assertions.assertEquals(0, state.getActiveTasks());
    Assertions.assertEquals(1, state.getScheduledTasks());

    launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);
    state = stateManager.getState(true, false);
    System.out.println(state);

    Assertions.assertEquals(1, state.getActiveTasks());
    Assertions.assertEquals(1, state.getScheduledTasks());
    Assertions.assertEquals(1, state.getOnDemandLateTasks());
    Assertions.assertEquals(0, state.getLateTasks());

    launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);
    state = stateManager.getState(true, false);
    System.out.println(state);
    Assertions.assertEquals(2, state.getActiveTasks());
    Assertions.assertEquals(1, state.getScheduledTasks());
    Assertions.assertEquals(0, state.getOnDemandLateTasks());
    Assertions.assertEquals(0, state.getLateTasks());

    configuration.setDeltaAfterWhichTasksAreLateMillis(TimeUnit.SECONDS.toMillis(30));
  }
}
