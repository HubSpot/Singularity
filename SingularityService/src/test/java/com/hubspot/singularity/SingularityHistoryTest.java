package com.hubspot.singularity;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.TaskState;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.api.SingularityDeleteRequestRequest;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.config.HistoryPurgingConfiguration;
import com.hubspot.singularity.data.MetadataManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.SingularityHistoryPurger;
import com.hubspot.singularity.data.history.SingularityRequestHistoryPersister;
import com.hubspot.singularity.data.history.SingularityTaskHistoryPersister;
import com.hubspot.singularity.data.history.TaskHistoryHelper;
import com.hubspot.singularity.scheduler.SingularitySchedulerTestBase;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.FileSystemResourceAccessor;

public class SingularityHistoryTest extends SingularitySchedulerTestBase {

  @Inject
  protected Provider<DBI> dbiProvider;

  @Inject
  protected HistoryManager historyManager;

  @Inject
  protected MetadataManager metadataManager;

  @Inject
  protected SingularityTaskHistoryPersister taskHistoryPersister;

  @Inject
  protected SingularityRequestHistoryPersister requestHistoryPersister;

  @Inject
  protected SingularityTestAuthenticator testAuthenticator;

  @Inject
  protected TaskHistoryHelper taskHistoryHelper;

  public SingularityHistoryTest() {
    super(true);
  }

  @Before
  public void createTestData() throws Exception {
    Handle handle = dbiProvider.get().open();

    Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(handle.getConnection()));

    Liquibase liquibase = new Liquibase("singularity_test.sql", new FileSystemResourceAccessor(), database);
    liquibase.update((String) null);

    try {
      database.close();
    } catch (Throwable t) {
    }

