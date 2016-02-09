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
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.api.SingularityDeleteRequestRequest;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.config.HistoryPurgingConfiguration;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.HistoryManager.OrderDirection;
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
  protected SingularityTaskHistoryPersister taskHistoryPersister;

  @Inject
  protected SingularityRequestHistoryPersister requestHistoryPersister;

  @Inject
  protected SingularityTestAuthenticator testAuthenticator;

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

    SingularityTaskHistory taskHistory = new SingularityTaskHistory(null, Optional.<String> absent(), null, task, null, null, null);

    return taskHistory;
  }

  private void saveTasks(int num, long launchTime) {
    for (int i = 0; i < num; i++) {
      SingularityTaskHistory taskHistory = buildTask(launchTime + i);

      historyManager.saveTaskHistory(taskHistory);
    }
  }


  @Test
  public void historyUpdaterTest() {
    initRequest();
    initFirstDeploy();

    HistoryPurgingConfiguration historyPurgingConfiguration = new HistoryPurgingConfiguration();
    historyPurgingConfiguration.setEnabled(true);
    historyPurgingConfiguration.setDeleteTaskHistoryBytesInsteadOfEntireRow(false);
    historyPurgingConfiguration.setDeleteTaskHistoryAfterDays(1);

    SingularityTaskHistory taskHistory = buildTask(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3));

    historyManager.saveTaskHistory(taskHistory);

    Assert.assertTrue(historyManager.getTaskHistory(taskHistory.getTask().getTaskId().getId()).get().getTask() != null);

    Assert.assertEquals(1, historyManager.getTaskHistoryForRequest(requestId, 0, 100).size());

    SingularityHistoryPurger purger = new SingularityHistoryPurger(historyPurgingConfiguration, historyManager);

    purger.runActionOnPoll();

    Assert.assertEquals(1, historyManager.getTaskHistoryForRequest(requestId, 0, 100).size());

    Assert.assertTrue(!historyManager.getTaskHistory(taskHistory.getTask().getTaskId().getId()).isPresent());
  }

  @Test
  public void historyPurgerTest() {
    initRequest();
    initFirstDeploy();

    saveTasks(3, System.currentTimeMillis());

    Assert.assertEquals(3, historyManager.getTaskHistoryForRequest(requestId, 0, 10).size());

    HistoryPurgingConfiguration historyPurgingConfiguration = new HistoryPurgingConfiguration();
    historyPurgingConfiguration.setEnabled(true);
    historyPurgingConfiguration.setDeleteTaskHistoryBytesInsteadOfEntireRow(true);
    historyPurgingConfiguration.setDeleteTaskHistoryAfterDays(10);

    SingularityHistoryPurger purger = new SingularityHistoryPurger(historyPurgingConfiguration, historyManager);

    purger.runActionOnPoll();

    Assert.assertEquals(3, historyManager.getTaskHistoryForRequest(requestId, 0, 10).size());

    historyPurgingConfiguration.setDeleteTaskHistoryAfterTasksPerRequest(1);

    purger.runActionOnPoll();

    Assert.assertEquals(1, historyManager.getTaskHistoryForRequest(requestId, 0, 10).size());

    historyPurgingConfiguration.setDeleteTaskHistoryAfterTasksPerRequest(25);
    historyPurgingConfiguration.setDeleteTaskHistoryAfterDays(100);

    purger.runActionOnPoll();

    Assert.assertEquals(1, historyManager.getTaskHistoryForRequest(requestId, 0, 10).size());

    saveTasks(100, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(200));

    purger.runActionOnPoll();

    Assert.assertEquals(1, historyManager.getTaskHistoryForRequest(requestId, 0, 10).size());
  }

  @Test
  public void testRunId() {
    initScheduledRequest();
    initFirstDeploy();

    String runId = "my-run-id";

    SingularityPendingRequestParent parent = requestResource.scheduleImmediately(requestId,
        Optional.of(new SingularityRunNowRequest(Optional.<String> absent(), Optional.<Boolean> absent(), Optional.of(runId), Optional.<List<String>> absent())));

    Assert.assertEquals(runId, parent.getPendingRequest().getRunId().get());

    resourceOffers();

    Assert.assertEquals(runId, taskManager.getActiveTasks().get(0).getTaskRequest().getPendingTask().getRunId().get());

    SingularityTaskId taskId = taskManager.getActiveTaskIds().get(0);

    statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_FINISHED);

    configuration.setTaskPersistAfterStartupBufferMillis(0);
    taskMetadataConfiguration.setTaskPersistAfterFinishBufferMillis(0);

    taskHistoryPersister.runActionOnPoll();

    Assert.assertEquals(runId, historyManager.getTaskHistory(taskId.getId()).get().getTask().getTaskRequest().getPendingTask().getRunId().get());
    Assert.assertEquals(runId, historyManager.getTaskHistoryForRequest(requestId, 0, 10).get(0).getRunId().get());

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
    final TaskHistoryHelper taskHistoryHelperWithMockedTaskManager = new TaskHistoryHelper(taskManagerSpy, historyManager);

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
    Assert.assertEquals(1, taskHistoryHelperWithMockedTaskManager.getBlendedHistory(requestId, 0, 5).size());
  }

  @Test
  public void testMessage() {
    initRequest();

    String msg = null;
    for (int i = 0; i < 300; i++) {
      msg = msg + i;
    }

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(2), Optional.<Long> absent(), Optional.<Boolean> absent(), Optional.<String> absent(), Optional.of(msg)));
    requestResource.deleteRequest(requestId, Optional.of(new SingularityDeleteRequestRequest(Optional.of("a msg"), Optional.<String> absent())));

    cleaner.drainCleanupQueue();

    requestHistoryPersister.runActionOnPoll();

    List<SingularityRequestHistory> history = historyManager.getRequestHistory(requestId, Optional.of(OrderDirection.DESC), 0, 100);

    Assert.assertEquals(3, history.size());

    for (SingularityRequestHistory historyItem : history) {
      if (historyItem.getEventType() == RequestHistoryType.DELETED) {
        Assert.assertEquals("a msg", historyItem.getMessage().get());
      } else if (historyItem.getEventType() == RequestHistoryType.SCALED) {
        Assert.assertEquals(280, historyItem.getMessage().get().length());
      } else {
        Assert.assertTrue(!historyItem.getMessage().isPresent());
      }
    }

  }


}
