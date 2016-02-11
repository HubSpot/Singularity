package com.hubspot.singularity.scheduler;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.Protos.TaskState;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.data.history.SingularityDeployHistoryPersister;
import com.hubspot.singularity.data.history.SingularityRequestHistoryPersister;
import com.hubspot.singularity.data.history.SingularityTaskHistoryPersister;

import io.dropwizard.db.DataSourceFactory;

public class HistoryPersisterTest extends SingularitySchedulerTestBase {

  @Inject
  protected SingularityRequestHistoryPersister requestHistoryPersister;
  @Inject
  protected SingularityTaskHistoryPersister taskHistoryPersister;
  @Inject
  protected SingularityDeployHistoryPersister deployHistoryPersister;
  @Inject
  protected SingularityCleaner cleaner;

  public HistoryPersisterTest() {
    super(false);
  }

  @Test
  public void testRequestAgePurging() {
    initRequest();

    configuration.setDeleteStaleRequestsFromZkWhenNoDatabaseAfterHours(7);

    requestHistoryPersister.runActionOnPoll();

    Assert.assertTrue(!requestManager.getRequestHistory(requestId).isEmpty());

    requestManager.deleteRequest(request, user, Optional.<String> absent(), Optional.<String> absent());

    requestManager.deleteHistoryParent(requestId);

    requestManager.activate(request, RequestHistoryType.CREATED, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3), Optional.<String> absent(), Optional.<String> absent());
    requestManager.cooldown(request, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2));

    requestHistoryPersister.runActionOnPoll();

    Assert.assertTrue(!requestManager.getRequestHistory(requestId).isEmpty());

    configuration.setDeleteStaleRequestsFromZkWhenNoDatabaseAfterHours(1);

    requestHistoryPersister.runActionOnPoll();

    Assert.assertTrue(requestManager.getRequestHistory(requestId).isEmpty());
  }

  @Test
  public void testRequestCountPurging() {
    final SingularityRequest requestOne = new SingularityRequestBuilder("request1", RequestType.WORKER).build();

    saveRequest(requestOne);

    configuration.setMaxRequestHistoryUpdatesPerRequestInZkWhenNoDatabase(Optional.of(2));
    configuration.setDeleteStaleRequestsFromZkWhenNoDatabaseAfterHours(7);

    requestManager.deleteRequest(requestOne, user, Optional.<String>absent(), Optional.<String>absent());
    requestManager.deleteHistoryParent(requestOne.getId());
    final long actionOneTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4);
    requestManager.activate(requestOne, RequestHistoryType.CREATED, actionOneTimestamp, Optional.<String> absent(), Optional.<String> absent());
    final long actionTwoTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3);
    requestManager.cooldown(requestOne, actionTwoTimestamp);
    final long actionThreeTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2);
    requestManager.activate(requestOne, RequestHistoryType.CREATED, actionThreeTimestamp, Optional.<String> absent(), Optional.<String> absent());
    final long actionFourTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
    requestManager.cooldown(requestOne, actionFourTimestamp);

    requestHistoryPersister.runActionOnPoll();

    final List<SingularityRequestHistory> requestHistoryItems = requestManager.getRequestHistory(requestOne.getId());
    Collections.sort(requestHistoryItems);

    Assert.assertEquals(2, requestHistoryItems.size());
    Assert.assertEquals(actionFourTimestamp, requestHistoryItems.get(0).getCreatedAt());
    Assert.assertEquals(actionThreeTimestamp, requestHistoryItems.get(1).getCreatedAt());
  }

  @Test
  public void testTaskAgePurging() {
    initLoadBalancedRequest();
    initFirstDeploy();

    SingularityTask taskOne = launchTask(request, firstDeploy, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3), 1, TaskState.TASK_RUNNING);
    SingularityTask taskTwo = launchTask(request, firstDeploy, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3), 2, TaskState.TASK_RUNNING);

    taskHistoryPersister.runActionOnPoll();

    Assert.assertTrue(taskManager.getTaskHistory(taskOne.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskTwo.getTaskId()).isPresent());

    configuration.setDeleteTasksFromZkWhenNoDatabaseAfterHours(1);

    taskHistoryPersister.runActionOnPoll();

    Assert.assertTrue(taskManager.getTaskHistory(taskOne.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskTwo.getTaskId()).isPresent());

    statusUpdate(taskOne, TaskState.TASK_FINISHED, Optional.of(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2)));

    taskHistoryPersister.runActionOnPoll();

    Assert.assertTrue(taskManager.getTaskHistory(taskOne.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskTwo.getTaskId()).isPresent());

    cleaner.drainCleanupQueue();

    taskHistoryPersister.runActionOnPoll();

    Assert.assertTrue(!taskManager.getTaskHistory(taskOne.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskTwo.getTaskId()).isPresent());
  }

  @Test
  public void testTaskCountPurging() {
    initLoadBalancedRequest();
    initFirstDeploy();

    SingularityTask taskOne = launchTask(request, firstDeploy, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4), 1, TaskState.TASK_RUNNING);
    SingularityTask taskTwo = launchTask(request, firstDeploy, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3), 2, TaskState.TASK_RUNNING);
    SingularityTask taskThree = launchTask(request, firstDeploy, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2), 3, TaskState.TASK_RUNNING);
    SingularityTask taskFour = launchTask(request, firstDeploy, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1), 4, TaskState.TASK_RUNNING);

    taskHistoryPersister.runActionOnPoll();

    Assert.assertTrue(taskManager.getTaskHistory(taskOne.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskTwo.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskThree.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskFour.getTaskId()).isPresent());

    configuration.setMaxStaleTasksPerRequestInZkWhenNoDatabase(Optional.of(2));

    taskHistoryPersister.runActionOnPoll();

    Assert.assertTrue(taskManager.getTaskHistory(taskOne.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskTwo.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskThree.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskFour.getTaskId()).isPresent());

    statusUpdate(taskOne, TaskState.TASK_FINISHED, Optional.of(System.currentTimeMillis()));

    taskHistoryPersister.runActionOnPoll();

    Assert.assertTrue(taskManager.getTaskHistory(taskOne.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskTwo.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskThree.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskFour.getTaskId()).isPresent());

    cleaner.drainCleanupQueue();

    taskHistoryPersister.runActionOnPoll();

    Assert.assertTrue(taskManager.getTaskHistory(taskOne.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskTwo.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskThree.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskFour.getTaskId()).isPresent());

    statusUpdate(taskTwo, TaskState.TASK_FINISHED, Optional.of(System.currentTimeMillis()));

    taskHistoryPersister.runActionOnPoll();

    Assert.assertTrue(taskManager.getTaskHistory(taskOne.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskTwo.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskThree.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskFour.getTaskId()).isPresent());

    cleaner.drainCleanupQueue();

    taskHistoryPersister.runActionOnPoll();

    Assert.assertTrue(taskManager.getTaskHistory(taskOne.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskTwo.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskThree.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskFour.getTaskId()).isPresent());

    statusUpdate(taskThree, TaskState.TASK_FINISHED, Optional.of(System.currentTimeMillis()));

    taskHistoryPersister.runActionOnPoll();

    Assert.assertTrue(taskManager.getTaskHistory(taskOne.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskTwo.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskThree.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskFour.getTaskId()).isPresent());

    cleaner.drainCleanupQueue();

    taskHistoryPersister.runActionOnPoll();

    Assert.assertTrue(!taskManager.getTaskHistory(taskOne.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskTwo.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskThree.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskFour.getTaskId()).isPresent());

    statusUpdate(taskFour, TaskState.TASK_FINISHED, Optional.of(System.currentTimeMillis()));

    taskHistoryPersister.runActionOnPoll();

    Assert.assertTrue(!taskManager.getTaskHistory(taskOne.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskTwo.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskThree.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskFour.getTaskId()).isPresent());

    cleaner.drainCleanupQueue();

    taskHistoryPersister.runActionOnPoll();

    Assert.assertTrue(!taskManager.getTaskHistory(taskOne.getTaskId()).isPresent());
    Assert.assertTrue(!taskManager.getTaskHistory(taskTwo.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskThree.getTaskId()).isPresent());
    Assert.assertTrue(taskManager.getTaskHistory(taskFour.getTaskId()).isPresent());
  }

  @Test
  public void testDeployCountPurging() {
    SingularityRequest requestOne = buildRequest("request1");
    SingularityRequest requestTwo = buildRequest("request2");

    SingularityDeploy requestOneDeployOne = initAndFinishDeploy(requestOne, "r1d1");
    SingularityDeploy requestOneDeployTwo = initAndFinishDeploy(requestOne, "r1d2");
    SingularityDeploy requestOneDeployThree = initAndFinishDeploy(requestOne, "r1d3");
    SingularityDeploy requestOneDeployFour = initAndFinishDeploy(requestOne, "r1d4");  // r1d4 is the active deploy, not eligible for purging
    SingularityDeploy requestTwoDeployOne = initAndFinishDeploy(requestTwo, "r2d1");
    SingularityDeploy requestTwoDeployTwo = initAndFinishDeploy(requestTwo, "r2d2");  // r2d2 is the active deploy, not eligible for purging

    configuration.setMaxStaleDeploysPerRequestInZkWhenNoDatabase(Optional.of(2));

    deployHistoryPersister.runActionOnPoll();

    Assert.assertTrue(!deployManager.getDeployHistory(requestOneDeployOne.getRequestId(), requestOneDeployOne.getId(), true).isPresent());
    Assert.assertTrue(deployManager.getDeployHistory(requestOneDeployTwo.getRequestId(), requestOneDeployTwo.getId(), true).isPresent());
    Assert.assertTrue(deployManager.getDeployHistory(requestOneDeployThree.getRequestId(), requestOneDeployThree.getId(), true).isPresent());
    Assert.assertTrue(deployManager.getDeployHistory(requestOneDeployFour.getRequestId(), requestOneDeployFour.getId(), true).isPresent());
    Assert.assertTrue(deployManager.getDeployHistory(requestTwoDeployOne.getRequestId(), requestTwoDeployOne.getId(), true).isPresent());
    Assert.assertTrue(deployManager.getDeployHistory(requestTwoDeployTwo.getRequestId(), requestTwoDeployTwo.getId(), true).isPresent());
  }

  @Test
  public void testPurgingDoesntApplyIfDatabasePresent() {
    initRequest();
    initFirstDeploy();

    requestManager.deleteRequest(request, user, Optional.<String> absent(), Optional.<String> absent());

    requestManager.deleteHistoryParent(requestId);

    requestManager.activate(request, RequestHistoryType.CREATED, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3), Optional.<String> absent(), Optional.<String> absent());

    configuration.setDatabaseConfiguration(new DataSourceFactory());

    configuration.setDeleteStaleRequestsFromZkWhenNoDatabaseAfterHours(1);

    requestHistoryPersister.runActionOnPoll();

    Assert.assertTrue(!requestManager.getRequestHistory(requestId).isEmpty());
  }



}
