package com.hubspot.singularity.mesos;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.scheduler.SingularitySchedulerTestBase;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityTask;

public class SingularityStartupTest extends SingularitySchedulerTestBase {

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

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    startup.checkSchedulerForInconsistentState();

    resourceOffers();

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    taskManager.deleteActiveTask(task.getTaskId().getId());

    resourceOffers();

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    startup.checkSchedulerForInconsistentState();

    resourceOffers();

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }


  @Test
  public void testScheduledTasksDontGetRescheduled() {
    initScheduledRequest();
    deploy(firstDeployId);
    deployChecker.checkDeploys();

    Assert.assertTrue(requestManager.getPendingRequests().size() == 1);
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());

    startup.checkSchedulerForInconsistentState();

    Assert.assertTrue(requestManager.getPendingRequests().size() == 1);
    Assert.assertTrue(requestManager.getPendingRequests().get(0).getPendingType() == PendingType.NEW_DEPLOY);
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());

    scheduler.drainPendingQueue(stateCacheProvider.get());

    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());
    List<SingularityPendingTask> pending = taskManager.getPendingTasks();

    Assert.assertTrue(taskManager.getPendingTaskIds().size() == 1);

    startup.checkSchedulerForInconsistentState();
    scheduler.drainPendingQueue(stateCacheProvider.get());

    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());
    Assert.assertTrue(taskManager.getPendingTaskIds().size() == 1);
    Assert.assertTrue(taskManager.getActiveTaskIds().isEmpty());

    Assert.assertTrue(pending.equals(taskManager.getPendingTasks()));

    taskManager.deletePendingTask(pending.get(0).getPendingTaskId());

    startup.checkSchedulerForInconsistentState();

    Assert.assertTrue(requestManager.getPendingRequests().size() == 1);
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
  }

  @Test
  public void testScheduledTasksDontGetRescheduledDuringRun() {
    initScheduledRequest();
    initFirstDeploy();
    startTask(firstDeploy);

    startup.checkSchedulerForInconsistentState();
    scheduler.drainPendingQueue(stateCacheProvider.get());

    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());

    boolean caughtException = false;

    try {
      requestResource.scheduleImmediately(requestId);
    } catch (Exception e) {
      caughtException = true;
    }

    Assert.assertTrue(caughtException);
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());
  }

  @Test
  public void testOnDemandDoesntGetRescheduled() {
    saveRequest(new SingularityRequestBuilder(requestId, RequestType.ON_DEMAND).build());
    deploy(firstDeployId);
    deployChecker.checkDeploys();

    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());

    startup.checkSchedulerForInconsistentState();

    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());

    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, firstDeployId, System.currentTimeMillis(), Optional.<String> absent(), PendingType.ONEOFF, Optional.<Boolean> absent(), Optional.<String> absent()));

    startup.checkSchedulerForInconsistentState();

    Assert.assertTrue(requestManager.getPendingRequests().get(0).getPendingType() == PendingType.ONEOFF);
  }

  @Test
  public void testRunOnceDoesntGetRescheduled() {
    saveRequest(new SingularityRequestBuilder(requestId, RequestType.RUN_ONCE).build());
    deploy(firstDeployId);
    deployChecker.checkDeploys();

    scheduler.drainPendingQueue(stateCacheProvider.get());
    resourceOffers();

    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());

    startup.checkSchedulerForInconsistentState();

    // assert that SingularityStartup does not enqueue a SingularityPendingRequest (pendingType=STARTUP) for the RUN_ONCE request
    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());
  }
}
