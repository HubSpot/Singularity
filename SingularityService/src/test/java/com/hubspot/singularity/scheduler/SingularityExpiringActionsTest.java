package com.hubspot.singularity.scheduler;

import org.apache.mesos.v1.Protos.TaskState;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.api.SingularitySkipHealthchecksRequest;

public class SingularityExpiringActionsTest extends SingularitySchedulerTestBase {

  public SingularityExpiringActionsTest() {
    super(false);
  }

  @Test
  public void testExpiringPause() {
    initRequest();
    initFirstDeploy();

    SingularityTask taskOne = startTask(firstDeploy);

    requestResource.pause(requestId, Optional.of(new SingularityPauseRequest(Optional.absent(), Optional.of(1L), Optional.absent(), Optional.absent(), Optional.absent())), singularityUser);

    cleaner.drainCleanupQueue();

    Assert.assertEquals(1, taskManager.getKilledTaskIdRecords().size());

    statusUpdate(taskOne, TaskState.TASK_KILLED);

    resourceOffers();

    Assert.assertEquals(0, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(0, taskManager.getPendingTasks().size());
    Assert.assertEquals(RequestState.PAUSED, requestManager.getRequest(requestId).get().getState());
    Assert.assertEquals(requestId, requestManager.getPausedRequests(false).iterator().next().getRequest().getId());

    try {
      Thread.sleep(2);
    } catch (InterruptedException ie){
    }

    expiringUserActionPoller.runActionOnPoll();

    resourceOffers();

    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(0, taskManager.getPendingTasks().size());

    Assert.assertEquals(RequestState.ACTIVE, requestManager.getRequest(requestId).get().getState());
    Assert.assertEquals(requestId, requestManager.getActiveRequests(false).iterator().next().getRequest().getId());
  }

  @Test
  public void testExpiringBounceGoesAway() {
    initRequest();
    initFirstDeploy();

    startTask(firstDeploy, 1);

    requestResource.bounce(requestId,
      Optional.of(new SingularityBounceRequest(Optional.of(false), Optional.absent(), Optional.of(1L), Optional.absent(), Optional.of("msg"), Optional.absent())), singularityUser);

    cleaner.drainCleanupQueue();
    resourceOffers();
    runLaunchedTasks();
    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assert.assertTrue(taskManager.getCleanupTaskIds().isEmpty());
    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());

    Assert.assertTrue(!requestManager.getExpiringBounce(requestId).isPresent());
  }

