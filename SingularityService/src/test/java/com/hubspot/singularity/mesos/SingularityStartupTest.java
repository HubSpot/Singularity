package com.hubspot.singularity.mesos;

import com.google.inject.Inject;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import com.hubspot.singularity.async.CompletableFutures;
import com.hubspot.singularity.scheduler.SingularitySchedulerTestBase;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SingularityStartupTest extends SingularitySchedulerTestBase {
  private ExecutorService executorService = Executors.newSingleThreadExecutor();

  public SingularityStartupTest() {
    super(false);
  }

  @Inject
  private SingularityStartup startup;

  @Test
  public void testFailuresInLaunchPath() {
    initRequest();
    initFirstDeploy();

    SingularityTask task = prepTask();

    taskManager.createTaskAndDeletePendingTask(task);

    resourceOffers();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    CompletableFutures
      .allOf(startup.checkSchedulerForInconsistentState(executorService))
      .join();

    resourceOffers();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    taskManager.deleteLastActiveTaskStatus(task.getTaskId());

    resourceOffers();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    CompletableFutures
      .allOf(startup.checkSchedulerForInconsistentState(executorService))
      .join();

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testScheduledTasksDontGetRescheduled() {
    initScheduledRequest();
    deploy(firstDeployId);
    deployChecker.checkDeploys();

    Assertions.assertEquals(
      1,
      requestManager.getPendingRequests().size(),
      "NEW_DEPLOY request added"
    );
    Assertions.assertTrue(
      taskManager.getPendingTaskIds().isEmpty(),
      "No tasks started yet"
    );

    CompletableFutures
      .allOf(startup.checkSchedulerForInconsistentState(executorService))
      .join();

    Assertions.assertEquals(
      1,
      requestManager.getPendingRequests().size(),
      "NEW_DEPLOY request added"
    );
    Assertions.assertEquals(
      PendingType.NEW_DEPLOY,
      requestManager.getPendingRequests().get(0).getPendingType(),
      "NEW_DEPLOY is first"
    );
    Assertions.assertTrue(
      taskManager.getPendingTaskIds().isEmpty(),
      "No tasks started yet"
    );

    scheduler.drainPendingQueue();

    Assertions.assertTrue(
      requestManager.getPendingRequests().isEmpty(),
      "Pending queue is cleared"
    );
    List<SingularityPendingTask> pending = taskManager.getPendingTasks();

    Assertions.assertEquals(
      1,
      taskManager.getPendingTaskIds().size(),
      "One task is started"
    );
    Assertions.assertEquals(
      PendingType.NEW_DEPLOY,
      taskManager.getPendingTaskIds().get(0).getPendingType(),
      "First request takes precedence"
    );

    CompletableFutures
      .allOf(startup.checkSchedulerForInconsistentState(executorService))
      .join();
    scheduler.drainPendingQueue();

    Assertions.assertTrue(requestManager.getPendingRequests().isEmpty());
    Assertions.assertTrue(taskManager.getPendingTaskIds().size() == 1);
    Assertions.assertTrue(taskManager.getActiveTaskIds().isEmpty());

    Assertions.assertTrue(pending.equals(taskManager.getPendingTasks()));

    taskManager.deletePendingTask(pending.get(0).getPendingTaskId());

    CompletableFutures
      .allOf(startup.checkSchedulerForInconsistentState(executorService))
      .join();

    Assertions.assertTrue(requestManager.getPendingRequests().size() == 1);
    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());
  }

  @Test
  public void testScheduledTasksDontGetRescheduledDuringRun() {
    initScheduledRequest();
    initFirstDeploy();
    startTask(firstDeploy);

    CompletableFutures
      .allOf(startup.checkSchedulerForInconsistentState(executorService))
      .join();
    scheduler.drainPendingQueue();

    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assertions.assertTrue(requestManager.getPendingRequests().isEmpty());

    boolean caughtException = false;

    try {
      requestResource.scheduleImmediately(
        singularityUser,
        requestId,
        ((SingularityRunNowRequest) null)
      );
    } catch (Exception e) {
      caughtException = true;
    }

    Assertions.assertTrue(caughtException);
    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assertions.assertTrue(requestManager.getPendingRequests().isEmpty());
  }

  @Test
  public void testOnDemandDoesntGetRescheduled() {
    saveRequest(new SingularityRequestBuilder(requestId, RequestType.ON_DEMAND).build());
    deploy(firstDeployId);
    deployChecker.checkDeploys();

    Assertions.assertTrue(requestManager.getPendingRequests().isEmpty());
    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());

    CompletableFutures
      .allOf(startup.checkSchedulerForInconsistentState(executorService))
      .join();

    Assertions.assertTrue(requestManager.getPendingRequests().isEmpty());
    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());

    requestManager.addToPendingQueue(
      new SingularityPendingRequest(
        requestId,
        firstDeployId,
        System.currentTimeMillis(),
        Optional.<String>empty(),
        PendingType.ONEOFF,
        Optional.<Boolean>empty(),
        Optional.<String>empty()
      )
    );

    CompletableFutures
      .allOf(startup.checkSchedulerForInconsistentState(executorService))
      .join();

    Assertions.assertTrue(
      requestManager.getPendingRequests().get(0).getPendingType() == PendingType.ONEOFF
    );
  }

  @Test
  public void testRunOnceDoesntGetRescheduled() {
    saveRequest(new SingularityRequestBuilder(requestId, RequestType.RUN_ONCE).build());
    deploy(firstDeployId);
    deployChecker.checkDeploys();

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertTrue(requestManager.getPendingRequests().isEmpty());
    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());

    CompletableFutures
      .allOf(startup.checkSchedulerForInconsistentState(executorService))
      .join();

    // assert that SingularityStartup does not enqueue a SingularityPendingRequest (pendingType=NOT_STARTED) for the RUN_ONCE request
    Assertions.assertTrue(requestManager.getPendingRequests().isEmpty());
  }
}
