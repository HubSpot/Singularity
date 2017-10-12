package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.v1.Protos.TaskState;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
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

    requestManager.startDeletingRequest(request, Optional.absent(), user, Optional.<String> absent(), Optional.<String> absent());

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
    final SingularityRequest requestTwo = new SingularityRequestBuilder("request2", RequestType.WORKER).build();
    final SingularityRequest requestThree = new SingularityRequestBuilder("request3", RequestType.WORKER).build();

    saveRequest(requestOne);
    saveRequest(requestTwo);
    saveRequest(requestThree);

    configuration.setMaxRequestsWithHistoryInZkWhenNoDatabase(Optional.of(2));
    configuration.setDeleteStaleRequestsFromZkWhenNoDatabaseAfterHours(7);

    requestManager.startDeletingRequest(requestOne, Optional.absent(), user, Optional.<String>absent(), Optional.<String>absent());
    requestManager.deleteHistoryParent(requestOne.getId());
    requestManager.activate(requestOne, RequestHistoryType.CREATED, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4), Optional.<String> absent(), Optional.<String> absent());
    requestManager.cooldown(requestOne, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3));

    requestManager.startDeletingRequest(requestTwo, Optional.absent(), user, Optional.<String>absent(), Optional.<String>absent());
    requestManager.deleteHistoryParent(requestTwo.getId());
    requestManager.activate(requestTwo, RequestHistoryType.CREATED, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4), Optional.<String> absent(), Optional.<String> absent());
    requestManager.cooldown(requestTwo, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3));

    requestManager.startDeletingRequest(requestThree, Optional.absent(), user, Optional.<String>absent(), Optional.<String>absent());
    requestManager.deleteHistoryParent(requestThree.getId());
    requestManager.activate(requestThree, RequestHistoryType.CREATED, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4), Optional.<String> absent(), Optional.<String> absent());
    requestManager.cooldown(requestThree, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3));

    Assert.assertEquals(2, requestManager.getRequestHistory(requestOne.getId()).size());
    Assert.assertEquals(2, requestManager.getRequestHistory(requestTwo.getId()).size());
    Assert.assertEquals(2, requestManager.getRequestHistory(requestThree.getId()).size());

    requestHistoryPersister.runActionOnPoll();

    Assert.assertEquals(0, requestManager.getRequestHistory(requestOne.getId()).size());
    Assert.assertEquals(2, requestManager.getRequestHistory(requestTwo.getId()).size());
    Assert.assertEquals(2, requestManager.getRequestHistory(requestThree.getId()).size());
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

    configuration.setMaxStaleTasksPerRequestInZkWhenNoDatabase(Optional.of(2));
    final int tasksToLaunch = 10;

    final List<SingularityTaskId> taskIds = new ArrayList<>();

    for (int i=0; i<tasksToLaunch; i++) {
      final SingularityTask task = launchTask(request, firstDeploy, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3), 1, TaskState.TASK_RUNNING);
      statusUpdate(task, TaskState.TASK_FINISHED, Optional.of(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2)));
      cleaner.drainCleanupQueue();

      taskIds.add(task.getTaskId());
    }

    final List<SingularityTaskId> tasksBeforePurge = taskManager.getInactiveTaskIdsForDeploy(requestId, firstDeployId);
    Assert.assertEquals(taskIds.size(), tasksBeforePurge.size());
    Assert.assertTrue(tasksBeforePurge.containsAll(taskIds));

    taskHistoryPersister.runActionOnPoll();

    final List<SingularityTaskId> tasksAfterPurge = taskManager.getInactiveTaskIdsForDeploy(requestId, firstDeployId);
    Assert.assertEquals(configuration.getMaxStaleTasksPerRequestInZkWhenNoDatabase().get().intValue(), tasksAfterPurge.size());
    Assert.assertTrue(tasksAfterPurge.containsAll(taskIds.subList(tasksToLaunch-2, tasksToLaunch-1)));  // we should just have the last 2 tasks
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

    requestManager.startDeletingRequest(request, Optional.absent(), user, Optional.<String> absent(), Optional.<String> absent());

    requestManager.deleteHistoryParent(requestId);

    requestManager.activate(request, RequestHistoryType.CREATED, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3), Optional.<String> absent(), Optional.<String> absent());

    configuration.setDatabaseConfiguration(new DataSourceFactory());

    configuration.setDeleteStaleRequestsFromZkWhenNoDatabaseAfterHours(1);

    requestHistoryPersister.runActionOnPoll();

    Assert.assertTrue(!requestManager.getRequestHistory(requestId).isEmpty());
  }



}
