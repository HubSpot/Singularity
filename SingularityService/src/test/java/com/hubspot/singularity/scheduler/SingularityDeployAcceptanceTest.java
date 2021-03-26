package com.hubspot.singularity.scheduler;

import com.google.inject.Inject;
import com.hubspot.singularity.CanaryDeploySettings;
import com.hubspot.singularity.DeployAcceptanceMode;
import com.hubspot.singularity.DeployAcceptanceResult;
import com.hubspot.singularity.DeployAcceptanceState;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityDeployProgress;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.api.SingularityDeployRequest;
import com.hubspot.singularity.hooks.DeployAcceptanceHook;
import java.util.Optional;
import java.util.Set;
import org.apache.mesos.v1.Protos.TaskState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SingularityDeployAcceptanceTest extends SingularitySchedulerTestBase {
  @Inject
  Set<DeployAcceptanceHook> acceptanceHooks;

  public SingularityDeployAcceptanceTest() {
    super(false);
  }

  @Test
  public void testDeployWithFailingAcceptanceConditions() {
    NoopDeployAcceptanceHook hook = (NoopDeployAcceptanceHook) acceptanceHooks
      .iterator()
      .next();
    hook.setNextResult(
      new DeployAcceptanceResult(DeployAcceptanceState.PENDING, "waiting")
    );
    initRequest();

    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();

    requestResource.postRequest(
      request.toBuilder().setInstances(Optional.of(2)).build(),
      singularityUser
    );

    initFirstDeploy();

    SingularityTask firstTask = launchTask(
      request,
      firstDeploy,
      1,
      TaskState.TASK_RUNNING
    );
    SingularityTask secondTask = launchTask(
      request,
      firstDeploy,
      2,
      TaskState.TASK_RUNNING
    );

    SingularityDeployBuilder builder = new SingularityDeployBuilder(
      requestId,
      secondDeployId
    );
    builder
      .setCommand(Optional.of("sleep 1"))
      .setCanaryDeploySettings(
        CanaryDeploySettings
          .newbuilder()
          .setInstanceGroupSize(1)
          .setEnableCanaryDeploy(true)
          .setAcceptanceMode(DeployAcceptanceMode.CHECKS)
          .setInstanceGroupSize(1)
          .build()
      );
    deployResource.deploy(
      new SingularityDeployRequest(builder.build(), Optional.empty(), Optional.empty()),
      singularityUser
    );

    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();
    Assertions.assertEquals(1, taskManager.getPendingTaskIds().size());

    resourceOffers();
    Assertions.assertEquals(
      1,
      taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size()
    );

    SingularityTaskId firstNewTaskId = taskManager
      .getActiveTaskIdsForDeploy(requestId, secondDeployId)
      .get(0);
    statusUpdate(taskManager.getTask(firstNewTaskId).get(), TaskState.TASK_RUNNING);
    deployChecker.checkDeploys();
    deployChecker.checkDeploys();

    SingularityDeployProgress deployProgressStepOne = deployManager
      .getPendingDeploys()
      .get(0)
      .getDeployProgress();
    Assertions.assertEquals(1, deployProgressStepOne.getTargetActiveInstances());
    Assertions.assertEquals(
      "(no-op - PENDING) waiting",
      deployProgressStepOne.getAcceptanceResultMessageHistory().iterator().next()
    );
    Assertions.assertEquals(
      DeployAcceptanceState.PENDING,
      deployProgressStepOne.getStepAcceptanceResults().get(hook.getName())
    );

    hook.setNextResult(
      new DeployAcceptanceResult(DeployAcceptanceState.FAILED, ":sadpanda:")
    );

    deployChecker.checkDeploys();

    SingularityDeployResult deployResult = deployManager
      .getDeployResult(requestId, secondDeployId)
      .get();
    Assertions.assertEquals(DeployState.FAILED, deployResult.getDeployState());
    Assertions.assertTrue(deployResult.getMessage().orElse("").contains(":sadpanda:"));
  }

  @Test
  public void testDeployWithAcceptanceConditions() {
    NoopDeployAcceptanceHook hook = (NoopDeployAcceptanceHook) acceptanceHooks
      .iterator()
      .next();
    hook.setNextResult(
      new DeployAcceptanceResult(DeployAcceptanceState.PENDING, "waiting")
    );
    initRequest();

    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();

    requestResource.postRequest(
      request.toBuilder().setInstances(Optional.of(2)).build(),
      singularityUser
    );

    initFirstDeploy();

    SingularityTask firstTask = launchTask(
      request,
      firstDeploy,
      1,
      TaskState.TASK_RUNNING
    );
    SingularityTask secondTask = launchTask(
      request,
      firstDeploy,
      2,
      TaskState.TASK_RUNNING
    );

    SingularityDeployBuilder builder = new SingularityDeployBuilder(
      requestId,
      secondDeployId
    );
    builder
      .setCommand(Optional.of("sleep 1"))
      .setCanaryDeploySettings(
        CanaryDeploySettings
          .newbuilder()
          .setInstanceGroupSize(1)
          .setEnableCanaryDeploy(true)
          .setAcceptanceMode(DeployAcceptanceMode.CHECKS)
          .setInstanceGroupSize(1)
          .build()
      );
    deployResource.deploy(
      new SingularityDeployRequest(builder.build(), Optional.empty(), Optional.empty()),
      singularityUser
    );

    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();
    Assertions.assertEquals(1, taskManager.getPendingTaskIds().size());

    resourceOffers();
    Assertions.assertEquals(
      1,
      taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size()
    );

    SingularityTaskId firstNewTaskId = taskManager
      .getActiveTaskIdsForDeploy(requestId, secondDeployId)
      .get(0);
    statusUpdate(taskManager.getTask(firstNewTaskId).get(), TaskState.TASK_RUNNING);
    deployChecker.checkDeploys();
    deployChecker.checkDeploys();

    SingularityDeployProgress deployProgressStepOne = deployManager
      .getPendingDeploys()
      .get(0)
      .getDeployProgress();
    Assertions.assertEquals(1, deployProgressStepOne.getTargetActiveInstances());
    Assertions.assertEquals(
      "(no-op - PENDING) waiting",
      deployProgressStepOne.getAcceptanceResultMessageHistory().iterator().next()
    );
    Assertions.assertEquals(
      DeployAcceptanceState.PENDING,
      deployProgressStepOne.getStepAcceptanceResults().get(hook.getName())
    );

    hook.setNextResult(
      new DeployAcceptanceResult(DeployAcceptanceState.SUCCEEDED, "YAY")
    );

    deployChecker.checkDeploys();
    deployProgressStepOne = deployManager.getPendingDeploys().get(0).getDeployProgress();
    Assertions.assertEquals(2, deployProgressStepOne.getTargetActiveInstances());
    Assertions.assertTrue(
      deployProgressStepOne
        .getAcceptanceResultMessageHistory()
        .contains("(no-op - SUCCEEDED) YAY")
    );

    cleaner.drainCleanupQueue();
    statusUpdate(firstTask, TaskState.TASK_KILLED);

    deployChecker.checkDeploys();

    SingularityDeployProgress deployProgressStepTwo = deployManager
      .getPendingDeploys()
      .get(0)
      .getDeployProgress();
    Assertions.assertFalse(deployProgressStepTwo.isStepLaunchComplete());
    Assertions.assertEquals(2, deployProgressStepTwo.getTargetActiveInstances());

    scheduler.drainPendingQueue();
    Assertions.assertEquals(1, taskManager.getPendingTaskIds().size());

    resourceOffers();
    Assertions.assertEquals(
      2,
      taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size()
    );

    for (SingularityTaskId taskId : taskManager.getActiveTaskIdsForDeploy(
      requestId,
      secondDeployId
    )) {
      statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_RUNNING);
    }
    deployChecker.checkDeploys();
    deployChecker.checkDeploys();

    Assertions.assertEquals(
      2,
      taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size()
    );
    Assertions.assertEquals(
      DeployState.SUCCEEDED,
      deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState()
    );
  }
}
