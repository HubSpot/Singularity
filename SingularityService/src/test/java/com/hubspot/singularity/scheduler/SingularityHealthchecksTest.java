package com.hubspot.singularity.scheduler;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.hubspot.deploy.HealthcheckOptions;
import com.hubspot.deploy.HealthcheckOptionsBuilder;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.api.SingularitySkipHealthchecksRequest;
import com.hubspot.singularity.scheduler.SingularityNewTaskChecker.CheckTaskState;
import com.jayway.awaitility.Awaitility;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.mesos.v1.Protos.TaskState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class SingularityHealthchecksTest extends SingularitySchedulerTestBase {
  @Inject
  SingularityDeployHealthHelper deployHealthHelper;

  public SingularityHealthchecksTest() {
    super(false);
  }

  @Test
  public void testSkipHealthchecksEdgeCases() throws Exception {
    try {
      setConfigurationForNoDelay();
      configuration.setKillAfterTasksDoNotRunDefaultSeconds(100);
      configuration.setCheckNewTasksEverySeconds(1);

      initRequest();
      initHCDeploy();

      requestResource.skipHealthchecks(
        requestId,
        new SingularitySkipHealthchecksRequest(
          Optional.of(Boolean.TRUE),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        ),
        singularityUser
      );

      SingularityTask firstTask = startTask(firstDeploy, 1);

      Assertions.assertTrue(
        !taskManager.getLastHealthcheck(firstTask.getTaskId()).isPresent()
      );

      finishHealthchecks();
      finishNewTaskChecksAndCleanup();

      Assertions.assertEquals(1, taskManager.getNumActiveTasks());

      requestResource.skipHealthchecks(
        requestId,
        new SingularitySkipHealthchecksRequest(
          Optional.of(Boolean.FALSE),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        ),
        singularityUser
      );

      // run new task check ONLY.
      newTaskChecker.enqueueNewTaskCheck(
        firstTask,
        requestManager.getRequest(requestId),
        healthchecker
      );
      newTaskChecker.getTaskCheck(firstTask.getTaskId()).get();

      finishHealthchecks();
      finishNewTaskChecksAndCleanup();

      newTaskChecker.enqueueNewTaskCheck(
        firstTask,
        requestManager.getRequest(requestId),
        healthchecker
      );
      newTaskChecker.getTaskCheck(firstTask.getTaskId()).get();
      finishNewTaskChecksAndCleanup();
      finishHealthchecks();
      // healthcheck will fail
      Assertions.assertTrue(
        taskManager.getLastHealthcheck(firstTask.getTaskId()).isPresent()
      );
      Assertions.assertEquals(0, taskManager.getNumActiveTasks());
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

      requestResource.bounce(
        requestId,
        Optional.of(
          new SingularityBounceRequest(
            Optional.empty(),
            Optional.of(true),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
          )
        ),
        singularityUser
      );

      setConfigurationForNoDelay();

      cleaner.drainCleanupQueue();
      scheduler.drainPendingQueue();
      resourceOffers();

      List<SingularityTaskId> taskIds = taskManager.getAllTaskIds();
      taskIds.remove(firstTask.getTaskId());

      SingularityTaskId secondTaskId = taskIds.get(0);

      SingularityTask secondTask = taskManager.getTask(secondTaskId).get();

      statusUpdate(secondTask, TaskState.TASK_RUNNING);

      Assertions.assertTrue(
        healthchecker.cancelHealthcheck(firstTask.getTaskId().getId())
      );

      newTaskChecker.cancelNewTaskCheck(firstTask.getTaskId().getId());

      finishHealthchecks();

      Assertions.assertTrue(
        !taskManager.getLastHealthcheck(secondTask.getTaskId()).isPresent()
      );

      cleaner.drainCleanupQueue();
      killKilledTasks();

      Assertions.assertEquals(0, taskManager.getNumCleanupTasks());
      Assertions.assertEquals(1, taskManager.getNumActiveTasks());
    } finally {
      unsetConfigurationForNoDelay();
    }
  }

  @Test
  public void testHealthchecksDuringBounce() {
    initRequest();
    initHCDeploy();

    startTask(firstDeploy);

    requestResource.bounce(requestId, Optional.empty(), singularityUser);

    cleaner.drainCleanupQueue();

    SingularityTask secondTask = startTask(firstDeploy);

    cleaner.drainCleanupQueue();

    Assertions.assertEquals(1, taskManager.getNumCleanupTasks());
    Assertions.assertEquals(2, taskManager.getNumActiveTasks());

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(503),
        Optional.of(1000L),
        1,
        Optional.<String>empty(),
        Optional.<String>empty(),
        secondTask.getTaskId(),
        Optional.<Boolean>empty()
      )
    );

    cleaner.drainCleanupQueue();

    Assertions.assertEquals(1, taskManager.getNumCleanupTasks());
    Assertions.assertEquals(2, taskManager.getNumActiveTasks());

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(200),
        Optional.of(1000L),
        System.currentTimeMillis(),
        Optional.<String>empty(),
        Optional.<String>empty(),
        secondTask.getTaskId(),
        Optional.<Boolean>empty()
      )
    );

    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assertions.assertEquals(0, taskManager.getNumCleanupTasks());
    Assertions.assertEquals(1, taskManager.getNumActiveTasks());
  }

  @Test
  public void testHealthchecksTimeout() {
    initRequest();

    final long hourAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);

    final String deployId = "timeout_test";

    HealthcheckOptions options = new HealthcheckOptionsBuilder("http://uri")
      .setMaxRetries(Optional.of(2))
      .build();

    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId)
    .setHealthcheck(Optional.of(options));
    db.setDeployHealthTimeoutSeconds(Optional.of(TimeUnit.DAYS.toMillis(1)));

    SingularityDeploy deploy = initDeploy(db, hourAgo);

    deployChecker.checkDeploys();

    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    SingularityTask task = launchTask(
      request,
      deploy,
      hourAgo,
      hourAgo + 1,
      1,
      TaskState.TASK_RUNNING
    );

    deployChecker.checkDeploys();

    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(503),
        Optional.of(1000L),
        hourAgo + 1,
        Optional.<String>empty(),
        Optional.<String>empty(),
        task.getTaskId(),
        Optional.<Boolean>empty()
      )
    );

    deployChecker.checkDeploys();

    Assertions.assertEquals(
      DeployState.FAILED,
      deployManager.getDeployResult(requestId, deployId).get().getDeployState()
    );
  }

  @Test
  public void testMaxHealthcheckRetries() {
    initRequest();

    final String deployId = "retry_test";

    HealthcheckOptions options = new HealthcheckOptionsBuilder("http://uri")
      .setMaxRetries(Optional.of(2))
      .build();
    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId)
    .setHealthcheck(Optional.of(options));

    SingularityDeploy deploy = initDeploy(db, System.currentTimeMillis());

    deployChecker.checkDeploys();

    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    SingularityTask task = launchTask(
      request,
      deploy,
      System.currentTimeMillis(),
      1,
      TaskState.TASK_RUNNING
    );

    deployChecker.checkDeploys();

    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(503),
        Optional.of(1000L),
        System.currentTimeMillis(),
        Optional.<String>empty(),
        Optional.<String>empty(),
        task.getTaskId(),
        Optional.<Boolean>empty()
      )
    );
    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(503),
        Optional.of(1000L),
        System.currentTimeMillis() + 1,
        Optional.<String>empty(),
        Optional.<String>empty(),
        task.getTaskId(),
        Optional.<Boolean>empty()
      )
    );

    deployChecker.checkDeploys();

    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(503),
        Optional.of(1000L),
        System.currentTimeMillis() + 1,
        Optional.<String>empty(),
        Optional.<String>empty(),
        task.getTaskId(),
        Optional.<Boolean>empty()
      )
    );

    deployChecker.checkDeploys();

    Assertions.assertEquals(
      DeployState.FAILED,
      deployManager.getDeployResult(requestId, deployId).get().getDeployState()
    );
  }

  @Test
  public void testNewTaskCheckerRespectsDeployHealthcheckRetries() {
    initRequest();

    final String deployId = "new_task_healthcheck";

    HealthcheckOptions options = new HealthcheckOptionsBuilder("http://uri")
      .setMaxRetries(Optional.of(1))
      .build();
    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId)
    .setHealthcheck(Optional.of(options));

    SingularityDeploy deploy = initAndFinishDeploy(request, db, Optional.empty());

    SingularityTask task = launchTask(
      request,
      deploy,
      System.currentTimeMillis(),
      1,
      TaskState.TASK_RUNNING
    );

    Assertions.assertEquals(
      CheckTaskState.CHECK_IF_HEALTHCHECK_OVERDUE,
      newTaskChecker.getTaskState(
        task,
        requestManager.getRequest(requestId),
        healthchecker
      )
    );
    Assertions.assertTrue(taskManager.getCleanupTaskIds().isEmpty());

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(503),
        Optional.of(1000L),
        System.currentTimeMillis() + 1,
        Optional.<String>empty(),
        Optional.<String>empty(),
        task.getTaskId(),
        Optional.<Boolean>empty()
      )
    );

    Assertions.assertEquals(
      CheckTaskState.CHECK_IF_HEALTHCHECK_OVERDUE,
      newTaskChecker.getTaskState(
        task,
        requestManager.getRequest(requestId),
        healthchecker
      )
    );

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(503),
        Optional.of(1000L),
        System.currentTimeMillis() + 1,
        Optional.<String>empty(),
        Optional.<String>empty(),
        task.getTaskId(),
        Optional.<Boolean>empty()
      )
    );

    Assertions.assertEquals(
      CheckTaskState.UNHEALTHY_KILL_TASK,
      newTaskChecker.getTaskState(
        task,
        requestManager.getRequest(requestId),
        healthchecker
      )
    );
  }

  @Test
  public void testHealthchecksSuccess() {
    initRequest();

    final String deployId = "hc_test";

    HealthcheckOptions options = new HealthcheckOptionsBuilder("http://uri")
      .setMaxRetries(Optional.of(2))
      .build();

    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId)
    .setHealthcheck(Optional.of(options));
    SingularityDeploy deploy = initDeploy(db, System.currentTimeMillis());

    deployChecker.checkDeploys();

    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    SingularityTask task = launchTask(
      request,
      deploy,
      System.currentTimeMillis(),
      1,
      TaskState.TASK_RUNNING
    );

    deployChecker.checkDeploys();

    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(503),
        Optional.of(1000L),
        System.currentTimeMillis(),
        Optional.<String>empty(),
        Optional.<String>empty(),
        task.getTaskId(),
        Optional.<Boolean>empty()
      )
    );

    deployChecker.checkDeploys();

    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(200),
        Optional.of(1000L),
        System.currentTimeMillis() + 1,
        Optional.<String>empty(),
        Optional.<String>empty(),
        task.getTaskId(),
        Optional.<Boolean>empty()
      )
    );

    deployChecker.checkDeploys();

    Assertions.assertEquals(
      DeployState.SUCCEEDED,
      deployManager.getDeployResult(requestId, deployId).get().getDeployState()
    );
  }

  @Test
  public void testFailingStatusCodes() {
    initRequest();

    final String deployId = "retry_test";

    List<Integer> failureCodes = ImmutableList.of(404);
    HealthcheckOptions options = new HealthcheckOptionsBuilder("http://uri")
      .setMaxRetries(Optional.of(3))
      .setFailureStatusCodes(Optional.of(failureCodes))
      .build();
    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId)
    .setHealthcheck(Optional.of(options));

    SingularityDeploy deploy = initDeploy(db, System.currentTimeMillis());

    deployChecker.checkDeploys();

    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    SingularityTask task = launchTask(
      request,
      deploy,
      System.currentTimeMillis(),
      1,
      TaskState.TASK_RUNNING
    );

    deployChecker.checkDeploys();

    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(503),
        Optional.of(1000L),
        System.currentTimeMillis(),
        Optional.<String>empty(),
        Optional.<String>empty(),
        task.getTaskId(),
        Optional.<Boolean>empty()
      )
    );

    deployChecker.checkDeploys();

    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(404),
        Optional.of(1000L),
        System.currentTimeMillis() + 1,
        Optional.<String>empty(),
        Optional.<String>empty(),
        task.getTaskId(),
        Optional.<Boolean>empty()
      )
    );

    deployChecker.checkDeploys();

    // Bad status code should cause instant failure even though retries remain
    Assertions.assertEquals(
      DeployState.FAILED,
      deployManager.getDeployResult(requestId, deployId).get().getDeployState()
    );
  }

  @Test
  public void testStartupTimeout() {
    initRequest();

    final long hourAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
    final String deployId = "startup_timeout_test";

    HealthcheckOptions options = new HealthcheckOptionsBuilder("http://uri")
      .setMaxRetries(Optional.of(2))
      .setStartupTimeoutSeconds(Optional.of((int) TimeUnit.MINUTES.toSeconds(30)))
      .build();
    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId)
    .setHealthcheck(Optional.of(options));
    db.setDeployHealthTimeoutSeconds(Optional.of(TimeUnit.DAYS.toMillis(1)));
    SingularityDeploy deploy = initDeploy(db, hourAgo);

    deployChecker.checkDeploys();
    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    SingularityTask task = launchTask(
      request,
      deploy,
      hourAgo,
      hourAgo + 1,
      1,
      TaskState.TASK_RUNNING
    );
    deployChecker.checkDeploys();
    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.<Integer>empty(),
        Optional.of(1000L),
        hourAgo + 1,
        Optional.<String>empty(),
        Optional.of("ERROR"),
        task.getTaskId(),
        Optional.of(true)
      )
    );
    deployChecker.checkDeploys();
    Assertions.assertEquals(
      DeployState.FAILED,
      deployManager.getDeployResult(requestId, deployId).get().getDeployState()
    );
  }

  @Test
  public void testStartupDoesNotCountTowardsRetries() {
    initRequest();

    final String deployId = "retry_test";

    HealthcheckOptions options = new HealthcheckOptionsBuilder("http://uri")
      .setMaxRetries(Optional.of(1))
      .build();
    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId)
    .setHealthcheck(Optional.of(options));
    SingularityDeploy deploy = initDeploy(db, System.currentTimeMillis());
    deployChecker.checkDeploys();
    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    SingularityTask task = launchTask(
      request,
      deploy,
      System.currentTimeMillis(),
      1,
      TaskState.TASK_RUNNING
    );
    deployChecker.checkDeploys();
    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.<Integer>empty(),
        Optional.of(1000L),
        System.currentTimeMillis(),
        Optional.<String>empty(),
        Optional.of("ConnectionRefused"),
        task.getTaskId(),
        Optional.of(true)
      )
    );
    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.<Integer>empty(),
        Optional.of(1000L),
        System.currentTimeMillis() + 1,
        Optional.<String>empty(),
        Optional.of("ConnectionRefused"),
        task.getTaskId(),
        Optional.of(true)
      )
    );
    deployChecker.checkDeploys();
    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(503),
        Optional.of(1000L),
        System.currentTimeMillis() + 1,
        Optional.<String>empty(),
        Optional.<String>empty(),
        task.getTaskId(),
        Optional.<Boolean>empty()
      )
    );
    deployChecker.checkDeploys();
    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(200),
        Optional.of(1000L),
        System.currentTimeMillis() + 1,
        Optional.<String>empty(),
        Optional.<String>empty(),
        task.getTaskId(),
        Optional.<Boolean>empty()
      )
    );
    deployChecker.checkDeploys();
    Assertions.assertEquals(
      DeployState.SUCCEEDED,
      deployManager.getDeployResult(requestId, deployId).get().getDeployState()
    );
  }

  @Test
  public void testPortIndices() {
    try {
      setConfigurationForNoDelay();
      initRequest();
      HealthcheckOptions options = new HealthcheckOptionsBuilder("/uri")
        .setPortIndex(Optional.of(1))
        .setStartupDelaySeconds(Optional.of(0))
        .build();
      firstDeploy =
        initAndFinishDeploy(
          request,
          new SingularityDeployBuilder(request.getId(), firstDeployId)
            .setCommand(Optional.of("sleep 100"))
            .setHealthcheck(Optional.of(options)),
          Optional.of(new Resources(1, 64, 3, 0))
        );

      requestResource.postRequest(
        request.toBuilder().setInstances(Optional.of(2)).build(),
        singularityUser
      );
      scheduler.drainPendingQueue();

      String[] portRange = { "80:82" };
      sms
        .resourceOffers(
          Arrays.asList(
            createOffer(
              20,
              20000,
              50000,
              "slave1",
              "host1",
              Optional.<String>empty(),
              Collections.<String, String>emptyMap(),
              portRange
            )
          )
        )
        .join();

      SingularityTaskId firstTaskId = taskManager
        .getActiveTaskIdsForRequest(requestId)
        .get(0);

      SingularityTask firstTask = taskManager.getTask(firstTaskId).get();
      statusUpdate(firstTask, TaskState.TASK_RUNNING);

      healthchecker.asyncHealthcheck(firstTask);

      Awaitility
        .await("healthcheck present")
        .atMost(6, TimeUnit.SECONDS)
        .until(() -> taskManager.getLastHealthcheck(firstTask.getTaskId()).isPresent());

      Assertions.assertTrue(
        taskManager
          .getLastHealthcheck(firstTask.getTaskId())
          .get()
          .toString()
          .contains("host1:81")
      );
    } finally {
      unsetConfigurationForNoDelay();
    }
  }

  @Test
  public void testPortNumber() {
    try {
      setConfigurationForNoDelay();
      initRequest();
      HealthcheckOptions options = new HealthcheckOptionsBuilder("http://uri")
        .setPortNumber(Optional.of(81L))
        .setStartupDelaySeconds(Optional.of(0))
        .build();
      firstDeploy =
        initAndFinishDeploy(
          request,
          new SingularityDeployBuilder(request.getId(), firstDeployId)
            .setCommand(Optional.of("sleep 100"))
            .setResources(Optional.of(new Resources(1, 64, 3, 0)))
            .setHealthcheck(Optional.of(options)),
          Optional.empty()
        );

      requestResource.postRequest(
        request.toBuilder().setInstances(Optional.of(2)).build(),
        singularityUser
      );
      scheduler.drainPendingQueue();

      String[] portRange = { "80:82" };
      sms
        .resourceOffers(
          Arrays.asList(
            createOffer(
              20,
              20000,
              50000,
              "slave1",
              "host1",
              Optional.<String>empty(),
              Collections.<String, String>emptyMap(),
              portRange
            )
          )
        )
        .join();

      SingularityTaskId firstTaskId = taskManager
        .getActiveTaskIdsForRequest(requestId)
        .get(0);

      SingularityTask firstTask = taskManager.getTask(firstTaskId).get();
      statusUpdate(firstTask, TaskState.TASK_RUNNING);

      healthchecker.asyncHealthcheck(firstTask);

      Awaitility
        .await("healthcheck present")
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> taskManager.getLastHealthcheck(firstTask.getTaskId()).isPresent());

      Assertions.assertTrue(
        taskManager
          .getLastHealthcheck(firstTask.getTaskId())
          .get()
          .toString()
          .contains("host1:81")
      );
    } finally {
      unsetConfigurationForNoDelay();
    }
  }

  @Test
  public void testRespectsSkipHealthchecksAfterExpiration() {
    initRequest();
    initHCDeploy();
    scheduler.drainPendingQueue();
    startTask(firstDeploy);
    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals(
      0,
      deployHealthHelper
        .getHealthyTasks(
          request,
          Optional.of(firstDeploy),
          taskManager.getActiveTaskIds(),
          false
        )
        .size()
    );
    SingularityTaskId taskId = taskManager.getActiveTaskIds().get(0);
    Assertions.assertFalse(taskManager.getLastHealthcheck(taskId).isPresent());

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
    Assertions.assertTrue(taskManager.getLastHealthcheck(taskId).isPresent());
    Assertions.assertEquals(
      1,
      deployHealthHelper
        .getHealthyTasks(
          request,
          Optional.of(firstDeploy),
          taskManager.getActiveTaskIds(),
          false
        )
        .size()
    );
    expiringUserActionPoller.runActionOnPoll();
    Assertions.assertEquals(
      1,
      deployHealthHelper
        .getHealthyTasks(
          request,
          Optional.of(firstDeploy),
          taskManager.getActiveTaskIds(),
          false
        )
        .size()
    );
  }

  private void setConfigurationForNoDelay() {
    configuration.setNewTaskCheckerBaseDelaySeconds(0);
    configuration.setHealthcheckIntervalSeconds(0);
    configuration.setStartupIntervalSeconds(0);
    configuration.setDeployHealthyBySeconds(0);
    configuration.setKillAfterTasksDoNotRunDefaultSeconds(1);
    configuration.setHealthcheckMaxRetries(Optional.of(0));
  }

  private void unsetConfigurationForNoDelay() {
    configuration.setNewTaskCheckerBaseDelaySeconds(1);
    configuration.setHealthcheckIntervalSeconds(5);
    configuration.setStartupIntervalSeconds(2);
    configuration.setDeployHealthyBySeconds(120);
    configuration.setKillAfterTasksDoNotRunDefaultSeconds(600);
    configuration.setHealthcheckMaxRetries(Optional.<Integer>empty());
  }
}
