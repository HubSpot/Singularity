package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import org.apache.mesos.Protos.TaskState;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.data.history.SingularityRequestHistoryPersister;
import com.hubspot.singularity.data.history.SingularityTaskHistoryPersister;

import io.dropwizard.db.DataSourceFactory;

public class HistoryPersisterTest extends SingularitySchedulerTestBase {

  @Inject
  protected SingularityRequestHistoryPersister requestHistoryPersister;
  @Inject
  protected SingularityTaskHistoryPersister taskHistoryPersister;
  @Inject
  protected SingularityCleaner cleaner;

  public HistoryPersisterTest() {
    super(false);
  }

  @Test
  public void testRequestPurging() {
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
  public void testTaskPurging() {
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
