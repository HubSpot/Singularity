package com.hubspot.singularity.scheduler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.Protos.TaskState;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityShellCommand;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.api.SingularitySkipHealthchecksRequest;
import com.hubspot.singularity.scheduler.SingularityNewTaskChecker.CheckTaskState;

public class SingularityHealthchecksTest extends SingularitySchedulerTestBase {

  public SingularityHealthchecksTest() {
    super(false);
  }

  @Test
  public void testSkipHealthchecksEdgeCases() {
    configuration.setNewTaskCheckerBaseDelaySeconds(0);
    configuration.setHealthcheckIntervalSeconds(0);
    configuration.setDeployHealthyBySeconds(0);
    configuration.setKillAfterTasksDoNotRunDefaultSeconds(100);
    configuration.setHealthcheckMaxRetries(Optional.of(0));
    configuration.setCheckNewTasksEverySeconds(1);

    initRequest();
    initHCDeploy();

    requestResource.skipHealthchecks(requestId, new SingularitySkipHealthchecksRequest(Optional.of(Boolean.TRUE), Optional.<Long> absent(), Optional.<String> absent(), Optional.<String> absent()));

    SingularityTask firstTask = startTask(firstDeploy, 1);

    Assert.assertTrue(!taskManager.getLastHealthcheck(firstTask.getTaskId()).isPresent());

    finishHealthchecks();
    finishNewTaskChecksAndCleanup();

    Assert.assertEquals(1, taskManager.getNumActiveTasks());

    requestResource.skipHealthchecks(requestId, new SingularitySkipHealthchecksRequest(Optional.of(Boolean.FALSE), Optional.<Long> absent(), Optional.<String> absent(), Optional.<String> absent()));

    // run new task check ONLY.
    newTaskChecker.enqueueNewTaskCheck(firstTask, requestManager.getRequest(requestId), healthchecker);

    finishNewTaskChecks();
    finishHealthchecks();
    finishNewTaskChecksAndCleanup();

    // healthcheck will fail
    Assert.assertTrue(taskManager.getLastHealthcheck(firstTask.getTaskId()).isPresent());
    Assert.assertEquals(0, taskManager.getNumActiveTasks());
  }

  @Test
  public void testSkipHealthchecksDuringBounce() {
    initRequest();
    initHCDeploy();

    SingularityTask firstTask = startTask(firstDeploy, 1);

    requestResource.bounce(requestId, Optional.of(new SingularityBounceRequest(Optional.<Boolean> absent(), Optional.of(true), Optional.<Long> absent(), Optional.<String> absent(), Optional.<String>absent(), Optional.<SingularityShellCommand>absent())));

    configuration.setNewTaskCheckerBaseDelaySeconds(0);
    configuration.setHealthcheckIntervalSeconds(0);
    configuration.setDeployHealthyBySeconds(0);
    configuration.setKillAfterTasksDoNotRunDefaultSeconds(1);
    configuration.setHealthcheckMaxRetries(Optional.of(0));

    cleaner.drainCleanupQueue();
    resourceOffers();

    List<SingularityTaskId> taskIds = taskManager.getAllTaskIds();
    taskIds.remove(firstTask.getTaskId());

    SingularityTaskId secondTaskId = taskIds.get(0);

    SingularityTask secondTask = taskManager.getTask(secondTaskId).get();

    statusUpdate(secondTask, TaskState.TASK_RUNNING);

    Assert.assertTrue(healthchecker.cancelHealthcheck(firstTask.getTaskId().getId()));

    newTaskChecker.cancelNewTaskCheck(firstTask.getTaskId().getId());

    finishHealthchecks();
    finishNewTaskChecks();

    Assert.assertTrue(!taskManager.getLastHealthcheck(secondTask.getTaskId()).isPresent());

    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assert.assertEquals(0, taskManager.getNumCleanupTasks());
    Assert.assertEquals(1, taskManager.getNumActiveTasks());
  }