  @Test
  public void testExpiringNonIncrementalBounce() {
    initWithTasks(3);

    requestResource.bounce(requestId,
      Optional.of(new SingularityBounceRequest(Optional.absent(), Optional.absent(), Optional.of(1L), Optional.of("aid"), Optional.absent(), Optional.absent())), singularityUser);

    Assert.assertTrue(!requestManager.getCleanupRequests().get(0).getMessage().isPresent());
    Assert.assertEquals("aid", requestManager.getCleanupRequests().get(0).getActionId().get());

    // creates cleanup tasks:
    cleaner.drainCleanupQueue();

    Assert.assertEquals(1, requestManager.getPendingRequests().size());
    Assert.assertEquals(0, requestManager.getCleanupRequests().size());
    Assert.assertEquals(3, taskManager.getCleanupTaskIds().size());

    // should have 1 pending task and 2 launched

    resourceOffersByNumTasks(2);

    Assert.assertEquals(1, taskManager.getPendingTasks().size());
    Assert.assertEquals(5, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(3, taskManager.getCleanupTaskIds().size());
    Assert.assertEquals(0, requestManager.getPendingRequests().size());
    Assert.assertEquals(0, requestManager.getCleanupRequests().size());

    try {
      Thread.sleep(1);
    } catch (InterruptedException ie) {
    }

    expiringUserActionPoller.runActionOnPoll();

    resourceOffers();
    cleaner.drainCleanupQueue();
    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assert.assertEquals(3, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(0, taskManager.getPendingTasks().size());
    Assert.assertEquals(0, taskManager.getCleanupTaskIds().size());
    Assert.assertEquals(0, requestManager.getPendingRequests().size());
    Assert.assertEquals(0, requestManager.getCleanupRequests().size());
  }

  @Test
  public void testExpiringIncrementalBounce() {
    initRequest();

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(3), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent()), singularityUser);

    initFirstDeploy();

    startTask(firstDeploy, 1);
    startTask(firstDeploy, 2);
    startTask(firstDeploy, 3);

    requestResource.bounce(requestId,
      Optional.of(new SingularityBounceRequest(Optional.of(true), Optional.absent(), Optional.of(1L), Optional.absent(), Optional.of("msg"), Optional.absent())), singularityUser);

    Assert.assertTrue(requestManager.cleanupRequestExists(requestId));
    Assert.assertEquals("msg", requestManager.getCleanupRequests().get(0).getMessage().get());
    Assert.assertTrue(requestManager.getCleanupRequests().get(0).getActionId().isPresent());

    String actionId = requestManager.getCleanupRequests().get(0).getActionId().get();

    cleaner.drainCleanupQueue();

    Assert.assertTrue(!requestManager.cleanupRequestExists(requestId));
    Assert.assertTrue(taskManager.getCleanupTaskIds().size() == 3);

    Assert.assertEquals("msg", taskManager.getCleanupTasks().get(0).getMessage().get());
    Assert.assertEquals(actionId, taskManager.getCleanupTasks().get(0).getActionId().get());

    startTask(firstDeploy, 4);
    //    launchTask(request, firstDeploy, 5, TaskState.TASK_STARTING);

    cleaner.drainCleanupQueue();

    Assert.assertEquals(1, taskManager.getKilledTaskIdRecords().size());
    Assert.assertEquals(4, taskManager.getActiveTaskIds().size());

    try {
      Thread.sleep(2);
    } catch (InterruptedException ie) {}

    expiringUserActionPoller.runActionOnPoll();

    cleaner.drainCleanupQueue();

    resourceOffers();

    killKilledTasks();

    Assert.assertTrue(!requestManager.getExpiringBounce(requestId).isPresent());
    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 3);
    Assert.assertTrue(!requestManager.cleanupRequestExists(requestId));
    Assert.assertTrue(taskManager.getCleanupTasks().isEmpty());
  }

  @Test
  public void testExpiringScale() {
    initRequest();
    initFirstDeploy();

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(5), Optional.of(1L), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent()), singularityUser);

    try {
      Thread.sleep(2);
    } catch (InterruptedException e) {

    }

    expiringUserActionPoller.runActionOnPoll();

    resourceOffers();
    resourceOffers();
    resourceOffers();
    resourceOffers();

    Assert.assertEquals(1, taskManager.getNumActiveTasks());
  }

  @Test
  public void testExpiringSkipHealthchecks() {
    initRequest();
    initHCDeploy();

    SingularityTask firstTask = startTask(firstDeploy);

    Assert.assertTrue(healthchecker.cancelHealthcheck(firstTask.getTaskId().getId()));

    requestResource.skipHealthchecks(requestId, new SingularitySkipHealthchecksRequest(Optional.of(true), Optional.of(1L), Optional.absent(), Optional.absent()), singularityUser);

    statusUpdate(firstTask, TaskState.TASK_FAILED);

    SingularityTask secondTask = startTask(firstDeploy);

    Assert.assertFalse(healthchecker.cancelHealthcheck(secondTask.getTaskId().getId()));

    statusUpdate(secondTask, TaskState.TASK_FAILED);

    expiringUserActionPoller.runActionOnPoll();

    SingularityTask thirdTask = startTask(firstDeploy);

    Assert.assertTrue(healthchecker.cancelHealthcheck(thirdTask.getTaskId().getId()));
  }

  @Test
  public void testExpiringScaleWithBounce() {
    initRequest();
    initFirstDeploy();

    requestResource.postRequest(request.toBuilder().setBounceAfterScale(Optional.of(true)).build(), singularityUser);

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(5), Optional.of(1L), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent()), singularityUser);

    Assert.assertEquals(1, requestManager.getCleanupRequests().size());
    cleaner.drainCleanupQueue();
    resourceOffers();
    resourceOffers();
    resourceOffers();
    resourceOffers();
    cleaner.drainCleanupQueue();
    killKilledTasks();
    Assert.assertEquals(5, taskManager.getNumActiveTasks());


    try {
      Thread.sleep(2);
    } catch (InterruptedException e) {

    }

    expiringUserActionPoller.runActionOnPoll();
    Assert.assertEquals(1, requestManager.getCleanupRequests().size());
    cleaner.drainCleanupQueue();
    Assert.assertEquals(4, taskManager.getKilledTaskIdRecords().size());

    launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    cleaner.drainCleanupQueue();
    Assert.assertEquals(5, taskManager.getKilledTaskIdRecords().size());
  }
}
