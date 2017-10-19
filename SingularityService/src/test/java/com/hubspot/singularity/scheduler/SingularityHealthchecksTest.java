package com.hubspot.singularity.scheduler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.v1.Protos.TaskState;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hubspot.deploy.HealthcheckOptions;
import com.hubspot.deploy.HealthcheckOptionsBuilder;
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
import com.jayway.awaitility.Awaitility;

public class SingularityHealthchecksTest extends SingularitySchedulerTestBase {

  public SingularityHealthchecksTest() {
    super(false);
  }

  @Test
  public void testSkipHealthchecksEdgeCases() {
    try {
      setConfigurationForNoDelay();
      configuration.setKillAfterTasksDoNotRunDefaultSeconds(100);
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
    } finally {
      unsetConfigurationForNoDelay();
      configuration.setCheckNewTasksEverySeconds(5);
    }
  }

  @Test
  public void testSkipHealthchecksDuringBounce() {
    try {
      initRequest();
      initHCDeploy();

      SingularityTask firstTask = startTask(firstDeploy, 1);

      requestResource.bounce(requestId, Optional.of(new SingularityBounceRequest(Optional.<Boolean> absent(), Optional.of(true), Optional.<Long> absent(), Optional.<String> absent(), Optional.<String>absent(), Optional.<SingularityShellCommand>absent())));

      setConfigurationForNoDelay();

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
    } finally {
      unsetConfigurationForNoDelay();
    }
  }