  @Test
  public void testHealthchecksDuringBounce() {
    initRequest();
    initHCDeploy();

    startTask(firstDeploy);

    requestResource.bounce(requestId);

    cleaner.drainCleanupQueue();

    SingularityTask secondTask = startTask(firstDeploy);

    cleaner.drainCleanupQueue();

    Assert.assertEquals(1, taskManager.getNumCleanupTasks());
    Assert.assertEquals(2, taskManager.getNumActiveTasks());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(500), Optional.of(1000L), 1, Optional.<String> absent(), Optional.<String> absent(), secondTask.getTaskId()));

    cleaner.drainCleanupQueue();

    Assert.assertEquals(1, taskManager.getNumCleanupTasks());
    Assert.assertEquals(2, taskManager.getNumActiveTasks());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(200), Optional.of(1000L), System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent(), secondTask.getTaskId()));

    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assert.assertEquals(0, taskManager.getNumCleanupTasks());
    Assert.assertEquals(1, taskManager.getNumActiveTasks());
  }

  @Test
  public void testHealthchecksTimeout() {
    initRequest();

    final long hourAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);

    final String deployId = "timeout_test";

    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId);
    db.setHealthcheckMaxTotalTimeoutSeconds(Optional.of(30L));
    db.setHealthcheckUri(Optional.of("http://uri"));
    db.setDeployHealthTimeoutSeconds(Optional.of(TimeUnit.DAYS.toMillis(1)));

    SingularityDeploy deploy = initDeploy(db, hourAgo);

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    SingularityTask task = launchTask(request, deploy, hourAgo, hourAgo + 1, 1, TaskState.TASK_RUNNING);

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(500), Optional.of(1000L), hourAgo + 1, Optional.<String> absent(), Optional.<String> absent(), task.getTaskId()));

    deployChecker.checkDeploys();

    Assert.assertEquals(DeployState.FAILED, deployManager.getDeployResult(requestId, deployId).get().getDeployState());
  }

  @Test
  public void testMaxHealthcheckRetries() {
    initRequest();

    final String deployId = "retry_test";

    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId);
    db.setHealthcheckMaxRetries(Optional.of(2));
    db.setHealthcheckUri(Optional.of("http://uri"));

    SingularityDeploy deploy = initDeploy(db, System.currentTimeMillis());

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    SingularityTask task = launchTask(request, deploy, System.currentTimeMillis(), 1, TaskState.TASK_RUNNING);

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(500), Optional.of(1000L), System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent(), task.getTaskId()));
    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(500), Optional.of(1000L), System.currentTimeMillis() + 1, Optional.<String> absent(), Optional.<String> absent(), task.getTaskId()));

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(500), Optional.of(1000L), System.currentTimeMillis() + 1, Optional.<String> absent(), Optional.<String> absent(), task.getTaskId()));

    deployChecker.checkDeploys();

    Assert.assertEquals(DeployState.FAILED, deployManager.getDeployResult(requestId, deployId).get().getDeployState());
  }

  @Test
  public void testNewTaskCheckerRespectsDeployHealthcheckRetries() {
    initRequest();

    final String deployId = "new_task_healthcheck";

    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId);
    db.setHealthcheckMaxRetries(Optional.of(1));
    db.setHealthcheckUri(Optional.of("http://uri"));

    SingularityDeploy deploy = initAndFinishDeploy(request, db);

    SingularityTask task = launchTask(request, deploy, System.currentTimeMillis(), 1, TaskState.TASK_RUNNING);

    Assert.assertEquals(CheckTaskState.CHECK_IF_OVERDUE, newTaskChecker.getTaskState(task, requestManager.getRequest(requestId), healthchecker));
    Assert.assertTrue(taskManager.getCleanupTaskIds().isEmpty());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(500), Optional.of(1000L), System.currentTimeMillis() + 1, Optional.<String> absent(), Optional.<String> absent(), task.getTaskId()));

    Assert.assertEquals(CheckTaskState.CHECK_IF_OVERDUE, newTaskChecker.getTaskState(task, requestManager.getRequest(requestId), healthchecker));

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(500), Optional.of(1000L), System.currentTimeMillis() + 1, Optional.<String> absent(), Optional.<String> absent(), task.getTaskId()));

    Assert.assertEquals(CheckTaskState.UNHEALTHY_KILL_TASK, newTaskChecker.getTaskState(task, requestManager.getRequest(requestId), healthchecker));
  }

  @Test
  public void testHealthchecksSuccess() {
    initRequest();

    final String deployId = "hc_test";

    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId);
    db.setHealthcheckMaxRetries(Optional.of(2));
    db.setHealthcheckMaxTotalTimeoutSeconds(Optional.of(30L));
    db.setHealthcheckUri(Optional.of("http://uri"));

    SingularityDeploy deploy = initDeploy(db, System.currentTimeMillis());

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    SingularityTask task = launchTask(request, deploy, System.currentTimeMillis(), 1, TaskState.TASK_RUNNING);

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(500), Optional.of(1000L), System.currentTimeMillis(), Optional.<String>absent(), Optional.<String>absent(), task.getTaskId()));

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(200), Optional.of(1000L), System.currentTimeMillis() + 1, Optional.<String>absent(), Optional.<String>absent(), task.getTaskId()));

    deployChecker.checkDeploys();

    Assert.assertEquals(DeployState.SUCCEEDED, deployManager.getDeployResult(requestId, deployId).get().getDeployState());
  }

  @Test
  public void testPortIndices() {
    configuration.setNewTaskCheckerBaseDelaySeconds(0);
    configuration.setHealthcheckIntervalSeconds(0);
    configuration.setDeployHealthyBySeconds(0);
    configuration.setKillAfterTasksDoNotRunDefaultSeconds(1);
    configuration.setHealthcheckMaxRetries(Optional.of(0));

    initRequest();
    firstDeploy = initAndFinishDeploy(request, new SingularityDeployBuilder(request.getId(), firstDeployId)
      .setCommand(Optional.of("sleep 100"))
      .setHealthcheckUri(Optional.of("http://uri"))
      .setResources(Optional.of(new Resources(1, 64, 3, 0)))
      .setHealthcheckPortIndex(Optional.of(1)));

    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(2)).build());
    scheduler.drainPendingQueue(stateCacheProvider.get());

    String[] portRange = {"80:82"};
    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String> absent(), Collections.<String, String>emptyMap(), portRange)));

    SingularityTaskId firstTaskId = taskManager.getActiveTaskIdsForRequest(requestId).get(0);

    SingularityTask firstTask = taskManager.getTask(firstTaskId).get();
    statusUpdate(firstTask, TaskState.TASK_RUNNING);

    newTaskChecker.enqueueNewTaskCheck(firstTask, requestManager.getRequest(requestId), healthchecker);

    finishNewTaskChecks();
    finishHealthchecks();
    finishNewTaskChecksAndCleanup();

    Assert.assertTrue(taskManager.getLastHealthcheck(firstTask.getTaskId()).get().toString().contains("host1:81"));
  }

}