    handle.close();
  }

  @After
  public void blowDBAway() {
    Handle handle = dbiProvider.get().open();

    handle.execute("DELETE FROM taskHistory;DELETE FROM requestHistory;DELETE FROM deployHistory;");

    handle.close();
  }

  private SingularityTaskHistory buildTask(long launchTime) {
    SingularityTask task = prepTask(request, firstDeploy, launchTime, 1);

    return new SingularityTaskHistory(null, Optional.<String> absent(), Optional.<String>absent(), null, task, null, null, null);
  }

  private void saveTasks(int num, long launchTime) {
    for (int i = 0; i < num; i++) {
      SingularityTaskHistory taskHistory = buildTask(launchTime + i);

      historyManager.saveTaskHistory(taskHistory);
    }
  }


  private List<SingularityTaskIdHistory> getTaskHistoryForRequest(String requestId, int start, int limit) {
    return historyManager.getTaskIdHistory(Optional.of(requestId), Optional.<String> absent(), Optional.<String>absent(), Optional.<String> absent(), Optional.<ExtendedTaskState> absent(),
        Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<OrderDirection> absent(), Optional.of(start), limit);
  }

  @Test
  public void testHistoryDoesntHaveActiveTasks() {
    initRequest();
    initFirstDeploy();

    SingularityTask taskOne = launchTask(request, firstDeploy, 1L, 10L, 1, TaskState.TASK_RUNNING, true);
    SingularityTask taskTwo = launchTask(request, firstDeploy, 2l, 10L, 2, TaskState.TASK_RUNNING, true);
    SingularityTask taskThree = launchTask(request, firstDeploy, 3l, 10L, 3, TaskState.TASK_RUNNING, true);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.<String>absent(), Optional.<String> absent(), Optional.<ExtendedTaskState> absent(),
        Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<OrderDirection> absent()), 0, 10), 0);
  }

  @Test
  public void historyUpdaterTest() {
    initRequest();
    initFirstDeploy();

    HistoryPurgingConfiguration historyPurgingConfiguration = new HistoryPurgingConfiguration();
    historyPurgingConfiguration.setEnabled(true);
    historyPurgingConfiguration.setDeleteTaskHistoryBytesAfterDays(1);

    SingularityTaskHistory taskHistory = buildTask(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3));

    historyManager.saveTaskHistory(taskHistory);

    Assert.assertTrue(historyManager.getTaskHistory(taskHistory.getTask().getTaskId().getId()).get().getTask() != null);

    Assert.assertEquals(1, getTaskHistoryForRequest(requestId, 0, 100).size());

    SingularityHistoryPurger purger = new SingularityHistoryPurger(historyPurgingConfiguration, historyManager, taskManager, deployManager, requestManager, metadataManager);

    purger.runActionOnPoll();

    Assert.assertEquals(1, getTaskHistoryForRequest(requestId, 0, 100).size());

    Assert.assertTrue(!historyManager.getTaskHistory(taskHistory.getTask().getTaskId().getId()).isPresent());
  }

  @Test
  public void historyPurgerTest() {
    initRequest();
    initFirstDeploy();

    saveTasks(3, System.currentTimeMillis());

    Assert.assertEquals(3, getTaskHistoryForRequest(requestId, 0, 10).size());

    HistoryPurgingConfiguration historyPurgingConfiguration = new HistoryPurgingConfiguration();
    historyPurgingConfiguration.setEnabled(true);
    historyPurgingConfiguration.setDeleteTaskHistoryAfterDays(10);

    SingularityHistoryPurger purger = new SingularityHistoryPurger(historyPurgingConfiguration, historyManager, taskManager, deployManager, requestManager, metadataManager);

    purger.runActionOnPoll();

    Assert.assertEquals(3, getTaskHistoryForRequest(requestId, 0, 10).size());

    historyPurgingConfiguration.setDeleteTaskHistoryAfterTasksPerRequest(1);

    purger.runActionOnPoll();

    Assert.assertEquals(1, getTaskHistoryForRequest(requestId, 0, 10).size());

    historyPurgingConfiguration.setDeleteTaskHistoryAfterTasksPerRequest(25);
    historyPurgingConfiguration.setDeleteTaskHistoryAfterDays(100);

    purger.runActionOnPoll();

    Assert.assertEquals(1, getTaskHistoryForRequest(requestId, 0, 10).size());

    saveTasks(10, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(200));

    purger.runActionOnPoll();

    Assert.assertEquals(1, getTaskHistoryForRequest(requestId, 0, 10).size());
  }

  @Test
  public void testRunId() {
    initScheduledRequest();
    initFirstDeploy();

    String runId = "my-run-id";

    SingularityPendingRequestParent parent = requestResource.scheduleImmediately(requestId,
        new SingularityRunNowRequest(Optional.<String> absent(), Optional.<Boolean> absent(), Optional.of(runId), Optional.<List<String>> absent(), Optional.<Resources>absent(), Optional.<Long>absent()));

    Assert.assertEquals(runId, parent.getPendingRequest().getRunId().get());

    resourceOffers();

    Assert.assertEquals(runId, taskManager.getActiveTasks().get(0).getTaskRequest().getPendingTask().getRunId().get());

    SingularityTaskId taskId = taskManager.getActiveTaskIds().get(0);

    statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_FINISHED);

    configuration.setTaskPersistAfterStartupBufferMillis(0);
    taskMetadataConfiguration.setTaskPersistAfterFinishBufferMillis(0);

    taskHistoryPersister.runActionOnPoll();

    Assert.assertEquals(runId, historyManager.getTaskHistory(taskId.getId()).get().getTask().getTaskRequest().getPendingTask().getRunId().get());
    Assert.assertEquals(runId, getTaskHistoryForRequest(requestId, 0, 10).get(0).getRunId().get());

    parent = requestResource.scheduleImmediately(requestId);

    Assert.assertTrue(parent.getPendingRequest().getRunId().isPresent());
  }

  @Test
  public void testTaskBufferPersist() {
    initRequest();
    initFirstDeploy();

    taskMetadataConfiguration.setTaskPersistAfterFinishBufferMillis(TimeUnit.MINUTES.toMillis(100));

    SingularityTask task = launchTask(request, firstDeploy, System.currentTimeMillis(), System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(3), 1, TaskState.TASK_RUNNING);

    statusUpdate(task, TaskState.TASK_FINISHED, Optional.of(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(2)));

    taskHistoryPersister.runActionOnPoll();

    Assert.assertEquals(1, taskManager.getAllTaskIds().size());

    configuration.setTaskPersistAfterStartupBufferMillis(0);

    taskHistoryPersister.runActionOnPoll();

    Assert.assertEquals(1, taskManager.getAllTaskIds().size());

    taskMetadataConfiguration.setTaskPersistAfterFinishBufferMillis(0);

    taskHistoryPersister.runActionOnPoll();

    Assert.assertEquals(0, taskManager.getAllTaskIds().size());
  }

  @Test
  public void testPersisterRaceCondition() {
    final TaskManager taskManagerSpy = spy(taskManager);
    final TaskHistoryHelper taskHistoryHelperWithMockedTaskManager = new TaskHistoryHelper(taskManagerSpy, historyManager, requestManager, configuration);

    initScheduledRequest();
    initFirstDeploy();

    requestResource.scheduleImmediately(requestId);

    resourceOffers();

    final SingularityTaskId taskId = taskManager.getActiveTaskIds().get(0);

    statusUpdate(taskManager.getTask(taskId).get(), Protos.TaskState.TASK_FINISHED, Optional.of(System.currentTimeMillis()));

    // persist inactive task(s)
    taskHistoryPersister.runActionOnPoll();

    // mimic the persister race condition by overriding the inactive task IDs in ZK to the persisted task ID
    doReturn(Arrays.asList(taskId)).when(taskManagerSpy).getInactiveTaskIdsForRequest(eq(requestId));

    // assert that the history works, but more importantly, that we don't NPE
    Assert.assertEquals(1, taskHistoryHelperWithMockedTaskManager.getBlendedHistory(new SingularityTaskHistoryQuery(requestId), 0, 5).size());
  }

  @Test
  public void testTaskSearchByRequest() {
    initOnDemandRequest();
    initFirstDeploy();

    SingularityTask taskOne = launchTask(request, firstDeploy, 10000L, 10L, 1, TaskState.TASK_RUNNING, true);
    SingularityTask taskThree = launchTask(request, firstDeploy, 20000l, 10L, 2, TaskState.TASK_RUNNING, true);
    SingularityTask taskFive = launchTask(request, firstDeploy, 30000l, 10L, 3, TaskState.TASK_RUNNING, true);

    requestId = "test-request-2";

    initOnDemandRequest();
    initFirstDeploy();

    SingularityTask taskTwo = launchTask(request, firstDeploy, 15000L, 10L, 1, TaskState.TASK_RUNNING, true);
    SingularityTask taskFour = launchTask(request, firstDeploy, 25000l, 10L, 2, TaskState.TASK_RUNNING, true);
    SingularityTask taskSix = launchTask(request, firstDeploy, 35000l, 10L, 3, TaskState.TASK_RUNNING, true);
    SingularityTask taskSeven  = launchTask(request, firstDeploy, 70000l, 10L, 7, TaskState.TASK_RUNNING, true);

    statusUpdate(taskOne, TaskState.TASK_FAILED);
    statusUpdate(taskTwo, TaskState.TASK_FINISHED);
    statusUpdate(taskSix, TaskState.TASK_KILLED);
    statusUpdate(taskFour, TaskState.TASK_LOST);

    taskHistoryPersister.runActionOnPoll();

    statusUpdate(taskThree, TaskState.TASK_FAILED);
    statusUpdate(taskFive, TaskState.TASK_FINISHED);
    statusUpdate(taskSeven, TaskState.TASK_KILLED);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.<ExtendedTaskState> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<OrderDirection> absent()), 0, 3), 3,
        taskSeven, taskSix, taskFive);

    taskHistoryPersister.runActionOnPoll();

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.<ExtendedTaskState> absent(), Optional.of(70000L), Optional.of(20000L), Optional.<Long> absent(), Optional.<Long> absent(), Optional.of(OrderDirection.ASC)), 0, 3), 3,
        taskFour, taskFive, taskSix);
  }

  @Test
  public void testTaskSearchQueryInZkOnly() {
    initOnDemandRequest();
    initFirstDeploy();

    SingularityTask taskOne = launchTask(request, firstDeploy, 1L, 10L, 1, TaskState.TASK_RUNNING, true);
    SingularityTask taskTwo = launchTask(request, firstDeploy, 2L, 10L, 2, TaskState.TASK_RUNNING, true);
    SingularityTask taskThree = launchTask(request, firstDeploy, 3L, 10L, 3, TaskState.TASK_RUNNING, true);

    SingularityDeployMarker marker = initSecondDeploy();
    finishDeploy(marker, secondDeploy);

    SingularityTask taskFour = launchTask(request, secondDeploy, 4L, 10L, 4, TaskState.TASK_RUNNING, true);
    SingularityTask taskFive = launchTask(request, secondDeploy, 5L, 10L, 5, TaskState.TASK_RUNNING, true);
    SingularityTask taskSix = launchTask(request, secondDeploy, 6L, 10L, 6, TaskState.TASK_RUNNING, true);
    SingularityTask taskSeven  = launchTask(request, secondDeploy, 7L, 10L, 7, TaskState.TASK_RUNNING, true);

    statusUpdate(taskOne, TaskState.TASK_FAILED, Optional.of(20000L));
    statusUpdate(taskTwo, TaskState.TASK_FINISHED, Optional.of(21000L));
    statusUpdate(taskSix, TaskState.TASK_KILLED, Optional.of(22000L));
    statusUpdate(taskFour, TaskState.TASK_LOST, Optional.of(23000L));

    statusUpdate(taskThree, TaskState.TASK_FAILED, Optional.of(24000L));
    statusUpdate(taskFive, TaskState.TASK_FINISHED, Optional.of(25000L));
    statusUpdate(taskSeven, TaskState.TASK_KILLED, Optional.of(26000L));

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.<ExtendedTaskState> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<OrderDirection> absent()), 0, 3), 3,
        taskSeven, taskSix, taskFive);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.of(secondDeployId), Optional.<String> absent(), Optional.of("host4"),
        Optional.<ExtendedTaskState> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<OrderDirection> absent()), 0, 3), 1,
        taskFour);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.of(firstDeployId), Optional.<String> absent(), Optional.<String> absent(),
        Optional.of(ExtendedTaskState.TASK_FAILED), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.of(OrderDirection.ASC)), 0, 3), 2,
        taskOne, taskThree);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.of(ExtendedTaskState.TASK_LOST), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<OrderDirection> absent()), 0, 3), 1,
        taskFour);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.<ExtendedTaskState> absent(), Optional.of(7L), Optional.of(2L), Optional.<Long> absent(), Optional.<Long> absent(), Optional.of(OrderDirection.ASC)), 0, 3), 3,
        taskThree, taskFour, taskFive);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.<ExtendedTaskState> absent(), Optional.of(7L), Optional.of(2L), Optional.<Long> absent(), Optional.<Long> absent(), Optional.of(OrderDirection.ASC)), 1, 3), 3,
        taskFour, taskFive, taskSix);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.<ExtendedTaskState> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.of(26000L), Optional.of(21000L), Optional.of(OrderDirection.ASC)), 0, 3), 3,
      taskThree, taskFour, taskFive);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.<ExtendedTaskState> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.of(26000L), Optional.of(21000L), Optional.of(OrderDirection.ASC)), 1, 3), 3,
      taskFour, taskFive, taskSix);
  }

  @Test
  public void testTaskSearchQueryBlended() {
    initOnDemandRequest();
    initFirstDeploy();

    SingularityTask taskOne = launchTask(request, firstDeploy, 10000L, 10L, 1, TaskState.TASK_RUNNING, true, Optional.of("test-run-id-1"));
    SingularityTask taskTwo = launchTask(request, firstDeploy, 20000L, 10L, 2, TaskState.TASK_RUNNING, true);
    SingularityTask taskThree = launchTask(request, firstDeploy, 30000L, 10L, 3, TaskState.TASK_RUNNING, true);

    SingularityDeployMarker marker = initSecondDeploy();
    finishDeploy(marker, secondDeploy);

    SingularityTask taskFour = launchTask(request, secondDeploy, 40000L, 10L, 4, TaskState.TASK_RUNNING, true);
    SingularityTask taskFive = launchTask(request, secondDeploy, 50000L, 10L, 5, TaskState.TASK_RUNNING, true, Optional.of("test-run-id-5"));
    SingularityTask taskSix = launchTask(request, secondDeploy, 60000L, 10L, 6, TaskState.TASK_RUNNING, true);
    SingularityTask taskSeven = launchTask(request, secondDeploy, 70000L, 10L, 7, TaskState.TASK_RUNNING, true);

    statusUpdate(taskOne, TaskState.TASK_FAILED, Optional.of(80000L));
    statusUpdate(taskTwo, TaskState.TASK_FINISHED, Optional.of(90000L));
    statusUpdate(taskSix, TaskState.TASK_KILLED, Optional.of(100000L));
    statusUpdate(taskFour, TaskState.TASK_LOST, Optional.of(110000L));

    taskHistoryPersister.runActionOnPoll();

    statusUpdate(taskThree, TaskState.TASK_FAILED, Optional.of(120000L));
    statusUpdate(taskFive, TaskState.TASK_FINISHED, Optional.of(130000L));
    statusUpdate(taskSeven, TaskState.TASK_KILLED, Optional.of(140000L));

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.of(firstDeployId), Optional.<String> absent(), Optional.of("host1"),
        Optional.<ExtendedTaskState> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<OrderDirection> absent()), 0, 3), 1,
        taskOne);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.of(firstDeployId), Optional.<String> absent(), Optional.<String> absent(),
        Optional.of(ExtendedTaskState.TASK_FAILED), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.of(OrderDirection.ASC)), 0, 3), 2,
        taskOne, taskThree);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.of(ExtendedTaskState.TASK_LOST), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<OrderDirection> absent()), 0, 3), 1,
        taskFour);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.<ExtendedTaskState> absent(), Optional.of(70000L), Optional.of(20000L), Optional.<Long> absent(), Optional.<Long> absent(), Optional.of(OrderDirection.DESC)), 0, 3), 3,
        taskSix, taskFive, taskFour);

    taskHistoryPersister.runActionOnPoll();

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.<ExtendedTaskState> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<OrderDirection> absent()), 0, 3), 3,
        taskSeven, taskSix, taskFive);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.<ExtendedTaskState> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<OrderDirection> absent()), 2, 1), 1,
        taskFive);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.of(secondDeployId), Optional.<String> absent(), Optional.of("host4"),
        Optional.<ExtendedTaskState> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<OrderDirection> absent()), 0, 3), 1,
        taskFour);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.of(firstDeployId), Optional.<String> absent(), Optional.<String> absent(),
        Optional.of(ExtendedTaskState.TASK_FAILED), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.of(OrderDirection.ASC)), 0, 3), 2,
        taskOne, taskThree);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.of(ExtendedTaskState.TASK_LOST), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<OrderDirection> absent()), 0, 3), 1,
        taskFour);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.<ExtendedTaskState> absent(), Optional.of(70000L), Optional.of(20000L), Optional.<Long> absent(), Optional.<Long> absent(), Optional.of(OrderDirection.ASC)), 0, 3), 3,
        taskThree, taskFour, taskFive);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.<ExtendedTaskState> absent(), Optional.of(70000L), Optional.of(20000L), Optional.<Long> absent(), Optional.<Long> absent(), Optional.of(OrderDirection.ASC)), 1, 3), 3,
        taskFour, taskFive, taskSix);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.<ExtendedTaskState> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.of(140000L), Optional.of(90000L), Optional.of(OrderDirection.ASC)), 0, 3), 3,
      taskThree, taskFour, taskFive);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.<String> absent(), Optional.<String> absent(),
        Optional.<ExtendedTaskState> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.of(140000L), Optional.of(90000L), Optional.of(OrderDirection.ASC)), 1, 3), 3,
      taskFour, taskFive, taskSix);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.of("test-run-id-1"), Optional.<String> absent(),
        Optional.<ExtendedTaskState> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.of(OrderDirection.ASC)), 0, 1), 1,
      taskOne);

    match(taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), Optional.<String> absent(), Optional.of("test-run-id-5"), Optional.<String> absent(),
        Optional.<ExtendedTaskState> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.of(OrderDirection.ASC)), 0, 1), 1,
      taskFive);
  }

  private void match(List<SingularityTaskIdHistory> history, int num, SingularityTask... tasks) {
    Assert.assertEquals(num, history.size());

    for (int i = 0; i < tasks.length; i++) {
      SingularityTaskIdHistory idHistory = history.get(i);
      SingularityTask task = tasks[i];

      Assert.assertEquals(task.getTaskId(), idHistory.getTaskId());
    }
  }

  @Test
  public void testMessage() {
    initRequest();

    String msg = null;
    for (int i = 0; i < 300; i++) {
      msg = msg + i;
    }

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(2), Optional.<Long> absent(), Optional.<Boolean> absent(), Optional.<String> absent(), Optional.of(msg), Optional.<Boolean>absent(), Optional.<Boolean>absent()));
    requestResource.deleteRequest(requestId, Optional.of(new SingularityDeleteRequestRequest(Optional.of("a msg"), Optional.<String> absent(), Optional.absent())));

    cleaner.drainCleanupQueue();

    requestHistoryPersister.runActionOnPoll();

    List<SingularityRequestHistory> history = historyManager.getRequestHistory(requestId, Optional.of(OrderDirection.DESC), 0, 100);

    Assert.assertEquals(4, history.size());

    for (SingularityRequestHistory historyItem : history) {
      if (historyItem.getEventType() == RequestHistoryType.DELETED) {
        Assert.assertEquals("a msg", historyItem.getMessage().get());
      } else if (historyItem.getEventType() == RequestHistoryType.SCALED) {
        Assert.assertEquals(280, historyItem.getMessage().get().length());
      } else if (historyItem.getEventType() == RequestHistoryType.DELETING) {
        Assert.assertEquals("a msg", historyItem.getMessage().get());
      } else {
        Assert.assertTrue(!historyItem.getMessage().isPresent());
      }
    }
  }
}