  @Test
  public void testHealthchecksDuringBounce() {
    initRequest();
    initHCDeploy();

    startTask(firstDeploy);

    requestResource.bounce(requestId, Optional.absent());

    cleaner.drainCleanupQueue();

    SingularityTask secondTask = startTask(firstDeploy);

    cleaner.drainCleanupQueue();

    Assert.assertEquals(1, taskManager.getNumCleanupTasks());
    Assert.assertEquals(2, taskManager.getNumActiveTasks());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(503), Optional.of(1000L), 1, Optional.<String> absent(), Optional.<String> absent(), secondTask.getTaskId(), Optional.<Boolean>absent()));

    cleaner.drainCleanupQueue();

    Assert.assertEquals(1, taskManager.getNumCleanupTasks());
    Assert.assertEquals(2, taskManager.getNumActiveTasks());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(200), Optional.of(1000L), System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent(), secondTask.getTaskId(), Optional.<Boolean>absent()));

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

    HealthcheckOptions options =  new HealthcheckOptionsBuilder("http://uri").setMaxRetries(Optional.of(2)).build();

    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId).setHealthcheck(Optional.of(options));
    db.setDeployHealthTimeoutSeconds(Optional.of(TimeUnit.DAYS.toMillis(1)));

    SingularityDeploy deploy = initDeploy(db, hourAgo);

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    SingularityTask task = launchTask(request, deploy, hourAgo, hourAgo + 1, 1, TaskState.TASK_RUNNING);

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(503), Optional.of(1000L), hourAgo + 1, Optional.<String> absent(), Optional.<String> absent(), task.getTaskId(), Optional.<Boolean>absent()));

    deployChecker.checkDeploys();

    Assert.assertEquals(DeployState.FAILED, deployManager.getDeployResult(requestId, deployId).get().getDeployState());
  }

  @Test
  public void testMaxHealthcheckRetries() {
    initRequest();

    final String deployId = "retry_test";

    HealthcheckOptions options =  new HealthcheckOptionsBuilder("http://uri").setMaxRetries(Optional.of(2)).build();
    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId).setHealthcheck(Optional.of(options));

    SingularityDeploy deploy = initDeploy(db, System.currentTimeMillis());

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    SingularityTask task = launchTask(request, deploy, System.currentTimeMillis(), 1, TaskState.TASK_RUNNING);

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(503), Optional.of(1000L), System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent(), task.getTaskId(), Optional.<Boolean>absent()));
    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(503), Optional.of(1000L), System.currentTimeMillis() + 1, Optional.<String> absent(), Optional.<String> absent(), task.getTaskId(), Optional.<Boolean>absent()));

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(503), Optional.of(1000L), System.currentTimeMillis() + 1, Optional.<String> absent(), Optional.<String> absent(), task.getTaskId(), Optional.<Boolean>absent()));

    deployChecker.checkDeploys();

    Assert.assertEquals(DeployState.FAILED, deployManager.getDeployResult(requestId, deployId).get().getDeployState());
  }

  @Test
  public void testNewTaskCheckerRespectsDeployHealthcheckRetries() {
    initRequest();

    final String deployId = "new_task_healthcheck";

    HealthcheckOptions options =  new HealthcheckOptionsBuilder("http://uri").setMaxRetries(Optional.of(1)).build();
    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId).setHealthcheck(Optional.of(options));

    SingularityDeploy deploy = initAndFinishDeploy(request, db, Optional.absent());

    SingularityTask task = launchTask(request, deploy, System.currentTimeMillis(), 1, TaskState.TASK_RUNNING);

    Assert.assertEquals(CheckTaskState.CHECK_IF_HEALTHCHECK_OVERDUE, newTaskChecker.getTaskState(task, requestManager.getRequest(requestId), healthchecker));
    Assert.assertTrue(taskManager.getCleanupTaskIds().isEmpty());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(503), Optional.of(1000L), System.currentTimeMillis() + 1, Optional.<String> absent(), Optional.<String> absent(), task.getTaskId(), Optional.<Boolean>absent()));

    Assert.assertEquals(CheckTaskState.CHECK_IF_HEALTHCHECK_OVERDUE, newTaskChecker.getTaskState(task, requestManager.getRequest(requestId), healthchecker));

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(503), Optional.of(1000L), System.currentTimeMillis() + 1, Optional.<String> absent(), Optional.<String> absent(), task.getTaskId(), Optional.<Boolean>absent()));

    Assert.assertEquals(CheckTaskState.UNHEALTHY_KILL_TASK, newTaskChecker.getTaskState(task, requestManager.getRequest(requestId), healthchecker));
  }

  @Test
  public void testHealthchecksSuccess() {
    initRequest();

    final String deployId = "hc_test";

    HealthcheckOptions options =  new HealthcheckOptionsBuilder("http://uri").setMaxRetries(Optional.of(2)).build();

    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId).setHealthcheck(Optional.of(options));
    SingularityDeploy deploy = initDeploy(db, System.currentTimeMillis());

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    SingularityTask task = launchTask(request, deploy, System.currentTimeMillis(), 1, TaskState.TASK_RUNNING);

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(503), Optional.of(1000L), System.currentTimeMillis(), Optional.<String>absent(), Optional.<String>absent(), task.getTaskId(), Optional.<Boolean>absent()));

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(200), Optional.of(1000L), System.currentTimeMillis() + 1, Optional.<String>absent(), Optional.<String>absent(), task.getTaskId(), Optional.<Boolean>absent()));

    deployChecker.checkDeploys();

    Assert.assertEquals(DeployState.SUCCEEDED, deployManager.getDeployResult(requestId, deployId).get().getDeployState());
  }

  @Test
  public void testFailingStatusCodes() {
    initRequest();

    final String deployId = "retry_test";

    List<Integer> failureCodes = ImmutableList.of(404);
    HealthcheckOptions options =  new HealthcheckOptionsBuilder("http://uri").setMaxRetries(Optional.of(3)).setFailureStatusCodes(Optional.of(failureCodes)).build();
    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId).setHealthcheck(Optional.of(options));

    SingularityDeploy deploy = initDeploy(db, System.currentTimeMillis());

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    SingularityTask task = launchTask(request, deploy, System.currentTimeMillis(), 1, TaskState.TASK_RUNNING);

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(503), Optional.of(1000L), System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent(), task.getTaskId(), Optional.<Boolean>absent()));

    deployChecker.checkDeploys();

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(404), Optional.of(1000L), System.currentTimeMillis() + 1, Optional.<String> absent(), Optional.<String> absent(), task.getTaskId(), Optional.<Boolean>absent()));

    deployChecker.checkDeploys();

    // Bad status code should cause instant failure even though retries remain
    Assert.assertEquals(DeployState.FAILED, deployManager.getDeployResult(requestId, deployId).get().getDeployState());
  }

  @Test
  public void testStartupTimeout() {
    initRequest();

    final long hourAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
    final String deployId = "startup_timeout_test";

    HealthcheckOptions options =  new HealthcheckOptionsBuilder("http://uri").setMaxRetries(Optional.of(2)).setStartupTimeoutSeconds(Optional.of((int) TimeUnit.MINUTES.toSeconds(30))).build();
    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId).setHealthcheck(Optional.of(options));
    db.setDeployHealthTimeoutSeconds(Optional.of(TimeUnit.DAYS.toMillis(1)));
    SingularityDeploy deploy = initDeploy(db, hourAgo);

    deployChecker.checkDeploys();
    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    SingularityTask task = launchTask(request, deploy, hourAgo, hourAgo + 1, 1, TaskState.TASK_RUNNING);
    deployChecker.checkDeploys();
    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.<Integer>absent(), Optional.of(1000L), hourAgo + 1, Optional.<String> absent(), Optional.of("ERROR"), task.getTaskId(), Optional.of(true)));
    deployChecker.checkDeploys();
    Assert.assertEquals(DeployState.FAILED, deployManager.getDeployResult(requestId, deployId).get().getDeployState());
  }

  @Test
  public void testStartupDoesNotCountTowardsRetries() {
    initRequest();

    final String deployId = "retry_test";

    HealthcheckOptions options =  new HealthcheckOptionsBuilder("http://uri").setMaxRetries(Optional.of(1)).build();
    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId).setHealthcheck(Optional.of(options));
    SingularityDeploy deploy = initDeploy(db, System.currentTimeMillis());
    deployChecker.checkDeploys();
    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    SingularityTask task = launchTask(request, deploy, System.currentTimeMillis(), 1, TaskState.TASK_RUNNING);
    deployChecker.checkDeploys();
    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.<Integer>absent(), Optional.of(1000L), System.currentTimeMillis(), Optional.<String> absent(), Optional.of("ConnectionRefused"), task.getTaskId(), Optional.of(true)));
    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.<Integer>absent(), Optional.of(1000L), System.currentTimeMillis() + 1, Optional.<String> absent(), Optional.of("ConnectionRefused"), task.getTaskId(), Optional.of(true)));
    deployChecker.checkDeploys();
    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(503), Optional.of(1000L), System.currentTimeMillis() + 1, Optional.<String> absent(), Optional.<String> absent(), task.getTaskId(), Optional.<Boolean>absent()));
    deployChecker.checkDeploys();
    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(200), Optional.of(1000L), System.currentTimeMillis() + 1, Optional.<String> absent(), Optional.<String> absent(), task.getTaskId(), Optional.<Boolean>absent()));
    deployChecker.checkDeploys();
    Assert.assertEquals(DeployState.SUCCEEDED, deployManager.getDeployResult(requestId, deployId).get().getDeployState());
  }

  @Test
  public void testPortIndices() {
    try {
      setConfigurationForNoDelay();
      initRequest();
      HealthcheckOptions options = new HealthcheckOptionsBuilder("http://uri").setPortIndex(Optional.of(1)).setStartupDelaySeconds(Optional.of(0)).build();
      firstDeploy = initAndFinishDeploy(request, new SingularityDeployBuilder(request.getId(), firstDeployId).setCommand(Optional.of("sleep 100"))
          .setHealthcheck(Optional.of(options)), Optional.of(new Resources(1, 64, 3, 0)));

      requestResource.postRequest(request.toBuilder().setInstances(Optional.of(2)).build());
      scheduler.drainPendingQueue();

      String[] portRange = {"80:82"};
      sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String> absent(), Collections.<String, String>emptyMap(), portRange)));

      SingularityTaskId firstTaskId = taskManager.getActiveTaskIdsForRequest(requestId).get(0);

      SingularityTask firstTask = taskManager.getTask(firstTaskId).get();
      statusUpdate(firstTask, TaskState.TASK_RUNNING);

      newTaskChecker.enqueueNewTaskCheck(firstTask, requestManager.getRequest(requestId), healthchecker);

      Awaitility.await("healthcheck present").atMost(5, TimeUnit.SECONDS).until(() -> taskManager.getLastHealthcheck(firstTask.getTaskId()).isPresent());

      Assert.assertTrue(taskManager.getLastHealthcheck(firstTask.getTaskId()).get().toString().contains("host1:81"));
    } finally {
      unsetConfigurationForNoDelay();
    }
  }

  @Test
  public void testPortNumber() {
    try {
      setConfigurationForNoDelay();
      initRequest();
      HealthcheckOptions options = new HealthcheckOptionsBuilder("http://uri").setPortNumber(Optional.of(81L)).setStartupDelaySeconds(Optional.of(0)).build();
      firstDeploy = initAndFinishDeploy(request, new SingularityDeployBuilder(request.getId(), firstDeployId)
        .setCommand(Optional.of("sleep 100")).setResources(Optional.of(new Resources(1, 64, 3, 0)))
        .setHealthcheck(Optional.of(options)), Optional.absent());

      requestResource.postRequest(request.toBuilder().setInstances(Optional.of(2)).build());
      scheduler.drainPendingQueue();

      String[] portRange = {"80:82"};
      sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String> absent(), Collections.<String, String> emptyMap(), portRange)));

      SingularityTaskId firstTaskId = taskManager.getActiveTaskIdsForRequest(requestId).get(0);

      SingularityTask firstTask = taskManager.getTask(firstTaskId).get();
      statusUpdate(firstTask, TaskState.TASK_RUNNING);

      newTaskChecker.enqueueNewTaskCheck(firstTask, requestManager.getRequest(requestId), healthchecker);

      Awaitility.await("healthcheck present").atMost(5, TimeUnit.SECONDS).until(() -> taskManager.getLastHealthcheck(firstTask.getTaskId()).isPresent());

      Assert.assertTrue(taskManager.getLastHealthcheck(firstTask.getTaskId()).get().toString().contains("host1:81"));
    } finally {
      unsetConfigurationForNoDelay();
    }
  }

  private void setConfigurationForNoDelay() {
    configuration.setNewTaskCheckerBaseDelaySeconds(0);
    configuration.setHealthcheckIntervalSeconds(0);
    configuration.setDeployHealthyBySeconds(0);
    configuration.setKillAfterTasksDoNotRunDefaultSeconds(1);
    configuration.setHealthcheckMaxRetries(Optional.of(0));
  }

  private void unsetConfigurationForNoDelay() {
    configuration.setNewTaskCheckerBaseDelaySeconds(1);
    configuration.setHealthcheckIntervalSeconds(5);
    configuration.setDeployHealthyBySeconds(120);
    configuration.setKillAfterTasksDoNotRunDefaultSeconds(600);
    configuration.setHealthcheckMaxRetries(Optional.<Integer>absent());
  }
}
