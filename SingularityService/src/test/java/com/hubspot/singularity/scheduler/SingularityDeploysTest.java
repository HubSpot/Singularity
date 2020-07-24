package com.hubspot.singularity.scheduler;

import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployProgress;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskBuilder;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRunNowRequestBuilder;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityUpdatePendingDeployRequest;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.api.SingularityDeployRequest;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.WebApplicationException;
import org.apache.mesos.v1.Protos.TaskState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SingularityDeploysTest extends SingularitySchedulerTestBase {

  public SingularityDeploysTest() {
    super(false);
  }

  @Test
  @Order(-1) // TODO - investigate why this succeeds in isolation but fails when run in sequence with other tests
  public void testDeployManagerHandlesFailedLBTask() {
    initLoadBalancedRequest();
    initFirstDeploy();

    SingularityTask firstTask = startTask(firstDeploy);

    initSecondDeploy();

    SingularityTask secondTask = startTask(secondDeploy);

    // this should cause an LB call to happen:
    deployChecker.checkDeploys();

    Assertions.assertTrue(
      taskManager
        .getLoadBalancerState(secondTask.getTaskId(), LoadBalancerRequestType.ADD)
        .isPresent()
    );
    Assertions.assertTrue(
      !taskManager
        .getLoadBalancerState(secondTask.getTaskId(), LoadBalancerRequestType.DEPLOY)
        .isPresent()
    );
    Assertions.assertTrue(
      !taskManager
        .getLoadBalancerState(secondTask.getTaskId(), LoadBalancerRequestType.REMOVE)
        .isPresent()
    );

    statusUpdate(secondTask, TaskState.TASK_FAILED);
    statusUpdate(firstTask, TaskState.TASK_FAILED);
    scheduler.drainPendingQueue();

    deployChecker.checkDeploys();

    Assertions.assertTrue(
      deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState() ==
      DeployState.FAILED
    );

    List<SingularityPendingTask> pendingTasks = taskManager.getPendingTasks();

    Assertions.assertTrue(pendingTasks.size() == 1);
    Assertions.assertTrue(
      pendingTasks.get(0).getPendingTaskId().getDeployId().equals(firstDeployId)
    );
  }

  @Test
  public void testOnDemandGetsRescheduledWithNewDeploy() {
    initOnDemandRequest();
    initFirstDeploy();

    SingularityRunNowRequest runNowRequest = new SingularityRunNowRequest(
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Collections.emptyList(),
      Optional.empty(),
      Collections.emptyMap(),
      Collections.emptyMap(),
      Collections.emptyMap(),
      Collections.emptyList(),
      Optional.of(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5))
    );

    requestResource.scheduleImmediately(singularityUser, requestId, runNowRequest);
    scheduler.drainPendingQueue();
    Assertions.assertEquals(
      runNowRequest.getRunAt().get(),
      taskManager.getPendingTaskIds().get(0).getNextRunAt()
    );
    Assertions.assertEquals(
      firstDeployId,
      taskManager.getPendingTaskIds().get(0).getDeployId()
    );

    initSecondDeploy();
    deployChecker.checkDeploys();
    Assertions.assertEquals(
      secondDeployId,
      deployManager.getActiveDeployId(requestId).get()
    );
    scheduler.drainPendingQueue();
    Assertions.assertEquals(
      runNowRequest.getRunAt().get(),
      taskManager.getPendingTaskIds().get(0).getNextRunAt()
    );
    Assertions.assertEquals(
      secondDeployId,
      taskManager.getPendingTaskIds().get(0).getDeployId()
    );
  }

  @Test
  public void testMissingDeployIsEventuallyReconciled() {
    try {
      initRequest();
      deploy(firstDeployId);

      Assertions.assertTrue(deployManager.getPendingDeploy(requestId).isPresent());
      // mimics persister bug or odd concurrency issue
      deployManager.deleteDeployHistory(
        new SingularityDeployKey(requestId, firstDeployId)
      );
      configuration.setDeployHealthyBySeconds(0);
      deployChecker.checkDeploys();
      Assertions.assertEquals(
        deployManager.getDeployResult(requestId, firstDeployId).get().getDeployState(),
        DeployState.OVERDUE
      );
    } finally {
      configuration.setDeployHealthyBySeconds(120);
    }
  }

  @Test
  public void testDeployClearsObsoleteScheduledTasks() {
    initRequest();
    initFirstDeploy();
    initSecondDeploy();

    SingularityPendingTaskId taskIdOne = new SingularityPendingTaskId(
      requestId,
      firstDeployId,
      System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3),
      1,
      PendingType.IMMEDIATE,
      System.currentTimeMillis()
    );
    SingularityPendingTask taskOne = new SingularityPendingTaskBuilder()
      .setPendingTaskId(taskIdOne)
      .build();

    SingularityPendingTaskId taskIdTwo = new SingularityPendingTaskId(
      requestId,
      firstDeployId,
      System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1),
      2,
      PendingType.IMMEDIATE,
      System.currentTimeMillis()
    );
    SingularityPendingTask taskTwo = new SingularityPendingTaskBuilder()
      .setPendingTaskId(taskIdTwo)
      .build();

    SingularityPendingTaskId taskIdThree = new SingularityPendingTaskId(
      requestId,
      secondDeployId,
      System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3),
      1,
      PendingType.IMMEDIATE,
      System.currentTimeMillis()
    );
    SingularityPendingTask taskThree = new SingularityPendingTaskBuilder()
      .setPendingTaskId(taskIdThree)
      .build();

    SingularityPendingTaskId taskIdFour = new SingularityPendingTaskId(
      requestId + "hi",
      firstDeployId,
      System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3),
      5,
      PendingType.IMMEDIATE,
      System.currentTimeMillis()
    );
    SingularityPendingTask taskFour = new SingularityPendingTaskBuilder()
      .setPendingTaskId(taskIdFour)
      .build();

    taskManager.savePendingTask(taskOne);
    taskManager.savePendingTask(taskTwo);
    taskManager.savePendingTask(taskThree);
    taskManager.savePendingTask(taskFour);

    launchTask(request, secondDeploy, 1, TaskState.TASK_RUNNING);

    deployChecker.checkDeploys();

    List<SingularityPendingTaskId> pendingTaskIds = taskManager.getPendingTaskIds();

    Assertions.assertTrue(!pendingTaskIds.contains(taskIdOne));
    Assertions.assertTrue(!pendingTaskIds.contains(taskIdTwo));

    Assertions.assertTrue(pendingTaskIds.contains(taskIdThree));
    Assertions.assertTrue(pendingTaskIds.contains(taskIdFour));
  }

  @Test
  public void testDeployAllInstancesAtOnce() {
    initRequest();

    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();

    requestResource.postRequest(
      request.toBuilder().setInstances(Optional.of(2)).build(),
      singularityUser
    );

    initFirstDeploy();

    launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);
    launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);

    deploy(secondDeployId);
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();

    Assertions.assertEquals(2, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals(2, taskManager.getPendingTaskIds().size());

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
    Assertions.assertEquals(2, taskManager.getCleanupTaskIds().size());
    Assertions.assertEquals(
      DeployState.SUCCEEDED,
      deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState()
    );
    Assertions.assertEquals(
      2,
      taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size()
    );
  }

  @Test
  public void testDeployOneInstanceAtATime() {
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

    deploy(
      secondDeployId,
      Optional.<Boolean>empty(),
      Optional.of(1),
      Optional.<Boolean>empty(),
      false
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

    Assertions.assertEquals(1, taskManager.getCleanupTaskIds().size());
    Assertions.assertTrue(
      taskManager.getCleanupTaskIds().contains(firstTask.getTaskId())
    );
    SingularityDeployProgress deployProgressStepOne = deployManager
      .getPendingDeploys()
      .get(0)
      .getDeployProgress()
      .get();
    Assertions.assertTrue(deployProgressStepOne.isStepComplete());
    Assertions.assertEquals(1, deployProgressStepOne.getTargetActiveInstances());

    cleaner.drainCleanupQueue();
    statusUpdate(firstTask, TaskState.TASK_KILLED);

    deployChecker.checkDeploys();

    SingularityDeployProgress deployProgressStepTwo = deployManager
      .getPendingDeploys()
      .get(0)
      .getDeployProgress()
      .get();
    Assertions.assertFalse(deployProgressStepTwo.isStepComplete());
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

    Assertions.assertEquals(
      2,
      taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size()
    );
    Assertions.assertEquals(
      DeployState.SUCCEEDED,
      deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState()
    );
  }

  @Test
  public void testIncrementalDeployCancel() {
    initRequest();

    // Set up incremental deploy that is partly finished
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
    launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);
    deploy(
      secondDeployId,
      Optional.<Boolean>empty(),
      Optional.of(1),
      Optional.<Boolean>empty(),
      false
    );
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();
    resourceOffers();
    SingularityTaskId firstNewTaskId = taskManager
      .getActiveTaskIdsForDeploy(requestId, secondDeployId)
      .get(0);
    statusUpdate(taskManager.getTask(firstNewTaskId).get(), TaskState.TASK_RUNNING);
    deployChecker.checkDeploys();

    cleaner.drainCleanupQueue();
    statusUpdate(firstTask, TaskState.TASK_KILLED);
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();
    resourceOffers();
    // End in-progress incremental deploy setup

    deployResource.cancelDeploy(singularityUser, requestId, secondDeployId);
    deployChecker.checkDeploys();
    Assertions.assertEquals(
      taskManager.getCleanupTasks().get(0).getCleanupType(),
      TaskCleanupType.INCREMENTAL_DEPLOY_CANCELLED
    );

    // Incremental deploy task should not be shut down while active deploy is below target instances
    cleaner.drainCleanupQueue();
    Assertions.assertTrue(taskManager.getKilledTaskIdRecords().isEmpty());
    Assertions.assertEquals(
      taskManager.getCleanupTasks().get(0).getCleanupType(),
      TaskCleanupType.INCREMENTAL_DEPLOY_CANCELLED
    );

    launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);
    cleaner.drainCleanupQueue();
    Assertions.assertFalse(taskManager.getKilledTaskIdRecords().isEmpty());
  }

  @Test
  public void testScaleDownDuringDeploy() {
    initRequest();

    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();

    requestResource.postRequest(
      request.toBuilder().setInstances(Optional.of(2)).build(),
      singularityUser
    );

    initFirstDeploy();

    launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);
    launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);

    deploy(secondDeployId);
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();
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

    requestResource.postRequest(
      request.toBuilder().setInstances(Optional.of(1)).build(),
      singularityUser
    );
    scheduler.drainPendingQueue();

    Assertions.assertEquals(1, taskManager.getCleanupTaskIds().size());

    deployChecker.checkDeploys();
    Assertions.assertEquals(2, taskManager.getCleanupTaskIds().size());

    // Extra task from the new deploy should get cleaned up as well
    scheduler.drainPendingQueue();
    Assertions.assertEquals(3, taskManager.getCleanupTaskIds().size());
  }

  @Test
  public void testDeployWithManualStep() {
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

    deploy(
      secondDeployId,
      Optional.<Boolean>empty(),
      Optional.of(1),
      Optional.of(false),
      false
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

    Assertions.assertEquals(1, taskManager.getCleanupTaskIds().size());
    Assertions.assertTrue(
      taskManager.getCleanupTaskIds().contains(firstTask.getTaskId())
    );
    SingularityDeployProgress deployProgressStepOne = deployManager
      .getPendingDeploys()
      .get(0)
      .getDeployProgress()
      .get();
    Assertions.assertTrue(deployProgressStepOne.isStepComplete());
    Assertions.assertEquals(1, deployProgressStepOne.getTargetActiveInstances());

    cleaner.drainCleanupQueue();
    statusUpdate(firstTask, TaskState.TASK_KILLED);

    deployChecker.checkDeploys();

    // Deploy should not have moved to next step even though instances are launched
    deployProgressStepOne =
      deployManager.getPendingDeploys().get(0).getDeployProgress().get();
    Assertions.assertTrue(deployProgressStepOne.isStepComplete());
    Assertions.assertEquals(1, deployProgressStepOne.getTargetActiveInstances());

    // Add the 'ok' to move to the next step
    deployResource.updatePendingDeploy(
      singularityUser,
      new SingularityUpdatePendingDeployRequest(requestId, secondDeployId, 2)
    );

    deployChecker.checkDeploys();

    SingularityDeployProgress deployProgressStepTwo = deployManager
      .getPendingDeploys()
      .get(0)
      .getDeployProgress()
      .get();
    Assertions.assertFalse(deployProgressStepTwo.isStepComplete());
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

    Assertions.assertEquals(
      2,
      taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size()
    );
    Assertions.assertEquals(
      DeployState.SUCCEEDED,
      deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState()
    );
  }

  @Test
  public void testDeployMultipleInstancesAtOnce() {
    initRequest();

    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();

    requestResource.postRequest(
      request.toBuilder().setInstances(Optional.of(4)).build(),
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
    SingularityTask thirdTask = launchTask(
      request,
      firstDeploy,
      3,
      TaskState.TASK_RUNNING
    );
    SingularityTask fourthTask = launchTask(
      request,
      firstDeploy,
      4,
      TaskState.TASK_RUNNING
    );

    deploy(
      secondDeployId,
      Optional.<Boolean>empty(),
      Optional.of(2),
      Optional.<Boolean>empty(),
      false
    );
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();
    Assertions.assertEquals(2, taskManager.getPendingTaskIds().size());

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

    Assertions.assertEquals(2, taskManager.getCleanupTaskIds().size());
    List<SingularityTaskId> cleanupTaskIds = taskManager.getCleanupTaskIds();
    Assertions.assertTrue(
      cleanupTaskIds.contains(firstTask.getTaskId()) &&
      cleanupTaskIds.contains(secondTask.getTaskId())
    );
    SingularityDeployProgress deployProgressStepOne = deployManager
      .getPendingDeploys()
      .get(0)
      .getDeployProgress()
      .get();
    Assertions.assertTrue(deployProgressStepOne.isStepComplete());
    Assertions.assertEquals(2, deployProgressStepOne.getTargetActiveInstances());

    cleaner.drainCleanupQueue();
    statusUpdate(firstTask, TaskState.TASK_KILLED);
    statusUpdate(secondTask, TaskState.TASK_KILLED);

    deployChecker.checkDeploys();

    SingularityDeployProgress deployProgressStepTwo = deployManager
      .getPendingDeploys()
      .get(0)
      .getDeployProgress()
      .get();
    Assertions.assertFalse(deployProgressStepTwo.isStepComplete());
    Assertions.assertEquals(4, deployProgressStepTwo.getTargetActiveInstances());

    scheduler.drainPendingQueue();
    Assertions.assertEquals(2, taskManager.getPendingTaskIds().size());

    resourceOffers();
    Assertions.assertEquals(
      4,
      taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size()
    );

    for (SingularityTaskId taskId : taskManager.getActiveTaskIdsForDeploy(
      requestId,
      secondDeployId
    )) {
      statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_RUNNING);
    }
    deployChecker.checkDeploys();

    Assertions.assertEquals(
      4,
      taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size()
    );
    Assertions.assertEquals(
      DeployState.SUCCEEDED,
      deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState()
    );
  }

  @Test
  public void testCancelDeploy() {
    initRequest();

    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();

    initFirstDeploy();

    SingularityTask firstTask = launchTask(
      request,
      firstDeploy,
      1,
      TaskState.TASK_RUNNING
    );

    deploy(
      secondDeployId,
      Optional.<Boolean>empty(),
      Optional.of(1),
      Optional.of(false),
      false
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
    deployResource.cancelDeploy(singularityUser, requestId, secondDeployId);

    deployChecker.checkDeploys();

    Assertions.assertTrue(taskManager.getCleanupTaskIds().contains(firstNewTaskId));
    Assertions.assertFalse(
      taskManager.getCleanupTaskIds().contains(firstTask.getTaskId())
    );
    Assertions.assertEquals(
      DeployState.CANCELED,
      deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState()
    );
  }

  @Test
  public void testDeployFails() {
    initRequest();

    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();

    initFirstDeploy();

    SingularityTask firstTask = launchTask(
      request,
      firstDeploy,
      1,
      TaskState.TASK_RUNNING
    );

    deploy(
      secondDeployId,
      Optional.<Boolean>empty(),
      Optional.of(1),
      Optional.of(false),
      false
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
    statusUpdate(taskManager.getTask(firstNewTaskId).get(), TaskState.TASK_FAILED);

    deployChecker.checkDeploys();

    Assertions.assertFalse(
      taskManager.getCleanupTaskIds().contains(firstTask.getTaskId())
    );
    Assertions.assertEquals(
      DeployState.FAILED,
      deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState()
    );
  }

  @Test
  public void testDeployFailsForInvalidRequestState() {
    initRequest();

    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();

    initFirstDeploy();

    deploy(
      secondDeployId,
      Optional.<Boolean>empty(),
      Optional.of(1),
      Optional.of(false),
      false
    );
    requestManager.pause(
      request,
      System.currentTimeMillis(),
      Optional.<String>empty(),
      Optional.<String>empty()
    );
    deployChecker.checkDeploys();

    Assertions.assertEquals(
      DeployState.FAILED,
      deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState()
    );
  }

  @Test
  public void testDeployFailsAfterMaxTaskRetries() {
    initRequest();

    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, firstDeployId);
    db.setMaxTaskRetries(Optional.of(1));
    SingularityDeploy deploy = initDeploy(db, System.currentTimeMillis());

    deployChecker.checkDeploys();
    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, firstDeployId).isPresent()
    );

    SingularityTask task = launchTask(
      request,
      deploy,
      System.currentTimeMillis(),
      1,
      TaskState.TASK_FAILED
    );

    deployChecker.checkDeploys();
    Assertions.assertEquals(
      deployManager.getPendingDeploys().get(0).getCurrentDeployState(),
      DeployState.WAITING
    );

    SingularityTask taskTryTwo = launchTask(
      request,
      deploy,
      System.currentTimeMillis(),
      1,
      TaskState.TASK_FAILED
    );

    deployChecker.checkDeploys();
    Assertions.assertEquals(
      deployManager.getDeployResult(requestId, firstDeployId).get().getDeployState(),
      DeployState.FAILED
    );
  }

  @Test
  public void testDeploySucceedsWithTaskRetries() {
    initRequest();

    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, firstDeployId);
    db.setMaxTaskRetries(Optional.of(1));
    SingularityDeploy deploy = initDeploy(db, System.currentTimeMillis());

    deployChecker.checkDeploys();
    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, firstDeployId).isPresent()
    );

    SingularityTask task = launchTask(
      request,
      deploy,
      System.currentTimeMillis(),
      1,
      TaskState.TASK_FAILED
    );

    deployChecker.checkDeploys();
    Assertions.assertEquals(
      deployManager.getPendingDeploys().get(0).getCurrentDeployState(),
      DeployState.WAITING
    );

    SingularityTask taskTryTwo = launchTask(
      request,
      deploy,
      System.currentTimeMillis(),
      1,
      TaskState.TASK_RUNNING
    );

    deployChecker.checkDeploys();
    Assertions.assertEquals(
      deployManager.getDeployResult(requestId, firstDeployId).get().getDeployState(),
      DeployState.SUCCEEDED
    );
  }

  @Test
  public void testLbUpdatesAfterEachDeployStep() {
    initLoadBalancedRequest();

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

    deploy(
      secondDeployId,
      Optional.<Boolean>empty(),
      Optional.of(1),
      Optional.<Boolean>empty(),
      true
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
    SingularityPendingDeploy pendingDeploy = deployManager
      .getPendingDeploy(requestId)
      .get();
    Assertions.assertEquals(DeployState.WAITING, pendingDeploy.getCurrentDeployState());

    testingLbClient.setNextBaragonRequestState(BaragonRequestState.WAITING);

    deployChecker.checkDeploys();
    pendingDeploy = deployManager.getPendingDeploy(requestId).get();
    Assertions.assertEquals(DeployState.WAITING, pendingDeploy.getCurrentDeployState());

    testingLbClient.setNextBaragonRequestState(BaragonRequestState.SUCCESS);

    deployChecker.checkDeploys();
    Assertions.assertTrue(
      taskManager.getCleanupTaskIds().contains(firstTask.getTaskId())
    );

    pendingDeploy = deployManager.getPendingDeploy(requestId).get();
    SingularityDeployProgress deployProgressStepOne = pendingDeploy
      .getDeployProgress()
      .get();
    Assertions.assertTrue(deployProgressStepOne.isStepComplete());
    Assertions.assertEquals(1, deployProgressStepOne.getTargetActiveInstances());

    cleaner.drainCleanupQueue();
    statusUpdate(firstTask, TaskState.TASK_KILLED);

    deployChecker.checkDeploys();

    pendingDeploy = deployManager.getPendingDeploy(requestId).get();
    Assertions.assertFalse(pendingDeploy.getDeployProgress().get().isStepComplete());
    Assertions.assertEquals(
      2,
      pendingDeploy.getDeployProgress().get().getTargetActiveInstances()
    );

    scheduler.drainPendingQueue();
    Assertions.assertEquals(1, taskManager.getPendingTaskIds().size());

    resourceOffers();
    Assertions.assertEquals(
      2,
      taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size()
    );

    SingularityTaskId secondNewTaskId = null;
    for (SingularityTaskId taskId : taskManager.getActiveTaskIdsForDeploy(
      requestId,
      secondDeployId
    )) {
      if (taskId.getInstanceNo() == 2) {
        secondNewTaskId = taskId;
      }
    }

    statusUpdate(taskManager.getTask(secondNewTaskId).get(), TaskState.TASK_RUNNING);
    testingLbClient.setNextBaragonRequestState(BaragonRequestState.WAITING);

    deployChecker.checkDeploys();
    pendingDeploy = deployManager.getPendingDeploy(requestId).get();
    Assertions.assertEquals(DeployState.WAITING, pendingDeploy.getCurrentDeployState());

    testingLbClient.setNextBaragonRequestState(BaragonRequestState.SUCCESS);

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

  @Test
  public void testCanceledDeployTasksStayActiveUntilReplaced() {
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

    deploy(
      secondDeployId,
      Optional.<Boolean>empty(),
      Optional.of(1),
      Optional.<Boolean>empty(),
      false
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

    Assertions.assertEquals(1, taskManager.getCleanupTaskIds().size());
    Assertions.assertTrue(
      taskManager.getCleanupTaskIds().contains(firstTask.getTaskId())
    );
    SingularityDeployProgress deployProgressStepOne = deployManager
      .getPendingDeploys()
      .get(0)
      .getDeployProgress()
      .get();
    Assertions.assertTrue(deployProgressStepOne.isStepComplete());
    Assertions.assertEquals(1, deployProgressStepOne.getTargetActiveInstances());

    cleaner.drainCleanupQueue();
    statusUpdate(firstTask, TaskState.TASK_KILLED);

    deployChecker.checkDeploys();
    deployResource.cancelDeploy(singularityUser, requestId, secondDeployId);
    deployChecker.checkDeploys();

    scheduler.drainPendingQueue();
    List<SingularityPendingTaskId> pendingTaskIds = taskManager.getPendingTaskIds();
    Assertions.assertEquals(1, pendingTaskIds.size());
    Assertions.assertEquals(firstDeployId, pendingTaskIds.get(0).getDeployId());

    cleaner.drainCleanupQueue();
    List<SingularityTaskId> cleanupTaskIds = taskManager.getCleanupTaskIds();
    Assertions.assertEquals(1, cleanupTaskIds.size());
    Assertions.assertEquals(secondDeployId, cleanupTaskIds.get(0).getDeployId());

    resourceOffers();
    for (SingularityTaskId taskId : taskManager.getActiveTaskIdsForDeploy(
      requestId,
      firstDeployId
    )) {
      statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_RUNNING);
    }

    cleaner.drainCleanupQueue();

    Assertions.assertEquals(0, taskManager.getCleanupTaskIds().size());
    Assertions.assertEquals(
      2,
      taskManager.getActiveTaskIdsForDeploy(requestId, firstDeployId).size()
    );
  }

  @Test
  public void testAfterDeployWaitsForScheduledTaskToFinish() {
    initScheduledRequest();
    initFirstDeploy();

    SingularityTask firstTask = launchTask(
      request,
      firstDeploy,
      1,
      TaskState.TASK_RUNNING
    );

    Assertions.assertTrue(taskManager.getPendingTasks().isEmpty());
    Assertions.assertTrue(taskManager.getActiveTaskIds().contains(firstTask.getTaskId()));
    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assertions.assertTrue(taskManager.getCleanupTaskIds().isEmpty());

    deploy("nextDeployId");
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();

    // no second task should be scheduled

    Assertions.assertTrue(taskManager.getPendingTasks().isEmpty());
    Assertions.assertTrue(taskManager.getActiveTaskIds().contains(firstTask.getTaskId()));
    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assertions.assertTrue(!taskManager.getCleanupTaskIds().isEmpty());

    statusUpdate(firstTask, TaskState.TASK_FINISHED);
    scheduler.drainPendingQueue();
    cleaner.drainCleanupQueue();

    Assertions.assertTrue(!taskManager.getPendingTasks().isEmpty());
    Assertions.assertTrue(taskManager.getActiveTaskIds().isEmpty());
    Assertions.assertTrue(taskManager.getCleanupTaskIds().isEmpty());

    SingularityPendingTaskId pendingTaskId = taskManager.getPendingTaskIds().get(0);

    Assertions.assertEquals("nextDeployId", pendingTaskId.getDeployId());
    Assertions.assertEquals(requestId, pendingTaskId.getRequestId());
  }

  @Test
  public void testScheduledJobLivesThroughDeploy() {
    initScheduledRequest();
    initFirstDeploy();

    createAndSchedulePendingTask(firstDeployId);

    Assertions.assertTrue(!taskManager.getPendingTaskIds().isEmpty());

    deploy("d2");
    scheduler.drainPendingQueue();

    deployChecker.checkDeploys();

    scheduler.drainPendingQueue();

    Assertions.assertTrue(!taskManager.getPendingTaskIds().isEmpty());
  }

  @Test
  public void testUnpauseOnDeploy() {
    initRequest();
    initFirstDeploy();

    requestManager.pause(
      request,
      System.currentTimeMillis(),
      Optional.<String>empty(),
      Optional.<String>empty()
    );

    boolean exception = false;

    try {
      deploy("d2");
    } catch (Exception e) {
      exception = true;
    }

    Assertions.assertTrue(exception);

    deploy("d3", Optional.of(true));

    Assertions.assertTrue(
      requestManager.getRequest(requestId).get().getState() ==
      RequestState.DEPLOYING_TO_UNPAUSE
    );

    scheduler.drainPendingQueue();
    sms
      .resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1")))
      .join();

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_FAILED);

    deployChecker.checkDeploys();

    Assertions.assertTrue(
      requestManager.getRequest(requestId).get().getState() == RequestState.PAUSED
    );

    Assertions.assertTrue(taskManager.getActiveTaskIds().isEmpty());
    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assertions.assertTrue(requestManager.getPendingRequests().isEmpty());

    deploy("d4", Optional.of(true));

    Assertions.assertTrue(
      requestManager.getRequest(requestId).get().getState() ==
      RequestState.DEPLOYING_TO_UNPAUSE
    );

    scheduler.drainPendingQueue();
    sms
      .resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1")))
      .join();

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_RUNNING);
    deployChecker.checkDeploys();

    RequestState requestState = requestManager.getRequest(requestId).get().getState();

    Assertions.assertTrue(requestState == RequestState.ACTIVE);
  }

  @Test
  public void testSkipDeployHealthchecks() {
    initRequest();

    final String deployId = "deploy_test";

    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId);
    db.setHealthcheckUri(Optional.of("http://uri"));
    db.setSkipHealthchecksOnDeploy(Optional.of(true));

    SingularityDeploy deploy = initDeploy(db, System.currentTimeMillis());

    deployChecker.checkDeploys();

    Assertions.assertTrue(
      !deployManager.getDeployResult(requestId, deployId).isPresent()
    );

    launchTask(request, deploy, System.currentTimeMillis(), 1, TaskState.TASK_RUNNING);

    deployChecker.checkDeploys();

    Assertions.assertEquals(
      DeployState.SUCCEEDED,
      deployManager.getDeployResult(requestId, deployId).get().getDeployState()
    );
  }

  @Test
  public void testUsesNewRequestDataFromPendingDeploy() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)));

    scheduler.drainPendingQueue();

    Assertions.assertEquals(2, taskManager.getPendingTaskIds().size());
    Assertions.assertEquals(
      2,
      requestManager.getRequest(requestId).get().getRequest().getInstancesSafe()
    );

    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();
    SingularityRequest newRequest = request
      .toBuilder()
      .setInstances(Optional.of(1))
      .build();

    String deployId = "test_new_request_data";
    SingularityDeploy deploy = new SingularityDeployBuilder(request.getId(), deployId)
      .setCommand(Optional.of("sleep 100"))
      .build();

    deployResource.deploy(
      new SingularityDeployRequest(
        deploy,
        Optional.empty(),
        Optional.empty(),
        Optional.of(newRequest)
      ),
      singularityUser
    );

    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();

    List<SingularityPendingTaskId> pendingTaskIdsForNewDeploy = new ArrayList<>();
    for (SingularityPendingTaskId pendingTaskId : taskManager.getPendingTaskIds()) {
      if (pendingTaskId.getDeployId().equals(deployId)) {
        pendingTaskIdsForNewDeploy.add(pendingTaskId);
      }
    }

    Assertions.assertEquals(1, pendingTaskIdsForNewDeploy.size());
    Assertions.assertEquals(
      2,
      requestManager.getRequest(requestId).get().getRequest().getInstancesSafe()
    );

    resourceOffers();
    for (SingularityTaskId taskId : taskManager.getActiveTaskIdsForDeploy(
      requestId,
      deployId
    )) {
      statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_RUNNING);
    }
    deployChecker.checkDeploys();

    Assertions.assertEquals(
      1,
      requestManager.getRequest(requestId).get().getRequest().getInstancesSafe()
    );
  }

  @Test
  public void testCannotUpdateRequestDuringPendingDeployWithNewData() {
    Assertions.assertThrows(
      WebApplicationException.class,
      () -> {
        initRequest();
        SingularityRequest request = requestResource
          .getRequest(requestId, singularityUser)
          .getRequest();
        SingularityRequest newRequest = request
          .toBuilder()
          .setInstances(Optional.of(1))
          .build();

        String deployId = "test_new_request_data";
        SingularityDeploy deploy = new SingularityDeployBuilder(request.getId(), deployId)
          .setCommand(Optional.of("sleep 100"))
          .build();

        deployResource.deploy(
          new SingularityDeployRequest(
            deploy,
            Optional.empty(),
            Optional.empty(),
            Optional.of(newRequest)
          ),
          singularityUser
        );

        requestResource.postRequest(newRequest, singularityUser);
      }
    );
  }

  @Test
  public void testDeployTimesOut() {
    initRequest();

    final long hourAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);

    final String deployId = "timeout_test";

    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, deployId);
    db.setDeployHealthTimeoutSeconds(Optional.of(TimeUnit.MINUTES.toSeconds(1)));

    initDeploy(db, hourAgo);

    deployChecker.checkDeploys();

    Assertions.assertEquals(
      DeployState.OVERDUE,
      deployManager.getDeployResult(requestId, deployId).get().getDeployState()
    );
  }

  @Test
  public void testIncrementalDeployInstanceCounter() {
    initRequest();
    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();
    requestResource.postRequest(
      request.toBuilder().setInstances(Optional.of(4)).build(),
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
    SingularityTask thirdTask = launchTask(
      request,
      firstDeploy,
      3,
      TaskState.TASK_RUNNING
    );

    deploy(
      secondDeployId,
      Optional.<Boolean>empty(),
      Optional.of(1),
      Optional.<Boolean>empty(),
      false
    );
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();
    resourceOffers();
    SingularityTaskId firstNewTaskId = taskManager
      .getActiveTaskIdsForDeploy(requestId, secondDeployId)
      .get(0);
    statusUpdate(taskManager.getTask(firstNewTaskId).get(), TaskState.TASK_RUNNING);
    deployChecker.checkDeploys();

    SingularityDeployProgress deployProgress = deployManager
      .getPendingDeploy(requestId)
      .get()
      .getDeployProgress()
      .get();
    Assertions.assertEquals(1, deployProgress.getTargetActiveInstances());
    Assertions.assertEquals(1, deployProgress.getCurrentActiveInstances());

    cleaner.drainCleanupQueue();
    statusUpdate(firstTask, TaskState.TASK_KILLED);

    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();
    resourceOffers();
    for (SingularityTaskId taskId : taskManager.getActiveTaskIdsForDeploy(
      requestId,
      secondDeployId
    )) {
      statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_RUNNING);
    }

    deployChecker.checkDeploys();
    deployProgress =
      deployManager.getPendingDeploy(requestId).get().getDeployProgress().get();
    Assertions.assertEquals(2, deployProgress.getTargetActiveInstances());
    Assertions.assertEquals(2, deployProgress.getCurrentActiveInstances());

    cleaner.drainCleanupQueue();
    statusUpdate(secondTask, TaskState.TASK_KILLED);

    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();
    resourceOffers();
    for (SingularityTaskId taskId : taskManager.getActiveTaskIdsForDeploy(
      requestId,
      secondDeployId
    )) {
      statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_RUNNING);
    }

    deployChecker.checkDeploys();
    deployProgress =
      deployManager.getPendingDeploy(requestId).get().getDeployProgress().get();
    Assertions.assertEquals(3, deployProgress.getTargetActiveInstances());
    Assertions.assertEquals(3, deployProgress.getCurrentActiveInstances());
  }

  @Test
  public void testDeployWithImmediateRunIsLaunchedImmediately() {
    initRequestWithType(RequestType.SCHEDULED, false);
    String deployId = "d1";

    SingularityRunNowRequest runNowRequest = new SingularityRunNowRequestBuilder()
      .setMessage("Message")
      .build();
    SingularityDeploy deploy = new SingularityDeployBuilder(requestId, deployId)
      .setRunImmediately(Optional.of(runNowRequest))
      .setCommand(Optional.of("printenv > tmp.txt"))
      .build();
    SingularityDeployRequest deployRequest = new SingularityDeployRequest(
      deploy,
      Optional.empty(),
      Optional.empty()
    );
    deployResource.deploy(deployRequest, singularityUser);
    deployChecker.checkDeploys();

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(1, taskManager.getNumActiveTasks());
    Assertions.assertEquals(0, taskManager.getNumScheduledTasks());
    SingularityTaskId taskId = taskManager
      .getActiveTaskIdsForDeploy(requestId, deployId)
      .get(0);
    SingularityTask task = taskManager.getTask(taskId).get();

    Map<String, Object> command = (Map<String, Object>) task
      .getMesosTask()
      .getAllOtherFields()
      .get("command");

    Assertions.assertEquals("printenv > tmp.txt", (String) command.get("value"));
  }

  @Test
  public void testDeployWithImmediateRunSchedulesAfterRunningImmediately() {
    initRequestWithType(RequestType.SCHEDULED, false);
    String deployId = "d1";

    SingularityRunNowRequest runNowRequest = new SingularityRunNowRequestBuilder()
      .setMessage("Message")
      .build();
    SingularityDeploy deploy = new SingularityDeployBuilder(requestId, deployId)
      .setRunImmediately(Optional.of(runNowRequest))
      .setCommand(Optional.of("printenv > tmp.txt"))
      .build();
    SingularityDeployRequest deployRequest = new SingularityDeployRequest(
      deploy,
      Optional.empty(),
      Optional.empty()
    );
    deployResource.deploy(deployRequest, singularityUser);
    deployChecker.checkDeploys();

    scheduler.drainPendingQueue();
    resourceOffers();

    SingularityTaskId taskId = taskManager
      .getActiveTaskIdsForDeploy(requestId, deployId)
      .get(0);
    SingularityTask task = taskManager.getTask(taskId).get();
    statusUpdate(task, TaskState.TASK_RUNNING);
    statusUpdate(task, TaskState.TASK_FINISHED);

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(0, taskManager.getNumActiveTasks());
    Assertions.assertEquals(1, taskManager.getNumScheduledTasks());
  }
}
