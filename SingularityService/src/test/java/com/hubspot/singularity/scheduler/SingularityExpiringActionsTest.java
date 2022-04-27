package com.hubspot.singularity.scheduler;

import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.api.SingularitySkipHealthchecksRequest;
import java.util.Optional;
import org.apache.mesos.v1.Protos.TaskState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SingularityExpiringActionsTest extends SingularitySchedulerTestBase {

  public SingularityExpiringActionsTest() {
    super(false);
  }

  @Test
  public void testExpiringPause() throws InterruptedException {
    initRequest();
    initFirstDeploy();

    SingularityTask taskOne = startTask(firstDeploy);

    requestResource.pause(
      requestId,
      Optional.of(
        new SingularityPauseRequest(
          Optional.empty(),
          Optional.of(1L),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        )
      ),
      singularityUser
    );

    cleaner.drainCleanupQueue();

    Assertions.assertEquals(1, taskManager.getKilledTaskIdRecords().size());

    statusUpdate(taskOne, TaskState.TASK_KILLED);

    resourceOffers();

    Assertions.assertEquals(0, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals(0, taskManager.getPendingTasks().size());
    Assertions.assertEquals(
      RequestState.PAUSED,
      requestManager.getRequest(requestId).get().getState()
    );
    Assertions.assertEquals(
      requestId,
      requestManager.getPausedRequests(false).iterator().next().getRequest().getId()
    );

    Thread.sleep(2);
    expiringUserActionPoller.runActionOnPoll();

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals(0, taskManager.getPendingTasks().size());

    Assertions.assertEquals(
      RequestState.ACTIVE,
      requestManager.getRequest(requestId).get().getState()
    );
    Assertions.assertEquals(
      requestId,
      requestManager.getActiveRequests(false).iterator().next().getRequest().getId()
    );
  }

  @Test
  public void testExpiringBounceGoesAway() {
    initRequest();
    initFirstDeploy();

    startTask(firstDeploy, 1);

    requestResource.bounce(
      requestId,
      Optional.of(
        new SingularityBounceRequest(
          Optional.of(false),
          Optional.empty(),
          Optional.of(1L),
          Optional.empty(),
          Optional.of("msg"),
          Optional.empty()
        )
      ),
      singularityUser
    );

    cleaner.drainCleanupQueue();
    scheduler.drainPendingQueue();
    resourceOffers();
    runLaunchedTasks();
    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assertions.assertTrue(taskManager.getCleanupTaskIds().isEmpty());
    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());

    Assertions.assertTrue(!requestManager.getExpiringBounce(requestId).isPresent());
  }

  @Test
  public void testExpiringNonIncrementalBounce() throws InterruptedException {
    initWithTasks(3);

    requestResource.bounce(
      requestId,
      Optional.of(
        new SingularityBounceRequest(
          Optional.empty(),
          Optional.empty(),
          Optional.of(1L),
          Optional.of("aid"),
          Optional.empty(),
          Optional.empty()
        )
      ),
      singularityUser
    );

    Assertions.assertTrue(
      !requestManager.getCleanupRequests().get(0).getMessage().isPresent()
    );
    Assertions.assertEquals(
      "aid",
      requestManager.getCleanupRequests().get(0).getActionId().get()
    );

    // creates cleanup tasks:
    cleaner.drainCleanupQueue();

    Assertions.assertEquals(1, requestManager.getPendingRequests().size());
    Assertions.assertEquals(0, requestManager.getCleanupRequests().size());
    Assertions.assertEquals(3, taskManager.getCleanupTaskIds().size());

    // should have 1 pending task and 2 launched
    scheduler.drainPendingQueue();
    resourceOffersByNumTasks(2);

    Assertions.assertEquals(1, taskManager.getPendingTasks().size());
    Assertions.assertEquals(5, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals(3, taskManager.getCleanupTaskIds().size());
    Assertions.assertEquals(0, requestManager.getPendingRequests().size());
    Assertions.assertEquals(0, requestManager.getCleanupRequests().size());

    Thread.sleep(1);

    expiringUserActionPoller.runActionOnPoll();

    scheduler.drainPendingQueue();
    resourceOffers();
    cleaner.drainCleanupQueue();
    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assertions.assertEquals(3, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals(0, taskManager.getPendingTasks().size());
    Assertions.assertEquals(0, taskManager.getCleanupTaskIds().size());
    Assertions.assertEquals(0, requestManager.getPendingRequests().size());
    Assertions.assertEquals(0, requestManager.getCleanupRequests().size());
  }

  @Test
  public void testExpiringIncrementalBounce() throws InterruptedException {
    initRequest();

    requestResource.scale(
      requestId,
      new SingularityScaleRequest(
        Optional.of(3),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    initFirstDeploy();

    startTask(firstDeploy, 1);
    startTask(firstDeploy, 2);
    startTask(firstDeploy, 3);

    requestResource.bounce(
      requestId,
      Optional.of(
        new SingularityBounceRequest(
          Optional.of(true),
          Optional.empty(),
          Optional.of(1L),
          Optional.empty(),
          Optional.of("msg"),
          Optional.empty()
        )
      ),
      singularityUser
    );

    Assertions.assertTrue(requestManager.cleanupRequestExists(requestId));
    Assertions.assertEquals(
      "msg",
      requestManager.getCleanupRequests().get(0).getMessage().get()
    );
    Assertions.assertTrue(
      requestManager.getCleanupRequests().get(0).getActionId().isPresent()
    );

    String actionId = requestManager.getCleanupRequests().get(0).getActionId().get();

    cleaner.drainCleanupQueue();

    Assertions.assertTrue(!requestManager.cleanupRequestExists(requestId));
    Assertions.assertTrue(taskManager.getCleanupTaskIds().size() == 3);

    Assertions.assertEquals(
      "msg",
      taskManager.getCleanupTasks().get(0).getMessage().get()
    );
    Assertions.assertEquals(
      actionId,
      taskManager.getCleanupTasks().get(0).getActionId().get()
    );

    startTask(firstDeploy, 4);
    //    launchTask(request, firstDeploy, 5, TaskState.TASK_STARTING);

    cleaner.drainCleanupQueue();

    Assertions.assertEquals(1, taskManager.getKilledTaskIdRecords().size());
    Assertions.assertEquals(4, taskManager.getActiveTaskIds().size());

    Thread.sleep(2);

    expiringUserActionPoller.runActionOnPoll();

    cleaner.drainCleanupQueue();

    resourceOffers();

    killKilledTasks();

    Assertions.assertTrue(!requestManager.getExpiringBounce(requestId).isPresent());
    Assertions.assertTrue(requestManager.getPendingRequests().isEmpty());
    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 3);
    Assertions.assertTrue(!requestManager.cleanupRequestExists(requestId));
    Assertions.assertTrue(taskManager.getCleanupTasks().isEmpty());
  }

  @Test
  public void testExpiringScale() throws InterruptedException {
    initRequest();
    initFirstDeploy();

    requestResource.scale(
      requestId,
      new SingularityScaleRequest(
        Optional.of(5),
        Optional.of(1L),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    Thread.sleep(2);

    expiringUserActionPoller.runActionOnPoll();

    scheduler.drainPendingQueue();
    resourceOffers();
    scheduler.drainPendingQueue();
    resourceOffers();
    scheduler.drainPendingQueue();
    resourceOffers();
    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(1, taskManager.getNumActiveTasks());
  }

  @Test
  public void testExpiringSkipHealthchecks() {
    initRequest();
    initHCDeploy();

    SingularityTask firstTask = startTask(firstDeploy);

    Assertions.assertTrue(healthchecker.cancelHealthcheck(firstTask.getTaskId().getId()));

    requestResource.skipHealthchecks(
      requestId,
      new SingularitySkipHealthchecksRequest(
        Optional.of(true),
        Optional.of(1L),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    statusUpdate(firstTask, TaskState.TASK_FAILED);

    SingularityTask secondTask = startTask(firstDeploy);

    Assertions.assertFalse(
      healthchecker.cancelHealthcheck(secondTask.getTaskId().getId())
    );

    statusUpdate(secondTask, TaskState.TASK_FAILED);

    expiringUserActionPoller.runActionOnPoll();

    SingularityTask thirdTask = startTask(firstDeploy);

    Assertions.assertTrue(healthchecker.cancelHealthcheck(thirdTask.getTaskId().getId()));
  }

  @Test
  public void testExpiringScaleWithBounce() throws InterruptedException {
    initRequest();
    initFirstDeploy();

    requestResource.postRequest(
      request.toBuilder().setBounceAfterScale(Optional.of(true)).build(),
      singularityUser
    );

    requestResource.scale(
      requestId,
      new SingularityScaleRequest(
        Optional.of(5),
        Optional.of(1L),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    Assertions.assertEquals(1, requestManager.getCleanupRequests().size());
    cleaner.drainCleanupQueue();
    scheduler.drainPendingQueue();
    resourceOffers();
    scheduler.drainPendingQueue();
    resourceOffers();
    scheduler.drainPendingQueue();
    resourceOffers();
    scheduler.drainPendingQueue();
    resourceOffers();
    cleaner.drainCleanupQueue();
    killKilledTasks();
    Assertions.assertEquals(5, taskManager.getNumActiveTasks());

    Thread.sleep(2);

    expiringUserActionPoller.runActionOnPoll();
    Assertions.assertEquals(1, requestManager.getCleanupRequests().size());
    cleaner.drainCleanupQueue();
    Assertions.assertEquals(4, taskManager.getKilledTaskIdRecords().size());

    launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    cleaner.drainCleanupQueue();
    Assertions.assertEquals(5, taskManager.getKilledTaskIdRecords().size());
  }
}
