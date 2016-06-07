package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.SlaveID;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.TaskStatus;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityContainerType;
import com.hubspot.mesos.SingularityDockerInfo;
import com.hubspot.mesos.SingularityDockerNetworkType;
import com.hubspot.mesos.SingularityDockerPortMapping;
import com.hubspot.mesos.SingularityPortMappingType;
import com.hubspot.mesos.SingularityVolume;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.RequestCleanupType;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityDeployProgress;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestLbCleanup;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityUpdatePendingDeployRequest;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.api.SingularityDeleteRequestRequest;
import com.hubspot.singularity.api.SingularityDeployRequest;
import com.hubspot.singularity.api.SingularityKillTaskRequest;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.api.SingularitySkipHealthchecksRequest;
import com.hubspot.singularity.api.SingularityUnpauseRequest;
import com.hubspot.singularity.data.AbstractMachineManager.StateChangeResult;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.scheduler.SingularityNewTaskChecker.CheckTaskState;
import com.hubspot.singularity.scheduler.SingularityTaskReconciliation.ReconciliationState;
import com.sun.jersey.api.ConflictException;

public class SingularitySchedulerTest extends SingularitySchedulerTestBase {
  @Inject
  private SingularityValidator validator;

  public SingularitySchedulerTest() {
    super(false);
  }

  private SingularityPendingTask pendingTask(String requestId, String deployId, PendingType pendingType) {
    return new SingularityPendingTask(new SingularityPendingTaskId(requestId, deployId, System.currentTimeMillis(), 1, pendingType, System.currentTimeMillis()),
        Optional.<List<String>> absent(), Optional.<String> absent(), Optional.<String> absent(), Optional.<Boolean> absent(), Optional.<String> absent());
  }

  @Test
  public void testSchedulerIsolatesPendingTasksBasedOnDeploy() {
    initRequest();
    initFirstDeploy();
    initSecondDeploy();

    SingularityPendingTask p1 = pendingTask(requestId, firstDeployId, PendingType.ONEOFF);
    SingularityPendingTask p2 = pendingTask(requestId, firstDeployId, PendingType.TASK_DONE);
    SingularityPendingTask p3 = pendingTask(requestId, secondDeployId, PendingType.TASK_DONE);

    taskManager.savePendingTask(p1);
    taskManager.savePendingTask(p2);
    taskManager.savePendingTask(p3);

    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, secondDeployId, System.currentTimeMillis(), Optional.<String> absent(), PendingType.NEW_DEPLOY,
        Optional.<Boolean> absent(), Optional.<String> absent()));

    scheduler.drainPendingQueue(stateCacheProvider.get());

    // we expect there to be 3 pending tasks :

    List<SingularityPendingTask> returnedScheduledTasks = taskManager.getPendingTasks();

    Assert.assertEquals(3, returnedScheduledTasks.size());
    Assert.assertTrue(returnedScheduledTasks.contains(p1));
    Assert.assertTrue(returnedScheduledTasks.contains(p2));
    Assert.assertTrue(!returnedScheduledTasks.contains(p3));

    boolean found = false;

    for (SingularityPendingTask pendingTask : returnedScheduledTasks) {
      if (pendingTask.getPendingTaskId().getDeployId().equals(secondDeployId)) {
        found = true;
        Assert.assertEquals(PendingType.NEW_DEPLOY, pendingTask.getPendingTaskId().getPendingType());
      }
    }

    Assert.assertTrue(found);
  }

  @Test
  public void testDeployManagerHandlesFailedLBTask() {
    initLoadBalancedRequest();
    initFirstDeploy();

    SingularityTask firstTask = startTask(firstDeploy);

    initSecondDeploy();

    SingularityTask secondTask = startTask(secondDeploy);

    // this should cause an LB call to happen:
    deployChecker.checkDeploys();

    Assert.assertTrue(taskManager.getLoadBalancerState(secondTask.getTaskId(), LoadBalancerRequestType.ADD).isPresent());
    Assert.assertTrue(!taskManager.getLoadBalancerState(secondTask.getTaskId(), LoadBalancerRequestType.DEPLOY).isPresent());
    Assert.assertTrue(!taskManager.getLoadBalancerState(secondTask.getTaskId(), LoadBalancerRequestType.REMOVE).isPresent());

    statusUpdate(secondTask, TaskState.TASK_FAILED);
    statusUpdate(firstTask, TaskState.TASK_FAILED);

    deployChecker.checkDeploys();

    Assert.assertTrue(deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState() == DeployState.FAILED);

    List<SingularityPendingTask> pendingTasks = taskManager.getPendingTasks();

    Assert.assertTrue(pendingTasks.size() == 1);
    Assert.assertTrue(pendingTasks.get(0).getPendingTaskId().getDeployId().equals(firstDeployId));
  }

  @Test
  public void testDeployClearsObsoleteScheduledTasks() {
    initRequest();
    initFirstDeploy();
    initSecondDeploy();

    SingularityPendingTaskId taskIdOne = new SingularityPendingTaskId(requestId, firstDeployId, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3), 1, PendingType.IMMEDIATE, System.currentTimeMillis());
    SingularityPendingTask taskOne = new SingularityPendingTask(taskIdOne, Optional.<List<String>> absent(), Optional.<String> absent(), Optional.<String> absent(), Optional.<Boolean> absent(), Optional.<String> absent());

    SingularityPendingTaskId taskIdTwo = new SingularityPendingTaskId(requestId, firstDeployId, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1), 2, PendingType.IMMEDIATE, System.currentTimeMillis());
    SingularityPendingTask taskTwo = new SingularityPendingTask(taskIdTwo, Optional.<List<String>> absent(), Optional.<String> absent(), Optional.<String> absent(), Optional.<Boolean> absent(), Optional.<String> absent());

    SingularityPendingTaskId taskIdThree = new SingularityPendingTaskId(requestId, secondDeployId, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3), 1, PendingType.IMMEDIATE, System.currentTimeMillis());
    SingularityPendingTask taskThree = new SingularityPendingTask(taskIdThree, Optional.<List<String>> absent(), Optional.<String> absent(), Optional.<String> absent(), Optional.<Boolean> absent(), Optional.<String> absent());

    SingularityPendingTaskId taskIdFour = new SingularityPendingTaskId(requestId + "hi", firstDeployId, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3), 5, PendingType.IMMEDIATE, System.currentTimeMillis());
    SingularityPendingTask taskFour = new SingularityPendingTask(taskIdFour,Optional.<List<String>> absent(), Optional.<String> absent(), Optional.<String> absent(), Optional.<Boolean> absent(), Optional.<String> absent());

    taskManager.savePendingTask(taskOne);
    taskManager.savePendingTask(taskTwo);
    taskManager.savePendingTask(taskThree);
    taskManager.savePendingTask(taskFour);

    launchTask(request, secondDeploy, 1, TaskState.TASK_RUNNING);

    deployChecker.checkDeploys();

    List<SingularityPendingTaskId> pendingTaskIds = taskManager.getPendingTaskIds();

    Assert.assertTrue(!pendingTaskIds.contains(taskIdOne));
    Assert.assertTrue(!pendingTaskIds.contains(taskIdTwo));

    Assert.assertTrue(pendingTaskIds.contains(taskIdThree));
    Assert.assertTrue(pendingTaskIds.contains(taskIdFour));
  }

  @Test
  public void testDeployAllInstancesAtOnce() {
    initRequest();

    SingularityRequest request = requestResource.getRequest(requestId).getRequest();

    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(2)).build());

    initFirstDeploy();

    launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);
    launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);

    deploy(secondDeployId);
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue(stateCacheProvider.get());

    Assert.assertEquals(2, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(2, taskManager.getPendingTaskIds().size());

    resourceOffers();

    Assert.assertEquals(2, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());

    for (SingularityTaskId taskId : taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId)) {
      statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_RUNNING);
    }

    deployChecker.checkDeploys();
    Assert.assertEquals(2, taskManager.getCleanupTaskIds().size());
    Assert.assertEquals(DeployState.SUCCEEDED, deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState());
    Assert.assertEquals(2, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());
  }

  @Test
  public void testDeployOneInstanceAtATime() {
    initRequest();

    SingularityRequest request = requestResource.getRequest(requestId).getRequest();

    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(2)).build());

    initFirstDeploy();

    SingularityTask firstTask = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);
    SingularityTask secondTask = launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);

    deploy(secondDeployId, Optional.<Boolean> absent(), Optional.of(1), Optional.<Boolean> absent(), false);
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue(stateCacheProvider.get());
    Assert.assertEquals(1, taskManager.getPendingTaskIds().size());

    resourceOffers();
    Assert.assertEquals(1, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());

    SingularityTaskId firstNewTaskId = taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).get(0);
    statusUpdate(taskManager.getTask(firstNewTaskId).get(), TaskState.TASK_RUNNING);
    deployChecker.checkDeploys();

    Assert.assertEquals(1, taskManager.getCleanupTaskIds().size());
    Assert.assertTrue(taskManager.getCleanupTaskIds().contains(firstTask.getTaskId()));
    SingularityDeployProgress deployProgressStepOne = deployManager.getPendingDeploys().get(0).getDeployProgress().get();
    Assert.assertTrue(deployProgressStepOne.isStepComplete());
    Assert.assertEquals(1, deployProgressStepOne.getTargetActiveInstances());

    cleaner.drainCleanupQueue();
    statusUpdate(firstTask, TaskState.TASK_KILLED);

    deployChecker.checkDeploys();

    SingularityDeployProgress deployProgressStepTwo = deployManager.getPendingDeploys().get(0).getDeployProgress().get();
    Assert.assertFalse(deployProgressStepTwo.isStepComplete());
    Assert.assertEquals(2, deployProgressStepTwo.getTargetActiveInstances());

    scheduler.drainPendingQueue(stateCacheProvider.get());
    Assert.assertEquals(1, taskManager.getPendingTaskIds().size());

    resourceOffers();
    Assert.assertEquals(2, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());

    for (SingularityTaskId taskId : taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId)) {
      statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_RUNNING);
    }
    deployChecker.checkDeploys();

    Assert.assertEquals(2, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());
    Assert.assertEquals(DeployState.SUCCEEDED, deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState());
  }

  @Test
  public void testScaleDownDuringDeploy() {
    initRequest();

    SingularityRequest request = requestResource.getRequest(requestId).getRequest();

    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(2)).build());

    initFirstDeploy();

    launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);
    launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);

    deploy(secondDeployId);
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue(stateCacheProvider.get());
    resourceOffers();

    Assert.assertEquals(2, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());

    for (SingularityTaskId taskId : taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId)) {
      statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_RUNNING);
    }

    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(1)).build());
    scheduler.drainPendingQueue(stateCacheProvider.get());

    Assert.assertEquals(1, taskManager.getCleanupTaskIds().size());

    deployChecker.checkDeploys();
    Assert.assertEquals(2, taskManager.getCleanupTaskIds().size());

    // Extra task from the new deploy should get cleaned up as well
    scheduler.drainPendingQueue(stateCacheProvider.get());
    Assert.assertEquals(3, taskManager.getCleanupTaskIds().size());
  }

  @Test
  public void testDeployWithManualStep() {
    initRequest();

    SingularityRequest request = requestResource.getRequest(requestId).getRequest();

    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(2)).build());

    initFirstDeploy();

    SingularityTask firstTask = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);
    SingularityTask secondTask = launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);

    deploy(secondDeployId, Optional.<Boolean>absent(), Optional.of(1), Optional.of(false), false);
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue(stateCacheProvider.get());
    Assert.assertEquals(1, taskManager.getPendingTaskIds().size());

    resourceOffers();
    Assert.assertEquals(1, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());

    SingularityTaskId firstNewTaskId = taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).get(0);
    statusUpdate(taskManager.getTask(firstNewTaskId).get(), TaskState.TASK_RUNNING);
    deployChecker.checkDeploys();

    Assert.assertEquals(1, taskManager.getCleanupTaskIds().size());
    Assert.assertTrue(taskManager.getCleanupTaskIds().contains(firstTask.getTaskId()));
    SingularityDeployProgress deployProgressStepOne = deployManager.getPendingDeploys().get(0).getDeployProgress().get();
    Assert.assertTrue(deployProgressStepOne.isStepComplete());
    Assert.assertEquals(1, deployProgressStepOne.getTargetActiveInstances());

    cleaner.drainCleanupQueue();
    statusUpdate(firstTask, TaskState.TASK_KILLED);

    deployChecker.checkDeploys();

    // Deploy should not have moved to next step even though instances are launched
    deployProgressStepOne = deployManager.getPendingDeploys().get(0).getDeployProgress().get();
    Assert.assertTrue(deployProgressStepOne.isStepComplete());
    Assert.assertEquals(1, deployProgressStepOne.getTargetActiveInstances());

    // Add the 'ok' to move to the next step
    deployResource.updatePendingDeploy(new SingularityUpdatePendingDeployRequest(requestId, secondDeployId, 2));

    deployChecker.checkDeploys();

    SingularityDeployProgress deployProgressStepTwo = deployManager.getPendingDeploys().get(0).getDeployProgress().get();
    Assert.assertFalse(deployProgressStepTwo.isStepComplete());
    Assert.assertEquals(2, deployProgressStepTwo.getTargetActiveInstances());

    scheduler.drainPendingQueue(stateCacheProvider.get());
    Assert.assertEquals(1, taskManager.getPendingTaskIds().size());

    resourceOffers();
    Assert.assertEquals(2, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());

    for (SingularityTaskId taskId : taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId)) {
      statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_RUNNING);
    }
    deployChecker.checkDeploys();

    Assert.assertEquals(2, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());
    Assert.assertEquals(DeployState.SUCCEEDED, deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState());
  }

  @Test
  public void testDeployMultipleInstancesAtOnce() {
    initRequest();

    SingularityRequest request = requestResource.getRequest(requestId).getRequest();

    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(4)).build());

    initFirstDeploy();

    SingularityTask firstTask = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);
    SingularityTask secondTask = launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);
    SingularityTask thirdTask = launchTask(request, firstDeploy, 3, TaskState.TASK_RUNNING);
    SingularityTask fourthTask = launchTask(request, firstDeploy, 4, TaskState.TASK_RUNNING);

    deploy(secondDeployId, Optional.<Boolean>absent(), Optional.of(2), Optional.<Boolean> absent(), false);
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue(stateCacheProvider.get());
    Assert.assertEquals(2, taskManager.getPendingTaskIds().size());

    resourceOffers();
    Assert.assertEquals(2, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());

    for (SingularityTaskId taskId : taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId)) {
      statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_RUNNING);
    }
    deployChecker.checkDeploys();

    Assert.assertEquals(2, taskManager.getCleanupTaskIds().size());
    List<SingularityTaskId> cleanupTaskIds = taskManager.getCleanupTaskIds();
    Assert.assertTrue(cleanupTaskIds.contains(firstTask.getTaskId()) && cleanupTaskIds.contains(secondTask.getTaskId()));
    SingularityDeployProgress deployProgressStepOne = deployManager.getPendingDeploys().get(0).getDeployProgress().get();
    Assert.assertTrue(deployProgressStepOne.isStepComplete());
    Assert.assertEquals(2, deployProgressStepOne.getTargetActiveInstances());

    cleaner.drainCleanupQueue();
    statusUpdate(firstTask, TaskState.TASK_KILLED);
    statusUpdate(secondTask, TaskState.TASK_KILLED);

    deployChecker.checkDeploys();

    SingularityDeployProgress deployProgressStepTwo = deployManager.getPendingDeploys().get(0).getDeployProgress().get();
    Assert.assertFalse(deployProgressStepTwo.isStepComplete());
    Assert.assertEquals(4, deployProgressStepTwo.getTargetActiveInstances());

    scheduler.drainPendingQueue(stateCacheProvider.get());
    Assert.assertEquals(2, taskManager.getPendingTaskIds().size());

    resourceOffers();
    Assert.assertEquals(4, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());

    for (SingularityTaskId taskId : taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId)) {
      statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_RUNNING);
    }
    deployChecker.checkDeploys();

    Assert.assertEquals(4, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());
    Assert.assertEquals(DeployState.SUCCEEDED, deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState());
  }

  @Test
  public void testCancelDeploy() {
    initRequest();

    SingularityRequest request = requestResource.getRequest(requestId).getRequest();

    initFirstDeploy();

    SingularityTask firstTask = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    deploy(secondDeployId, Optional.<Boolean>absent(), Optional.of(1), Optional.of(false), false);
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue(stateCacheProvider.get());
    Assert.assertEquals(1, taskManager.getPendingTaskIds().size());

    resourceOffers();
    Assert.assertEquals(1, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());

    SingularityTaskId firstNewTaskId = taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).get(0);
    statusUpdate(taskManager.getTask(firstNewTaskId).get(), TaskState.TASK_RUNNING);
    deployResource.cancelDeploy(requestId, secondDeployId);

    deployChecker.checkDeploys();

    Assert.assertTrue(taskManager.getCleanupTaskIds().contains(firstNewTaskId));
    Assert.assertFalse(taskManager.getCleanupTaskIds().contains(firstTask.getTaskId()));
    Assert.assertEquals(DeployState.CANCELED, deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState());

  }

  @Test
  public void testDeployFails() {
    initRequest();

    SingularityRequest request = requestResource.getRequest(requestId).getRequest();

    initFirstDeploy();

    SingularityTask firstTask = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    deploy(secondDeployId, Optional.<Boolean>absent(), Optional.of(1), Optional.of(false), false);
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue(stateCacheProvider.get());
    Assert.assertEquals(1, taskManager.getPendingTaskIds().size());

    resourceOffers();
    Assert.assertEquals(1, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());

    SingularityTaskId firstNewTaskId = taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).get(0);
    statusUpdate(taskManager.getTask(firstNewTaskId).get(), TaskState.TASK_FAILED);

    deployChecker.checkDeploys();

    Assert.assertFalse(taskManager.getCleanupTaskIds().contains(firstTask.getTaskId()));
    Assert.assertEquals(DeployState.FAILED, deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState());
  }

  @Test
  public void testDeployFailsAfterMaxTaskRetries() {
    initRequest();

    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, firstDeployId);
    db.setMaxTaskRetries(Optional.of(1));
    SingularityDeploy deploy = initDeploy(db, System.currentTimeMillis());

    deployChecker.checkDeploys();
    Assert.assertTrue(!deployManager.getDeployResult(requestId, firstDeployId).isPresent());

    SingularityTask task = launchTask(request, deploy, System.currentTimeMillis(), 1, TaskState.TASK_FAILED);

    deployChecker.checkDeploys();
    Assert.assertEquals(deployManager.getPendingDeploys().get(0).getCurrentDeployState(), DeployState.WAITING);

    SingularityTask taskTryTwo = launchTask(request, deploy, System.currentTimeMillis(), 1, TaskState.TASK_FAILED);

    deployChecker.checkDeploys();
    Assert.assertEquals(deployManager.getDeployResult(requestId, firstDeployId).get().getDeployState(), DeployState.FAILED);
  }

  @Test
  public void testDeploySucceedsWithTaskRetries() {
    initRequest();

    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, firstDeployId);
    db.setMaxTaskRetries(Optional.of(1));
    SingularityDeploy deploy = initDeploy(db, System.currentTimeMillis());

    deployChecker.checkDeploys();
    Assert.assertTrue(!deployManager.getDeployResult(requestId, firstDeployId).isPresent());

    SingularityTask task = launchTask(request, deploy, System.currentTimeMillis(), 1, TaskState.TASK_FAILED);

    deployChecker.checkDeploys();
    Assert.assertEquals(deployManager.getPendingDeploys().get(0).getCurrentDeployState(), DeployState.WAITING);

    SingularityTask taskTryTwo = launchTask(request, deploy, System.currentTimeMillis(), 1, TaskState.TASK_RUNNING);

    deployChecker.checkDeploys();
    Assert.assertEquals(deployManager.getDeployResult(requestId, firstDeployId).get().getDeployState(), DeployState.SUCCEEDED);
  }

  @Test
  public void testLbUpdatesAfterEachDeployStep() {
    initLoadBalancedRequest();

    SingularityRequest request = requestResource.getRequest(requestId).getRequest();
    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(2)).build());
    initFirstDeploy();
    SingularityTask firstTask = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);
    SingularityTask secondTask = launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);

    deploy(secondDeployId, Optional.<Boolean>absent(), Optional.of(1), Optional.<Boolean> absent(), true);
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue(stateCacheProvider.get());
    Assert.assertEquals(1, taskManager.getPendingTaskIds().size());

    resourceOffers();
    Assert.assertEquals(1, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());
    SingularityTaskId firstNewTaskId = taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).get(0);

    statusUpdate(taskManager.getTask(firstNewTaskId).get(), TaskState.TASK_RUNNING);
    deployChecker.checkDeploys();
    SingularityPendingDeploy pendingDeploy = deployManager.getPendingDeploy(requestId).get();
    Assert.assertEquals(DeployState.WAITING, pendingDeploy.getCurrentDeployState());

    testingLbClient.setNextBaragonRequestState(BaragonRequestState.WAITING);

    deployChecker.checkDeploys();
    pendingDeploy = deployManager.getPendingDeploy(requestId).get();
    Assert.assertEquals(DeployState.WAITING, pendingDeploy.getCurrentDeployState());

    testingLbClient.setNextBaragonRequestState(BaragonRequestState.SUCCESS);

    deployChecker.checkDeploys();
    Assert.assertTrue(taskManager.getCleanupTaskIds().contains(firstTask.getTaskId()));

    pendingDeploy = deployManager.getPendingDeploy(requestId).get();
    SingularityDeployProgress deployProgressStepOne = pendingDeploy.getDeployProgress().get();
    Assert.assertTrue(deployProgressStepOne.isStepComplete());
    Assert.assertEquals(1, deployProgressStepOne.getTargetActiveInstances());

    cleaner.drainCleanupQueue();
    statusUpdate(firstTask, TaskState.TASK_KILLED);

    deployChecker.checkDeploys();

    pendingDeploy = deployManager.getPendingDeploy(requestId).get();
    Assert.assertFalse(pendingDeploy.getDeployProgress().get().isStepComplete());
    Assert.assertEquals(2, pendingDeploy.getDeployProgress().get().getTargetActiveInstances());

    scheduler.drainPendingQueue(stateCacheProvider.get());
    Assert.assertEquals(1, taskManager.getPendingTaskIds().size());

    resourceOffers();
    Assert.assertEquals(2, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());

    SingularityTaskId secondNewTaskId = null;
    for (SingularityTaskId taskId : taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId)) {
      if (taskId.getInstanceNo() == 2) {
        secondNewTaskId = taskId;
      }
    }

    statusUpdate(taskManager.getTask(secondNewTaskId).get(), TaskState.TASK_RUNNING);
    testingLbClient.setNextBaragonRequestState(BaragonRequestState.WAITING);

    deployChecker.checkDeploys();
    pendingDeploy = deployManager.getPendingDeploy(requestId).get();
    Assert.assertEquals(DeployState.WAITING, pendingDeploy.getCurrentDeployState());

    testingLbClient.setNextBaragonRequestState(BaragonRequestState.SUCCESS);

    deployChecker.checkDeploys();

    Assert.assertEquals(2, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());
    Assert.assertEquals(DeployState.SUCCEEDED, deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState());
  }

  @Test
  public void testCanceledDeployTasksStayActiveUntilReplaced() {
    initRequest();

    SingularityRequest request = requestResource.getRequest(requestId).getRequest();

    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(2)).build());

    initFirstDeploy();

    SingularityTask firstTask = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);
    SingularityTask secondTask = launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);

    deploy(secondDeployId, Optional.<Boolean> absent(), Optional.of(1), Optional.<Boolean> absent(), false);
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue(stateCacheProvider.get());
    Assert.assertEquals(1, taskManager.getPendingTaskIds().size());

    resourceOffers();
    Assert.assertEquals(1, taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).size());

    SingularityTaskId firstNewTaskId = taskManager.getActiveTaskIdsForDeploy(requestId, secondDeployId).get(0);
    statusUpdate(taskManager.getTask(firstNewTaskId).get(), TaskState.TASK_RUNNING);
    deployChecker.checkDeploys();

    Assert.assertEquals(1, taskManager.getCleanupTaskIds().size());
    Assert.assertTrue(taskManager.getCleanupTaskIds().contains(firstTask.getTaskId()));
    SingularityDeployProgress deployProgressStepOne = deployManager.getPendingDeploys().get(0).getDeployProgress().get();
    Assert.assertTrue(deployProgressStepOne.isStepComplete());
    Assert.assertEquals(1, deployProgressStepOne.getTargetActiveInstances());

    cleaner.drainCleanupQueue();
    statusUpdate(firstTask, TaskState.TASK_KILLED);

    deployChecker.checkDeploys();
    deployResource.cancelDeploy(requestId, secondDeployId);
    deployChecker.checkDeploys();

    scheduler.drainPendingQueue(stateCacheProvider.get());
    List<SingularityPendingTaskId> pendingTaskIds = taskManager.getPendingTaskIds();
    Assert.assertEquals(1, pendingTaskIds.size());
    Assert.assertEquals(firstDeployId, pendingTaskIds.get(0).getDeployId());

    cleaner.drainCleanupQueue();
    List<SingularityTaskId> cleanupTaskIds = taskManager.getCleanupTaskIds();
    Assert.assertEquals(1, cleanupTaskIds.size());
    Assert.assertEquals(secondDeployId, cleanupTaskIds.get(0).getDeployId());

    resourceOffers();
    for (SingularityTaskId taskId : taskManager.getActiveTaskIdsForDeploy(requestId, firstDeployId)) {
      statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_RUNNING);
    }

    cleaner.drainCleanupQueue();

    Assert.assertEquals(0, taskManager.getCleanupTaskIds().size());
    Assert.assertEquals(2, taskManager.getActiveTaskIdsForDeploy(requestId, firstDeployId).size());
  }

  @Test
  public void testAfterDeployWaitsForScheduledTaskToFinish() {
    initScheduledRequest();
    initFirstDeploy();

    SingularityTask firstTask = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    Assert.assertTrue(taskManager.getPendingTasks().isEmpty());
    Assert.assertTrue(taskManager.getActiveTaskIds().contains(firstTask.getTaskId()));
    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assert.assertTrue(taskManager.getCleanupTaskIds().isEmpty());

    deploy("nextDeployId");
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue(stateCacheProvider.get());

    // no second task should be scheduled

    Assert.assertTrue(taskManager.getPendingTasks().isEmpty());
    Assert.assertTrue(taskManager.getActiveTaskIds().contains(firstTask.getTaskId()));
    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assert.assertTrue(!taskManager.getCleanupTaskIds().isEmpty());

    statusUpdate(firstTask, TaskState.TASK_FINISHED);
    scheduler.drainPendingQueue(stateCacheProvider.get());
    cleaner.drainCleanupQueue();

    Assert.assertTrue(!taskManager.getPendingTasks().isEmpty());
    Assert.assertTrue(taskManager.getActiveTaskIds().isEmpty());
    Assert.assertTrue(taskManager.getCleanupTaskIds().isEmpty());

    SingularityPendingTaskId pendingTaskId = taskManager.getPendingTaskIds().get(0);

    Assert.assertEquals("nextDeployId", pendingTaskId.getDeployId());
    Assert.assertEquals(requestId, pendingTaskId.getRequestId());
  }

  @Test
  public void testCleanerLeavesPausedRequestTasksByDemand() {
    initScheduledRequest();
    initFirstDeploy();

    SingularityTask firstTask = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);
    createAndSchedulePendingTask(firstDeployId);

    requestResource.pause(requestId, Optional.of(new SingularityPauseRequest(Optional.of(false), Optional.<Long> absent(), Optional.<String> absent(), Optional.<String>absent())));

    cleaner.drainCleanupQueue();

    Assert.assertTrue(taskManager.getKilledTaskIdRecords().isEmpty());
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assert.assertTrue(requestManager.getCleanupRequests().isEmpty());

    statusUpdate(firstTask, TaskState.TASK_FINISHED);

    // make sure something new isn't scheduled!
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
  }

  @Test
  public void testTaskKill() {
    initRequest();
    initFirstDeploy();

    SingularityTask firstTask = startTask(firstDeploy);

    taskResource.killTask(firstTask.getTaskId().getId());

    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assert.assertEquals(0, taskManager.getNumCleanupTasks());
    Assert.assertEquals(0, taskManager.getNumActiveTasks());
  }

  @Test
  public void testTaskBounce() {
    initRequest();
    initFirstDeploy();

    SingularityTask firstTask = startTask(firstDeploy);

    taskResource.killTask(firstTask.getTaskId().getId(), Optional.of(
        new SingularityKillTaskRequest(Optional.<Boolean> absent(), Optional.of("msg"), Optional.<String> absent(), Optional.of(true))));

    cleaner.drainCleanupQueue();

    killKilledTasks();

    Assert.assertEquals(1, taskManager.getNumCleanupTasks());
    Assert.assertEquals(0, taskManager.getKilledTaskIdRecords().size());

    resourceOffers();
    runLaunchedTasks();

    Assert.assertEquals(1, taskManager.getNumCleanupTasks());
    Assert.assertEquals(0, taskManager.getKilledTaskIdRecords().size());
    Assert.assertEquals(2, taskManager.getNumActiveTasks());

    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assert.assertEquals(0, taskManager.getNumCleanupTasks());
    Assert.assertEquals(1, taskManager.getNumActiveTasks());
  }

  @Test
  public void testBounceWithLoadBalancer() {
    initLoadBalancedRequest();
    initFirstDeploy();
    configuration.setNewTaskCheckerBaseDelaySeconds(1000000);

    SingularityTask taskOne = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    saveLoadBalancerState(BaragonRequestState.SUCCESS, taskOne.getTaskId(), LoadBalancerRequestType.ADD);

    requestResource.bounce(requestId, Optional.<SingularityBounceRequest> absent());

    cleaner.drainCleanupQueue();
    resourceOffers();

    Assert.assertEquals(2, taskManager.getNumActiveTasks());

    List<SingularityTaskId> tasks = taskManager.getActiveTaskIds();
    tasks.remove(taskOne.getTaskId());

    SingularityTaskId taskTwo = tasks.get(0);

    cleaner.drainCleanupQueue();

    runLaunchedTasks();

    cleaner.drainCleanupQueue();

    Assert.assertEquals(0, taskManager.getKilledTaskIdRecords().size());
    Assert.assertEquals(2, taskManager.getNumActiveTasks());

    // add to LB:
    saveLoadBalancerState(BaragonRequestState.SUCCESS, taskTwo, LoadBalancerRequestType.ADD);

    cleaner.drainCleanupQueue();

    Assert.assertEquals(0, taskManager.getKilledTaskIdRecords().size());
    Assert.assertEquals(2, taskManager.getNumActiveTasks());

    saveLoadBalancerState(BaragonRequestState.SUCCESS, taskOne.getTaskId(), LoadBalancerRequestType.REMOVE);

    cleaner.drainCleanupQueue();

    Assert.assertEquals(1, taskManager.getKilledTaskIdRecords().size());

    killKilledTasks();

    Assert.assertEquals(1, taskManager.getNumActiveTasks());
  }

  @Test
  public void testKilledTaskIdRecords() {
    initScheduledRequest();
    initFirstDeploy();

    SingularityTask firstTask = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    requestResource.deleteRequest(requestId, Optional.<SingularityDeleteRequestRequest> absent());

    Assert.assertTrue(requestManager.getCleanupRequests().size() == 1);

    cleaner.drainCleanupQueue();

    Assert.assertTrue(requestManager.getCleanupRequests().isEmpty());
    Assert.assertTrue(!taskManager.getKilledTaskIdRecords().isEmpty());

    cleaner.drainCleanupQueue();

    Assert.assertTrue(!taskManager.getKilledTaskIdRecords().isEmpty());

    statusUpdate(firstTask, TaskState.TASK_KILLED);

    Assert.assertTrue(taskManager.getKilledTaskIdRecords().isEmpty());
  }

  @Test
  public void testLongRunningTaskKills() {
    initScheduledRequest();
    initFirstDeploy();

    launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    initSecondDeploy();
    deployChecker.checkDeploys();

    Assert.assertTrue(taskManager.getKilledTaskIdRecords().isEmpty());
    Assert.assertTrue(!taskManager.getCleanupTasks().isEmpty());

    cleaner.drainCleanupQueue();

    Assert.assertTrue(taskManager.getKilledTaskIdRecords().isEmpty());
    Assert.assertTrue(!taskManager.getCleanupTasks().isEmpty());

    requestManager.activate(request.toBuilder().setKillOldNonLongRunningTasksAfterMillis(Optional.<Long>of(0L)).build(), RequestHistoryType.CREATED, System.currentTimeMillis(), Optional.<String>absent(), Optional.<String>absent());

    cleaner.drainCleanupQueue();

    Assert.assertTrue(!taskManager.getKilledTaskIdRecords().isEmpty());
    Assert.assertTrue(taskManager.getCleanupTasks().isEmpty());
  }

  @Test
  public void testSchedulerCanBatchOnOffers() {
    initRequest();
    initFirstDeploy();

    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(3)).build());
    scheduler.drainPendingQueue(stateCacheProvider.get());

    List<Offer> oneOffer = Arrays.asList(createOffer(12, 1024));
    sms.resourceOffers(driver, oneOffer);

    Assert.assertTrue(taskManager.getActiveTasks().size() == 3);
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());
  }

  @Test
  public void testSchedulerExhaustsOffers() {
    initRequest();
    initFirstDeploy();

    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(10)).build());
    scheduler.drainPendingQueue(stateCacheProvider.get());

    sms.resourceOffers(driver, Arrays.asList(createOffer(2, 1024), createOffer(1, 1024)));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 3);
    Assert.assertTrue(taskManager.getPendingTaskIds().size() == 7);
  }

  @Test
  public void testSchedulerRandomizesOffers() {
    initRequest();
    initFirstDeploy();

    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(15)).build());
    scheduler.drainPendingQueue(stateCacheProvider.get());

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 1024), createOffer(20, 1024)));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 15);

    Set<String> offerIds = Sets.newHashSet();

    for (SingularityTask activeTask : taskManager.getActiveTasks()) {
      offerIds.add(activeTask.getOffer().getId().getValue());
    }

    Assert.assertTrue(offerIds.size() == 2);
  }

  @Test
  public void testSchedulerHandlesFinishedTasks() {
    initScheduledRequest();
    initFirstDeploy();

    schedule = "*/1 * * * * ? 1995";

    // cause it to be pending
    requestResource.postRequest(request.toBuilder().setQuartzSchedule(Optional.of(schedule)).build());
    scheduler.drainPendingQueue(stateCacheProvider.get());

    Assert.assertTrue(requestResource.getActiveRequests().isEmpty());
    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.FINISHED);
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());

    schedule = "*/1 * * * * ?";
    requestResource.postRequest(request.toBuilder().setQuartzSchedule(Optional.of(schedule)).build());
    scheduler.drainPendingQueue(stateCacheProvider.get());

    Assert.assertTrue(!requestResource.getActiveRequests().isEmpty());
    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.ACTIVE);

    Assert.assertTrue(!taskManager.getPendingTaskIds().isEmpty());
  }

  @Test
  public void testScheduledJobLivesThroughDeploy() {
    initScheduledRequest();
    initFirstDeploy();

    createAndSchedulePendingTask(firstDeployId);

    Assert.assertTrue(!taskManager.getPendingTaskIds().isEmpty());

    deploy("d2");
    scheduler.drainPendingQueue(stateCacheProvider.get());

    deployChecker.checkDeploys();

    scheduler.drainPendingQueue(stateCacheProvider.get());

    Assert.assertTrue(!taskManager.getPendingTaskIds().isEmpty());
  }

  @Test
  public void testOneOffsDontRunByThemselves() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId, RequestType.ON_DEMAND);
    requestResource.postRequest(bldr.build());
    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());

    deploy("d2");
    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());

    deployChecker.checkDeploys();

    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());

    requestResource.scheduleImmediately(requestId);

    resourceOffers();

    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_FINISHED);

    resourceOffers();
    Assert.assertEquals(0, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(0, taskManager.getPendingTaskIds().size());

    requestResource.scheduleImmediately(requestId);

    resourceOffers();

    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    resourceOffers();
    Assert.assertEquals(0, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(0, taskManager.getPendingTaskIds().size());
  }

  @Test
  public void testOneOffsDontMoveDuringDecomission() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId, RequestType.ON_DEMAND);
    requestResource.postRequest(bldr.build());
    deploy("d2");

    requestResource.scheduleImmediately(requestId);

    validateTaskDoesntMoveDuringDecommission();
  }

  private void validateTaskDoesntMoveDuringDecommission() {
    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave1", "host1", Optional.of("rack1"))));
    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave2", "host2", Optional.of("rack1"))));

    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());

    Assert.assertEquals("host1", taskManager.getActiveTaskIds().get(0).getSanitizedHost());

    Assert.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.STARTING_DECOMMISSION, Optional.<String> absent(), Optional.of("user1")));

    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave2", "host2", Optional.of("rack1"))));

    cleaner.drainCleanupQueue();

    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave2", "host2", Optional.of("rack1"))));

    cleaner.drainCleanupQueue();

    // task should not move!
    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assert.assertEquals("host1", taskManager.getActiveTaskIds().get(0).getSanitizedHost());
    Assert.assertTrue(taskManager.getKilledTaskIdRecords().isEmpty());
    Assert.assertTrue(taskManager.getCleanupTaskIds().size() == 1);
  }

  @Test
  public void testRunOnceRunOnlyOnce() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId, RequestType.RUN_ONCE);
    request = bldr.build();
    saveRequest(request);

    deployResource.deploy(new SingularityDeployRequest(new SingularityDeployBuilder(requestId, "d1").setCommand(Optional.of("cmd")).build(), Optional.<Boolean> absent(), Optional.<String> absent()));

    scheduler.drainPendingQueue(stateCacheProvider.get());

    deployChecker.checkDeploys();

    resourceOffers();

    Assert.assertTrue(deployManager.getRequestDeployState(requestId).get().getActiveDeploy().isPresent());
    Assert.assertTrue(!deployManager.getRequestDeployState(requestId).get().getPendingDeploy().isPresent());

    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    resourceOffers();

    Assert.assertTrue(taskManager.getActiveTaskIds().isEmpty());

    deployResource.deploy(new SingularityDeployRequest(new SingularityDeployBuilder(requestId, "d2").setCommand(Optional.of("cmd")).build(), Optional.<Boolean>absent(), Optional.<String> absent()));

    scheduler.drainPendingQueue(stateCacheProvider.get());

    deployChecker.checkDeploys();

    resourceOffers();

    Assert.assertTrue(deployManager.getRequestDeployState(requestId).get().getActiveDeploy().isPresent());
    Assert.assertTrue(!deployManager.getRequestDeployState(requestId).get().getPendingDeploy().isPresent());

    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_FINISHED);

    resourceOffers();

    Assert.assertTrue(taskManager.getActiveTaskIds().isEmpty());
  }

  @Test
  public void testMultipleRunOnceTasks() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId, RequestType.RUN_ONCE);
    request = bldr.build();
    saveRequest(request);

    deployResource.deploy(new SingularityDeployRequest(new SingularityDeployBuilder(requestId, "d1").setCommand(Optional.of("cmd")).build(), Optional.<Boolean> absent(), Optional.<String> absent()));
    deployChecker.checkDeploys();
    Assert.assertEquals(1, requestManager.getSizeOfPendingQueue());

    deployResource.deploy(new SingularityDeployRequest(new SingularityDeployBuilder(requestId, "d2").setCommand(Optional.of("cmd")).build(), Optional.<Boolean> absent(), Optional.<String> absent()));
    deployChecker.checkDeploys();
    Assert.assertEquals(2, requestManager.getSizeOfPendingQueue());

    scheduler.drainPendingQueue(stateCacheProvider.get());

    resourceOffers();
    Assert.assertEquals(2, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testRunOnceDontMoveDuringDecomission() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId, RequestType.RUN_ONCE);
    request = bldr.build();
    saveRequest(request);

    deployResource.deploy(new SingularityDeployRequest(new SingularityDeployBuilder(requestId, "d1").setCommand(Optional.of("cmd")).build(), Optional.<Boolean> absent(), Optional.<String> absent()));

    scheduler.drainPendingQueue(stateCacheProvider.get());

    deployChecker.checkDeploys();

    validateTaskDoesntMoveDuringDecommission();
  }

  @Test
  public void testDecommissionDoesntKillPendingDeploy() {
    initRequest();

    deployResource.deploy(new SingularityDeployRequest(new SingularityDeployBuilder(requestId, "d1").setCommand(Optional.of("cmd")).build(), Optional.<Boolean> absent(), Optional.<String> absent()));

    scheduler.drainPendingQueue(stateCacheProvider.get());
    deployChecker.checkDeploys();
    resourceOffers();

    Assert.assertEquals(1, taskManager.getNumActiveTasks());

    slaveResource.decommissionSlave(taskManager.getActiveTasks().get(0).getOffer().getSlaveId().getValue(), Optional.<SingularityMachineChangeRequest> absent());

    scheduler.checkForDecomissions(stateCacheProvider.get());

    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assert.assertEquals(1, taskManager.getNumActiveTasks());
    Assert.assertEquals(1, taskManager.getNumCleanupTasks());
    Assert.assertEquals(0, taskManager.getKilledTaskIdRecords().size());

    configuration.setPendingDeployHoldTaskDuringDecommissionMillis(1);

    try {
      Thread.sleep(2);
    } catch (InterruptedException e) {}

    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assert.assertEquals(0, taskManager.getNumActiveTasks());
    Assert.assertEquals(0, taskManager.getNumCleanupTasks());
  }

  @Test
  public void testRetries() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId, RequestType.RUN_ONCE);
    request = bldr.setNumRetriesOnFailure(Optional.of(2)).build();
    saveRequest(request);

    deployResource.deploy(new SingularityDeployRequest(new SingularityDeployBuilder(requestId, "d1").setCommand(Optional.of("cmd")).build(), Optional.<Boolean> absent(), Optional.<String> absent()));

    scheduler.drainPendingQueue(stateCacheProvider.get());
    deployChecker.checkDeploys();
    resourceOffers();

    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    resourceOffers();

    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    resourceOffers();

    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    resourceOffers();

    Assert.assertTrue(taskManager.getActiveTaskIds().isEmpty());
  }

  @Test
  public void testCooldownAfterSequentialFailures() {
    initRequest();
    initFirstDeploy();

    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.ACTIVE);

    configuration.setCooldownAfterFailures(2);

    SingularityTask firstTask = startTask(firstDeploy);
    SingularityTask secondTask = startTask(firstDeploy);

    statusUpdate(firstTask, TaskState.TASK_FAILED);

    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.ACTIVE);

    statusUpdate(secondTask, TaskState.TASK_FAILED);

    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.SYSTEM_COOLDOWN);

    cooldownChecker.checkCooldowns();

    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.SYSTEM_COOLDOWN);

    SingularityTask thirdTask = startTask(firstDeploy);

    statusUpdate(thirdTask, TaskState.TASK_FINISHED);

    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.ACTIVE);
  }

  @Test
  public void testCooldownOnlyWhenTasksRapidlyFail() {
    initRequest();
    initFirstDeploy();

    configuration.setCooldownAfterFailures(1);

    SingularityTask firstTask = startTask(firstDeploy);
    statusUpdate(firstTask, TaskState.TASK_FAILED, Optional.of(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(5)));

    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.ACTIVE);

    SingularityTask secondTask = startTask(firstDeploy);
    statusUpdate(secondTask, TaskState.TASK_FAILED);

    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.SYSTEM_COOLDOWN);
  }

  @Test
  public void testCooldownScalesToInstances() {
    initRequest();
    initFirstDeploy();

    configuration.setCooldownAfterFailures(2);
    configuration.setCooldownAfterPctOfInstancesFail(.51);

    requestManager.activate(request.toBuilder().setInstances(Optional.of(4)).build(), RequestHistoryType.CREATED, System.currentTimeMillis(), Optional.<String> absent(), Optional.<String>absent());

    SingularityTask task1 = startTask(firstDeploy, 1);
    SingularityTask task2 = startTask(firstDeploy, 2);
    SingularityTask task3 = startTask(firstDeploy, 3);
    SingularityTask task4 = startTask(firstDeploy, 4);

    statusUpdate(task1, TaskState.TASK_FAILED);
    statusUpdate(task2, TaskState.TASK_FAILED);
    statusUpdate(task3, TaskState.TASK_FAILED);

    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.ACTIVE);

    task1 = startTask(firstDeploy, 1);
    task2 = startTask(firstDeploy, 2);
    task3 = startTask(firstDeploy, 3);

    statusUpdate(task1, TaskState.TASK_FAILED);
    statusUpdate(task2, TaskState.TASK_FAILED);

    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.ACTIVE);

    statusUpdate(task3, TaskState.TASK_FAILED);

    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.SYSTEM_COOLDOWN);

    statusUpdate(task4, TaskState.TASK_FINISHED);

    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.ACTIVE);
  }

  @Test
  public void testSlavePlacementSeparate() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)).setSlavePlacement(Optional.of(SlavePlacement.SEPARATE)));

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1"), createOffer(20, 20000, "slave1", "host1")));

    Assert.assertTrue(taskManager.getPendingTaskIds().size() == 1);
    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1")));

    Assert.assertTrue(taskManager.getPendingTaskIds().size() == 1);
    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    eventListener.taskHistoryUpdateEvent(new SingularityTaskHistoryUpdate(taskManager.getActiveTaskIds().get(0), System.currentTimeMillis(), ExtendedTaskState.TASK_CLEANING, Optional.<String>absent(), Optional.<String>absent()));

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1")));

    Assert.assertTrue(taskManager.getPendingTaskIds().size() == 1);
    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave2", "host2")));

    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 2);
  }

  @Test
  public void testSlavePlacementOptimistic() {
    initRequest();
    initFirstDeploy();

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1"), createOffer(20, 20000, "slave2", "host2")));

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(3)).setSlavePlacement(Optional.of(SlavePlacement.OPTIMISTIC)));

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1")));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() < 3);

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1")));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() < 3);

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave2", "host2")));

    eventListener.taskHistoryUpdateEvent(new SingularityTaskHistoryUpdate(taskManager.getActiveTaskIds().get(0), System.currentTimeMillis(), ExtendedTaskState.TASK_CLEANING, Optional.<String>absent(), Optional.<String>absent()));

    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 3);
  }

  @Test
  public void testSlavePlacementOptimisticSingleOffer() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(3)).setSlavePlacement(Optional.of(SlavePlacement.OPTIMISTIC)));

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1"), createOffer(20, 20000, "slave2", "host2")));

    eventListener.taskHistoryUpdateEvent(new SingularityTaskHistoryUpdate(taskManager.getActiveTaskIds().get(0), System.currentTimeMillis(), ExtendedTaskState.TASK_CLEANING, Optional.<String>absent(), Optional.<String>absent()));

    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 3);
  }

  @Test
  public void testSlavePlacementGreedy() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(3)).setSlavePlacement(Optional.of(SlavePlacement.GREEDY)));

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1")));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 3);
  }

  @Test
  public void testReservedSlaveAttribute() {
    Map<String, List<String>> reservedAttributes = new HashMap<>();
    reservedAttributes.put("reservedKey", Arrays.asList("reservedValue1"));
    configuration.setReserveSlavesWithAttributes(reservedAttributes);

    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)));

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("reservedKey", "reservedValue1"))));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave2", "host2", Optional.<String>absent(), ImmutableMap.of("reservedKey", "notAReservedValue"))));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testReservedSlaveWithMatchinRequestAttribute() {
    Map<String, List<String>> reservedAttributes = new HashMap<>();
    reservedAttributes.put("reservedKey", Arrays.asList("reservedValue1"));
    configuration.setReserveSlavesWithAttributes(reservedAttributes);

    Map<String, String> reservedAttributesMap = ImmutableMap.of("reservedKey", "reservedValue1");
    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)));

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String>absent(), reservedAttributesMap)));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)).setRequiredSlaveAttributes(Optional.of(reservedAttributesMap)));

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("reservedKey", "reservedValue1"))));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testAllowedSlaveAttributes() {
    Map<String, List<String>> reservedAttributes = new HashMap<>();
    reservedAttributes.put("reservedKey", Arrays.asList("reservedValue1"));
    configuration.setReserveSlavesWithAttributes(reservedAttributes);

    Map<String, String> allowedAttributes = new HashMap<>();
    allowedAttributes.put("reservedKey", "reservedValue1");

    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)));

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("reservedKey", "reservedValue1"))));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)).setAllowedSlaveAttributes(Optional.of(allowedAttributes)));

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("reservedKey", "reservedValue1"))));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testRequiredSlaveAttributesForRequest() {
    Map<String, String> requiredAttributes = new HashMap<>();
    requiredAttributes.put("requiredKey", "requiredValue1");

    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)).setRequiredSlaveAttributes(Optional.of(requiredAttributes)));

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("requiredKey", "notTheRightValue"))));
    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave2", "host2", Optional.<String>absent(), ImmutableMap.of("notTheRightKey", "requiredValue1"))));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave2", "host2", Optional.<String>absent(), requiredAttributes)));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testMultipleRequiredAttributes() {
    Map<String, String> requiredAttributes = new HashMap<>();
    requiredAttributes.put("requiredKey1", "requiredValue1");
    requiredAttributes.put("requiredKey2", "requiredValue2");

    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)).setRequiredSlaveAttributes(Optional.of(requiredAttributes)));

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("requiredKey1", "requiredValue1"))));
    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave2", "host2", Optional.<String>absent(), ImmutableMap.of("requiredKey1", "requiredValue1", "someotherkey", "someothervalue"))));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave2", "host2", Optional.<String>absent(), requiredAttributes)));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testLBCleanup() {
    initLoadBalancedRequest();
    initFirstDeploy();

    configuration.setLoadBalancerRemovalGracePeriodMillis(10000);

    SingularityTask task = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    saveLoadBalancerState(BaragonRequestState.SUCCESS, task.getTaskId(), LoadBalancerRequestType.ADD);

    statusUpdate(task, TaskState.TASK_FAILED);

    Assert.assertTrue(!taskManager.getLBCleanupTasks().isEmpty());

    testingLbClient.setNextBaragonRequestState(BaragonRequestState.WAITING);

    cleaner.drainCleanupQueue();
    Assert.assertTrue(!taskManager.getLBCleanupTasks().isEmpty());

    Optional<SingularityLoadBalancerUpdate> lbUpdate = taskManager.getLoadBalancerState(task.getTaskId(), LoadBalancerRequestType.REMOVE);

    Assert.assertTrue(lbUpdate.isPresent());
    Assert.assertTrue(lbUpdate.get().getLoadBalancerState() == BaragonRequestState.WAITING);

    testingLbClient.setNextBaragonRequestState(BaragonRequestState.FAILED);

    cleaner.drainCleanupQueue();
    Assert.assertTrue(!taskManager.getLBCleanupTasks().isEmpty());

    lbUpdate = taskManager.getLoadBalancerState(task.getTaskId(), LoadBalancerRequestType.REMOVE);

    Assert.assertTrue(lbUpdate.isPresent());
    Assert.assertTrue(lbUpdate.get().getLoadBalancerState() == BaragonRequestState.FAILED);

    testingLbClient.setNextBaragonRequestState(BaragonRequestState.SUCCESS);

    cleaner.drainCleanupQueue();

    Assert.assertTrue(!taskManager.getLBCleanupTasks().isEmpty());

    configuration.setLoadBalancerRemovalGracePeriodMillis(0);
    cleaner.drainCleanupQueue();

    Assert.assertTrue(taskManager.getLBCleanupTasks().isEmpty());
    lbUpdate = taskManager.getLoadBalancerState(task.getTaskId(), LoadBalancerRequestType.REMOVE);

    Assert.assertTrue(lbUpdate.isPresent());
    Assert.assertTrue(lbUpdate.get().getLoadBalancerState() == BaragonRequestState.SUCCESS);
    Assert.assertTrue(lbUpdate.get().getLoadBalancerRequestId().getAttemptNumber() == 2);
  }

  @Test
  public void testUnpauseOnDeploy() {
    initRequest();
    initFirstDeploy();

    requestManager.pause(request, System.currentTimeMillis(), Optional.<String>absent(), Optional.<String>absent());

    boolean exception = false;

    try {
      deploy("d2");
    } catch (Exception e) {
      exception = true;
    }

    Assert.assertTrue(exception);

    deploy("d3", Optional.of(true));

    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.DEPLOYING_TO_UNPAUSE);

    scheduler.drainPendingQueue(stateCacheProvider.get());
    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1")));
    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_FAILED);

    deployChecker.checkDeploys();

    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.PAUSED);

    Assert.assertTrue(taskManager.getActiveTaskIds().isEmpty());
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());

    deploy("d4", Optional.of(true));

    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.DEPLOYING_TO_UNPAUSE);

    scheduler.drainPendingQueue(stateCacheProvider.get());
    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1")));

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_RUNNING);
    deployChecker.checkDeploys();

    RequestState requestState = requestManager.getRequest(requestId).get().getState();

    Assert.assertTrue(requestState == RequestState.ACTIVE);
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

    Assert.assertTrue(!deployManager.getDeployResult(requestId, deployId).isPresent());

    launchTask(request, deploy, System.currentTimeMillis(), 1, TaskState.TASK_RUNNING);

    deployChecker.checkDeploys();

    Assert.assertEquals(DeployState.SUCCEEDED, deployManager.getDeployResult(requestId, deployId).get().getDeployState());
  }

  @Test
  public void testReconciliation() {
    Assert.assertTrue(!taskReconciliation.isReconciliationRunning());

    configuration.setCheckReconcileWhenRunningEveryMillis(1);

    initRequest();
    initFirstDeploy();

    Assert.assertTrue(taskReconciliation.startReconciliation() == ReconciliationState.STARTED);
    sleep(50);
    Assert.assertTrue(!taskReconciliation.isReconciliationRunning());

    SingularityTask taskOne = launchTask(request, firstDeploy, 1, TaskState.TASK_STARTING);
    SingularityTask taskTwo = launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);

    saveLastActiveTaskStatus(taskOne, Optional.<TaskStatus>absent(), -1000);

    Assert.assertTrue(taskReconciliation.startReconciliation() == ReconciliationState.STARTED);
    Assert.assertTrue(taskReconciliation.startReconciliation() == ReconciliationState.ALREADY_RUNNING);

    sleep(50);
    Assert.assertTrue(taskReconciliation.isReconciliationRunning());

    saveLastActiveTaskStatus(taskOne, Optional.of(buildTaskStatus(taskOne)), +1000);

    sleep(50);
    Assert.assertTrue(taskReconciliation.isReconciliationRunning());

    saveLastActiveTaskStatus(taskTwo, Optional.of(buildTaskStatus(taskTwo)), +1000);

    sleep(50);

    Assert.assertTrue(!taskReconciliation.isReconciliationRunning());
  }


  @Test
  public void testSchedulerPriority() {
    SingularityRequest request1 = buildRequest("request1");
    SingularityRequest request2 = buildRequest("request2");
    SingularityRequest request3 = buildRequest("request3");

    SingularityDeploy deploy1 = initAndFinishDeploy(request1, "r1d1");
    SingularityDeploy deploy2 = initAndFinishDeploy(request2, "r2d2");
    SingularityDeploy deploy3 = initAndFinishDeploy(request3, "r3d3");

    launchTask(request1, deploy1, 2, 1, TaskState.TASK_RUNNING);
    launchTask(request2, deploy2, 1, 1, TaskState.TASK_RUNNING);
    launchTask(request2, deploy2, 10, 1, TaskState.TASK_RUNNING);

    // r3 should have priority (never launched)
    // r1 last launch at 2
    // r2 last launch at 10

    List<SingularityTaskRequest> requests = Arrays.asList(buildTaskRequest(request1, deploy1, 100), buildTaskRequest(request2, deploy2, 101), buildTaskRequest(request3, deploy3, 95));
    schedulerPriority.sortTaskRequestsInPriorityOrder(requests);

    Assert.assertTrue(requests.get(0).getRequest().getId().equals(request3.getId()));
    Assert.assertTrue(requests.get(1).getRequest().getId().equals(request1.getId()));
    Assert.assertTrue(requests.get(2).getRequest().getId().equals(request2.getId()));

    schedulerPriority.notifyTaskLaunched(new SingularityTaskId(request3.getId(), deploy3.getId(), 500, 1, "host", "rack"));

    requests = Arrays.asList(buildTaskRequest(request1, deploy1, 100), buildTaskRequest(request2, deploy2, 101), buildTaskRequest(request3, deploy3, 95));
    schedulerPriority.sortTaskRequestsInPriorityOrder(requests);

    Assert.assertTrue(requests.get(0).getRequest().getId().equals(request1.getId()));
    Assert.assertTrue(requests.get(1).getRequest().getId().equals(request2.getId()));
    Assert.assertTrue(requests.get(2).getRequest().getId().equals(request3.getId()));
  }

  @Test
  public void badPauseExpires() {
    initRequest();

    requestManager.createCleanupRequest(new SingularityRequestCleanup(Optional.<String>absent(), RequestCleanupType.PAUSING, System.currentTimeMillis(),
        Optional.<Boolean>absent(), requestId, Optional.<String>absent(), Optional.<Boolean> absent(), Optional.<String>absent(), Optional.<String>absent()));

    cleaner.drainCleanupQueue();

    Assert.assertTrue(!requestManager.getCleanupRequests().isEmpty());
    configuration.setCleanupEverySeconds(0);

    sleep(1);
    cleaner.drainCleanupQueue();

    Assert.assertTrue(requestManager.getCleanupRequests().isEmpty());
  }

  @Test
  public void testPauseLbCleanup() {
    initLoadBalancedRequest();
    initFirstDeploy();

    requestManager.saveLbCleanupRequest(new SingularityRequestLbCleanup(requestId, Sets.newHashSet("test"), "/basepath", Collections.<String>emptyList(), Optional.<SingularityLoadBalancerUpdate>absent()));

    requestManager.pause(request, System.currentTimeMillis(), Optional.<String>absent(), Optional.<String>absent());

    testingLbClient.setNextBaragonRequestState(BaragonRequestState.WAITING);

    cleaner.drainCleanupQueue();
    Assert.assertTrue(!requestManager.getLbCleanupRequestIds().isEmpty());

    Optional<SingularityLoadBalancerUpdate> lbUpdate = requestManager.getLbCleanupRequest(requestId).get().getLoadBalancerUpdate();

    Assert.assertTrue(lbUpdate.isPresent());
    Assert.assertTrue(lbUpdate.get().getLoadBalancerState() == BaragonRequestState.WAITING);

    testingLbClient.setNextBaragonRequestState(BaragonRequestState.FAILED);

    cleaner.drainCleanupQueue();
    Assert.assertTrue(!requestManager.getLbCleanupRequestIds().isEmpty());

    lbUpdate = requestManager.getLbCleanupRequest(requestId).get().getLoadBalancerUpdate();

    Assert.assertTrue(lbUpdate.isPresent());
    Assert.assertTrue(lbUpdate.get().getLoadBalancerState() == BaragonRequestState.FAILED);

    testingLbClient.setNextBaragonRequestState(BaragonRequestState.SUCCESS);

    cleaner.drainCleanupQueue();
    Assert.assertTrue(requestManager.getLbCleanupRequestIds().isEmpty());
  }

  @Test
  public void testPause() {
    initRequest();
    initFirstDeploy();

    SingularityTask taskOne = startTask(firstDeploy);

    requestResource.pause(requestId, Optional.<SingularityPauseRequest> absent());

    cleaner.drainCleanupQueue();

    Assert.assertEquals(1, taskManager.getKilledTaskIdRecords().size());

    statusUpdate(taskOne, TaskState.TASK_KILLED);

    resourceOffers();

    Assert.assertEquals(0, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(0, taskManager.getPendingTasks().size());
    Assert.assertEquals(RequestState.PAUSED, requestManager.getRequest(requestId).get().getState());
    Assert.assertEquals(requestId, requestManager.getPausedRequests().iterator().next().getRequest().getId());

    requestResource.unpause(requestId, Optional.<SingularityUnpauseRequest> absent());

    resourceOffers();

    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(0, taskManager.getPendingTasks().size());

    Assert.assertEquals(RequestState.ACTIVE, requestManager.getRequest(requestId).get().getState());
    Assert.assertEquals(requestId, requestManager.getActiveRequests().iterator().next().getRequest().getId());
  }

  @Test
  public void testExpiringPause() {
    initRequest();
    initFirstDeploy();

    SingularityTask taskOne = startTask(firstDeploy);

    requestResource.pause(requestId, Optional.of(new SingularityPauseRequest(Optional.<Boolean> absent(), Optional.of(1L), Optional.<String> absent(), Optional.<String>absent())));

    cleaner.drainCleanupQueue();

    Assert.assertEquals(1, taskManager.getKilledTaskIdRecords().size());

    statusUpdate(taskOne, TaskState.TASK_KILLED);

    resourceOffers();

    Assert.assertEquals(0, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(0, taskManager.getPendingTasks().size());
    Assert.assertEquals(RequestState.PAUSED, requestManager.getRequest(requestId).get().getState());
    Assert.assertEquals(requestId, requestManager.getPausedRequests().iterator().next().getRequest().getId());

    try {
      Thread.sleep(2);
    } catch (InterruptedException ie){
    }

    expiringUserActionPoller.runActionOnPoll();

    resourceOffers();

    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(0, taskManager.getPendingTasks().size());

    Assert.assertEquals(RequestState.ACTIVE, requestManager.getRequest(requestId).get().getState());
    Assert.assertEquals(requestId, requestManager.getActiveRequests().iterator().next().getRequest().getId());
  }

  @Test
  public void testBounce() {
    initRequest();

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(3), Optional.<Long> absent(), Optional.<Boolean> absent(), Optional.<String> absent(), Optional.<String>absent()));

    initFirstDeploy();

    SingularityTask taskOne = startTask(firstDeploy, 1);
    SingularityTask taskTwo = startTask(firstDeploy, 2);
    SingularityTask taskThree = startTask(firstDeploy, 3);

    requestResource.bounce(requestId, Optional.<SingularityBounceRequest> absent());

    Assert.assertTrue(requestManager.cleanupRequestExists(requestId));

    cleaner.drainCleanupQueue();

    Assert.assertTrue(!requestManager.cleanupRequestExists(requestId));
    Assert.assertTrue(taskManager.getCleanupTaskIds().size() == 3);

    cleaner.drainCleanupQueue();

    Assert.assertTrue(!requestManager.cleanupRequestExists(requestId));
    Assert.assertTrue(taskManager.getCleanupTaskIds().size() == 3);

    resourceOffers();

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 6);

    cleaner.drainCleanupQueue();

    Assert.assertTrue(taskManager.getCleanupTaskIds().size() == 3);

    for (SingularityTask task : taskManager.getActiveTasks()) {
      if (!task.getTaskId().equals(taskOne.getTaskId()) && !task.getTaskId().equals(taskTwo.getTaskId()) && !task.getTaskId().equals(taskThree.getTaskId())) {
        statusUpdate(task, TaskState.TASK_RUNNING, Optional.of(1L));
      }
    }

    cleaner.drainCleanupQueue();

    Assert.assertTrue(taskManager.getCleanupTaskIds().isEmpty());
    Assert.assertTrue(taskManager.getKilledTaskIdRecords().size() == 3);
  }

  @Test
  public void testExpiringBounceGoesAway() {
    initRequest();
    initFirstDeploy();

    startTask(firstDeploy, 1);

    requestResource.bounce(requestId,
        Optional.of(new SingularityBounceRequest(Optional.of(false), Optional.<Boolean> absent(), Optional.of(1L), Optional.<String> absent(), Optional.of("msg"))));

    cleaner.drainCleanupQueue();
    resourceOffers();
    runLaunchedTasks();
    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assert.assertTrue(taskManager.getCleanupTaskIds().isEmpty());
    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());

    Assert.assertTrue(!requestManager.getExpiringBounce(requestId).isPresent());
  }

  @Test
  public void testExpiringNonIncrementalBounce() {
    initWithTasks(3);

    requestResource.bounce(requestId,
        Optional.of(new SingularityBounceRequest(Optional.<Boolean> absent(), Optional.<Boolean> absent(), Optional.of(1L), Optional.of("aid"), Optional.<String> absent())));

    Assert.assertTrue(!requestManager.getCleanupRequests().get(0).getMessage().isPresent());
    Assert.assertEquals("aid", requestManager.getCleanupRequests().get(0).getActionId().get());

    // creates cleanup tasks:
    cleaner.drainCleanupQueue();

    Assert.assertEquals(1, requestManager.getPendingRequests().size());
    Assert.assertEquals(0, requestManager.getCleanupRequests().size());
    Assert.assertEquals(3, taskManager.getCleanupTaskIds().size());

    // should have 1 pending task and 2 launched

    resourceOffersByNumTasks(2);

    Assert.assertEquals(1, taskManager.getPendingTasks().size());
    Assert.assertEquals(5, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(3, taskManager.getCleanupTaskIds().size());
    Assert.assertEquals(0, requestManager.getPendingRequests().size());
    Assert.assertEquals(0, requestManager.getCleanupRequests().size());

    try {
      Thread.sleep(1);
    } catch (InterruptedException ie) {
    }

    expiringUserActionPoller.runActionOnPoll();

    resourceOffers();
    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assert.assertEquals(3, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(0, taskManager.getPendingTasks().size());
    Assert.assertEquals(0, taskManager.getCleanupTaskIds().size());
    Assert.assertEquals(0, requestManager.getPendingRequests().size());
    Assert.assertEquals(0, requestManager.getCleanupRequests().size());
  }

  @Test
  public void testExpiringIncrementalBounce() {
    initRequest();

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(3), Optional.<Long> absent(), Optional.<Boolean> absent(), Optional.<String> absent(), Optional.<String>absent()));

    initFirstDeploy();

    startTask(firstDeploy, 1);
    startTask(firstDeploy, 2);
    startTask(firstDeploy, 3);

    requestResource.bounce(requestId,
        Optional.of(new SingularityBounceRequest(Optional.of(true), Optional.<Boolean> absent(), Optional.of(1L), Optional.<String> absent(), Optional.of("msg"))));

    Assert.assertTrue(requestManager.cleanupRequestExists(requestId));
    Assert.assertEquals("msg", requestManager.getCleanupRequests().get(0).getMessage().get());
    Assert.assertTrue(requestManager.getCleanupRequests().get(0).getActionId().isPresent());

    String actionId = requestManager.getCleanupRequests().get(0).getActionId().get();

    cleaner.drainCleanupQueue();

    Assert.assertTrue(!requestManager.cleanupRequestExists(requestId));
    Assert.assertTrue(taskManager.getCleanupTaskIds().size() == 3);

    Assert.assertEquals("msg", taskManager.getCleanupTasks().get(0).getMessage().get());
    Assert.assertEquals(actionId, taskManager.getCleanupTasks().get(0).getActionId().get());

    startTask(firstDeploy, 4);
//    launchTask(request, firstDeploy, 5, TaskState.TASK_STARTING);

    cleaner.drainCleanupQueue();

    Assert.assertEquals(1, taskManager.getKilledTaskIdRecords().size());
    Assert.assertEquals(4, taskManager.getActiveTaskIds().size());

    try {
      Thread.sleep(2);
    } catch (InterruptedException ie) {}

    expiringUserActionPoller.runActionOnPoll();

    cleaner.drainCleanupQueue();

    resourceOffers();

    killKilledTasks();

    Assert.assertTrue(!requestManager.getExpiringBounce(requestId).isPresent());
    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 3);
    Assert.assertTrue(!requestManager.cleanupRequestExists(requestId));
    Assert.assertTrue(taskManager.getCleanupTasks().isEmpty());
  }

  @Test
  public void testIncrementalBounceShutsDownOldTasksPerNewHealthyTask() {
    initRequest();

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(3), Optional.<Long> absent(), Optional.<Boolean> absent(), Optional.<String> absent(), Optional.<String>absent()));

    initFirstDeploy();

    startTask(firstDeploy, 1);
    startTask(firstDeploy, 2);
    startTask(firstDeploy, 3);

    requestResource.bounce(requestId,
        Optional.of(new SingularityBounceRequest(Optional.of(true), Optional.<Boolean>absent(), Optional.of(1L), Optional.<String>absent(), Optional.of("msg"))));

    Assert.assertTrue(requestManager.cleanupRequestExists(requestId));

    cleaner.drainCleanupQueue();

    Assert.assertTrue(!requestManager.cleanupRequestExists(requestId));
    Assert.assertEquals(3, taskManager.getCleanupTaskIds().size());

    SingularityTask newTask = launchTask(request, firstDeploy, 5, TaskState.TASK_STARTING);

    cleaner.drainCleanupQueue();

    Assert.assertEquals(0, taskManager.getKilledTaskIdRecords().size());
    Assert.assertEquals(4, taskManager.getActiveTaskIds().size());

    statusUpdate(newTask, TaskState.TASK_RUNNING);

    cleaner.drainCleanupQueue();

    Assert.assertEquals(1, taskManager.getKilledTaskIdRecords().size());
    Assert.assertEquals(4, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testIncrementalBounce() {
    initRequest();

    SingularityRequest request = requestResource.getRequest(requestId).getRequest();

    requestResource.postRequest(request.toBuilder()
        .setSlavePlacement(Optional.of(SlavePlacement.SEPARATE_BY_REQUEST))
        .setInstances(Optional.of(2)).build()
        );

    initHCDeploy();

    SingularityTask taskOne = startSeparatePlacementTask(firstDeploy, 1);
    SingularityTask taskTwo = startSeparatePlacementTask(firstDeploy, 2);

    requestManager.createCleanupRequest(new SingularityRequestCleanup(user, RequestCleanupType.INCREMENTAL_BOUNCE, System.currentTimeMillis(),
        Optional.<Boolean>absent(), requestId, Optional.of(firstDeployId), Optional.<Boolean> absent(), Optional.<String>absent(), Optional.<String>absent()));

    Assert.assertTrue(requestManager.cleanupRequestExists(requestId));

    cleaner.drainCleanupQueue();

    Assert.assertTrue(!requestManager.cleanupRequestExists(requestId));
    Assert.assertEquals(2, taskManager.getCleanupTaskIds().size());

    resourceOffers(3);

    SingularityTask taskThree = null;

    for (SingularityTask task : taskManager.getActiveTasks()) {
      if (!task.getTaskId().equals(taskOne.getTaskId()) && !task.getTaskId().equals(taskTwo.getTaskId())) {
        taskThree = task;
      }
    }

    statusUpdate(taskThree, TaskState.TASK_RUNNING, Optional.of(1L));
    Assert.assertEquals(3, taskManager.getActiveTaskIds().size());

    cleaner.drainCleanupQueue();

    // No old tasks should be killed before new ones pass healthchecks
    Assert.assertEquals(2, taskManager.getCleanupTaskIds().size());
    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(200), Optional.of(1000L), System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent(), taskThree.getTaskId()));

    cleaner.drainCleanupQueue();
    Assert.assertEquals(1, taskManager.getCleanupTaskIds().size());

    statusUpdate(taskOne, TaskState.TASK_KILLED);

    resourceOffers(3);

    SingularityTask taskFour = null;

    for (SingularityTask task : taskManager.getActiveTasks()) {
      if (!task.getTaskId().equals(taskOne.getTaskId()) && !task.getTaskId().equals(taskTwo.getTaskId()) && !task.getTaskId().equals(taskThree.getTaskId())) {
        taskFour = task;
      }
    }

    statusUpdate(taskFour, TaskState.TASK_RUNNING, Optional.of(1L));
    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(200), Optional.of(1000L), System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent(), taskFour.getTaskId()));

    cleaner.drainCleanupQueue();

    Assert.assertTrue(taskManager.getCleanupTaskIds().isEmpty());
  }

  @Test
  public void testScheduledNotification() {
    schedule = "0 0 * * * ?"; // run every hour
    initScheduledRequest();
    initFirstDeploy();

    configuration.setWarnIfScheduledJobIsRunningForAtLeastMillis(Long.MAX_VALUE);
    configuration.setWarnIfScheduledJobIsRunningPastNextRunPct(200);

    final long now = System.currentTimeMillis();

    SingularityTask firstTask = launchTask(request, firstDeploy, now - TimeUnit.HOURS.toMillis(3), 1, TaskState.TASK_RUNNING);

    scheduledJobPoller.runActionOnPoll();

    Mockito.verify(mailer, Mockito.times(0)).sendTaskOverdueMail(Matchers.<Optional<SingularityTask>> any(), Matchers.<SingularityTaskId> any(), Matchers.<SingularityRequest> any(), Matchers.anyLong(), Matchers.anyLong());

    configuration.setWarnIfScheduledJobIsRunningForAtLeastMillis(TimeUnit.HOURS.toMillis(1));

    scheduledJobPoller.runActionOnPoll();

    Mockito.verify(mailer, Mockito.times(1)).sendTaskOverdueMail(Matchers.<Optional<SingularityTask>> any(), Matchers.<SingularityTaskId> any(), Matchers.<SingularityRequest> any(),
      Matchers.anyLong(), Matchers.anyLong());

    scheduledJobPoller.runActionOnPoll();

    Mockito.verify(mailer, Mockito.times(1)).sendTaskOverdueMail(Matchers.<Optional<SingularityTask>> any(), Matchers.<SingularityTaskId> any(), Matchers.<SingularityRequest> any(),
      Matchers.anyLong(), Matchers.anyLong());

    statusUpdate(firstTask, TaskState.TASK_FINISHED);

    Optional<SingularityDeployStatistics> deployStatistics = deployManager.getDeployStatistics(requestId, firstDeployId);

    long oldAvg = deployStatistics.get().getAverageRuntimeMillis().get();

    Assert.assertTrue(deployStatistics.get().getNumTasks() == 1);
    Assert.assertTrue(deployStatistics.get().getAverageRuntimeMillis().get() > 1 && deployStatistics.get().getAverageRuntimeMillis().get() < TimeUnit.DAYS.toMillis(1));

    configuration.setWarnIfScheduledJobIsRunningForAtLeastMillis(1);

    SingularityTask secondTask = launchTask(request, firstDeploy, now - 500, 1, TaskState.TASK_RUNNING);

    scheduledJobPoller.runActionOnPoll();

    Mockito.verify(mailer, Mockito.times(1)).sendTaskOverdueMail(Matchers.<Optional<SingularityTask>> any(), Matchers.<SingularityTaskId> any(), Matchers.<SingularityRequest> any(),
      Matchers.anyLong(), Matchers.anyLong());

    statusUpdate(secondTask, TaskState.TASK_FINISHED);

    deployStatistics = deployManager.getDeployStatistics(requestId, firstDeployId);

    Assert.assertTrue(deployStatistics.get().getNumTasks() == 2);
    Assert.assertTrue(deployStatistics.get().getAverageRuntimeMillis().get() > 1 && deployStatistics.get().getAverageRuntimeMillis().get() < oldAvg);

    saveRequest(request.toBuilder().setScheduledExpectedRuntimeMillis(Optional.of(1L)).build());

    SingularityTask thirdTask = launchTask(request, firstDeploy, now - 502, 1, TaskState.TASK_RUNNING);

    scheduledJobPoller.runActionOnPoll();

    Mockito.verify(mailer, Mockito.times(2)).sendTaskOverdueMail(Matchers.<Optional<SingularityTask>> any(), Matchers.<SingularityTaskId> any(), Matchers.<SingularityRequest> any(),
      Matchers.anyLong(), Matchers.anyLong());

    taskManager.deleteTaskHistory(thirdTask.getTaskId());

    scheduledJobPoller.runActionOnPoll();

    Mockito.verify(mailer, Mockito.times(3)).sendTaskOverdueMail(Matchers.<Optional<SingularityTask>> any(), Matchers.<SingularityTaskId> any(), Matchers.<SingularityRequest> any(),
      Matchers.anyLong(), Matchers.anyLong());
  }

  @Test
  public void testBasicSlaveAndRackState() {
    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 1, "slave1", "host1", Optional.of("rack1"))));
    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 1, "slave2", "host2", Optional.of("rack2"))));
    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 1, "slave1", "host1", Optional.of("rack1"))));

    Assert.assertEquals(1, slaveManager.getHistory("slave1").size());
    Assert.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assert.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);

    Assert.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().equals(slaveManager.getHistory("slave1").get(0)));

    sms.slaveLost(driver, SlaveID.newBuilder().setValue("slave1").build());

    Assert.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 1);
    Assert.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 1);

    Assert.assertTrue(slaveManager.getNumObjectsAtState(MachineState.DEAD) == 1);
    Assert.assertTrue(rackManager.getNumObjectsAtState(MachineState.DEAD) == 1);

    Assert.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getState() == MachineState.DEAD);
    Assert.assertTrue(rackManager.getObject("rack1").get().getCurrentState().getState() == MachineState.DEAD);

    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 1, "slave3", "host3", Optional.of("rack1"))));

    Assert.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assert.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assert.assertTrue(slaveManager.getNumObjectsAtState(MachineState.DEAD) == 1);

    Assert.assertTrue(rackManager.getHistory("rack1").size() == 3);

    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 1, "slave1", "host1", Optional.of("rack1"))));

    Assert.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 3);
    Assert.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);

    sms.slaveLost(driver, SlaveID.newBuilder().setValue("slave1").build());

    Assert.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assert.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assert.assertTrue(slaveManager.getNumObjectsAtState(MachineState.DEAD) == 1);
    Assert.assertTrue(slaveManager.getHistory("slave1").size() == 4);

    sms.slaveLost(driver, SlaveID.newBuilder().setValue("slave1").build());

    Assert.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assert.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assert.assertTrue(slaveManager.getNumObjectsAtState(MachineState.DEAD) == 1);
    Assert.assertTrue(slaveManager.getHistory("slave1").size() == 4);

    slaveManager.deleteObject("slave1");

    Assert.assertTrue(slaveManager.getNumObjectsAtState(MachineState.DEAD) == 0);
    Assert.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assert.assertTrue(slaveManager.getHistory("slave1").isEmpty());
  }

  @Test
  public void testDecommissioning() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)));

    scheduler.drainPendingQueue(stateCacheProvider.get());

    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave1", "host1", Optional.of("rack1"))));
    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave2", "host2", Optional.of("rack1"))));
    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave3", "host3", Optional.of("rack2"))));
    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave4", "host4", Optional.of("rack2"))));


    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }
    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }

    Assert.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assert.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 4);

    Assert.assertTrue(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size() == 1);
    Assert.assertTrue(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size() == 1);
    Assert.assertTrue(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave3").get()).isEmpty());
    Assert.assertTrue(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave4").get()).isEmpty());

    Assert.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.STARTING_DECOMMISSION, Optional.<String> absent(), Optional.of("user1")));
    Assert.assertEquals(StateChangeResult.FAILURE_ALREADY_AT_STATE, slaveManager.changeState("slave1", MachineState.STARTING_DECOMMISSION, Optional.<String> absent(), Optional.of("user1")));
    Assert.assertEquals(StateChangeResult.FAILURE_NOT_FOUND, slaveManager.changeState("slave9231", MachineState.STARTING_DECOMMISSION, Optional.<String> absent(), Optional.of("user1")));

    Assert.assertEquals(MachineState.STARTING_DECOMMISSION, slaveManager.getObject("slave1").get().getCurrentState().getState());
    Assert.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getUser().get().equals("user1"));

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(3)));

    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave1", "host1", Optional.of("rack1"))));

    Assert.assertTrue(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size() == 1);

    Assert.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getState() == MachineState.DECOMMISSIONING);
    Assert.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getUser().get().equals("user1"));

    cleaner.drainCleanupQueue();

    Assert.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getState() == MachineState.DECOMMISSIONING);
    Assert.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getUser().get().equals("user1"));

    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave4", "host4", Optional.of("rack2"))));
    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave3", "host3", Optional.of("rack2"))));

    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave4").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }
    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave3").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }

    // all task should have moved.

    cleaner.drainCleanupQueue();

    Assert.assertTrue(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave4").get()).size() == 1);
    Assert.assertTrue(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave3").get()).size() == 1);
    Assert.assertEquals(1, taskManager.getKilledTaskIdRecords().size());

    // kill the task
    statusUpdate(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).get(0), TaskState.TASK_KILLED);

    Assert.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getState() == MachineState.DECOMMISSIONED);
    Assert.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getUser().get().equals("user1"));

    // let's DECOMMission rack2
    Assert.assertEquals(StateChangeResult.SUCCESS, rackManager.changeState("rack2", MachineState.STARTING_DECOMMISSION, Optional.<String> absent(), Optional.of("user2")));

    // it shouldn't place any on here, since it's DECOMMissioned
    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave1", "host1", Optional.of("rack1"))));

    Assert.assertEquals(0, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());

    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave1", "host1", Optional.of("rack1"))));

    Assert.assertEquals(0, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());

    slaveResource.activateSlave("slave1", Optional.<SingularityMachineChangeRequest> absent());

    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave1", "host1", Optional.of("rack1"))));

    Assert.assertEquals(1, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());

    Assert.assertTrue(rackManager.getObject("rack2").get().getCurrentState().getState() == MachineState.DECOMMISSIONING);

    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave2", "host2", Optional.of("rack1"))));

    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }
    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }

    cleaner.drainCleanupQueue();

    // kill the tasks
    statusUpdate(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave3").get()).get(0), TaskState.TASK_KILLED);

    Assert.assertTrue(rackManager.getObject("rack2").get().getCurrentState().getState() == MachineState.DECOMMISSIONING);

    statusUpdate(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave4").get()).get(0), TaskState.TASK_KILLED);

    Assert.assertTrue(rackManager.getObject("rack2").get().getCurrentState().getState() == MachineState.DECOMMISSIONED);

  }

  @Test
  public void testTaskOddities() {
    // test unparseable status update
    TaskStatus.Builder bldr = TaskStatus.newBuilder()
        .setTaskId(TaskID.newBuilder().setValue("task"))
        .setSlaveId(SlaveID.newBuilder().setValue("slave1"))
        .setState(TaskState.TASK_RUNNING);

    // should not throw exception:
    sms.statusUpdate(driver, bldr.build());

    initRequest();
    initFirstDeploy();

    SingularityTask taskOne = launchTask(request, firstDeploy, 1, TaskState.TASK_STARTING);

    taskManager.deleteTaskHistory(taskOne.getTaskId());

    Assert.assertTrue(taskManager.isActiveTask(taskOne.getTaskId().getId()));

    statusUpdate(taskOne, TaskState.TASK_RUNNING);
    statusUpdate(taskOne, TaskState.TASK_FAILED);

    Assert.assertTrue(!taskManager.isActiveTask(taskOne.getTaskId().getId()));

    Assert.assertEquals(2, taskManager.getTaskHistoryUpdates(taskOne.getTaskId()).size());
  }

  @Test
  public void testOnDemandTasksPersist() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId, RequestType.ON_DEMAND);
    requestResource.postRequest(bldr.build());
    deploy("d2");
    deployChecker.checkDeploys();

    requestResource.scheduleImmediately(requestId);

    resourceOffers();

    requestResource.scheduleImmediately(requestId);

    resourceOffers();

    Assert.assertEquals(2, taskManager.getActiveTaskIds().size());

    requestResource.scheduleImmediately(requestId);

    scheduler.drainPendingQueue(stateCacheProvider.get());

    requestResource.scheduleImmediately(requestId);

    scheduler.drainPendingQueue(stateCacheProvider.get());

    Assert.assertEquals(2, taskManager.getPendingTaskIds().size());

    resourceOffers();

    Assert.assertEquals(4, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testEmptyDecommissioning() {
    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave1", "host1", Optional.of("rack1"))));

    Assert.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.STARTING_DECOMMISSION, Optional.<String> absent(), Optional.of("user1")));

    scheduler.drainPendingQueue(stateCacheProvider.get());
    sms.resourceOffers(driver, Arrays.asList(createOffer(1, 129, "slave1", "host1", Optional.of("rack1"))));

    Assert.assertEquals(MachineState.DECOMMISSIONED, slaveManager.getObject("slave1").get().getCurrentState().getState());
  }

  @Test
  public void testJobRescheduledWhenItFinishesDuringDecommission() {
    initScheduledRequest();
    initFirstDeploy();

    resourceOffers();

    SingularityTask task = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    slaveManager.changeState("slave1", MachineState.STARTING_DECOMMISSION, Optional.<String> absent(), Optional.of("user1"));

    cleaner.drainCleanupQueue();
    resourceOffers();
    cleaner.drainCleanupQueue();

    statusUpdate(task, TaskState.TASK_FINISHED);

    Assert.assertTrue(!taskManager.getPendingTaskIds().isEmpty());
  }

  @Test
  public void testScaleDownTakesHighestInstances() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(5)));

    resourceOffers();

    Assert.assertEquals(5, taskManager.getActiveTaskIds().size());

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(2), Optional.<Long> absent(), Optional.<Boolean> absent(),
      Optional.<String> absent(), Optional.<String> absent()));

    resourceOffers();
    cleaner.drainCleanupQueue();

    Assert.assertEquals(3, taskManager.getKilledTaskIdRecords().size());

    for (SingularityKilledTaskIdRecord taskId : taskManager.getKilledTaskIdRecords()) {
      Assert.assertTrue(taskId.getTaskId().getInstanceNo() > 2);

      scheduler.drainPendingQueue(stateCacheProvider.get());
    }

  }

  @Test
  public void testExpiringScale() {
    initRequest();
    initFirstDeploy();

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(5), Optional.of(1L), Optional.<Boolean> absent(), Optional.<String> absent(), Optional.<String> absent()));

    try {
      Thread.sleep(2);
    } catch (InterruptedException e) {

    }

    expiringUserActionPoller.runActionOnPoll();

    resourceOffers();
    resourceOffers();
    resourceOffers();
    resourceOffers();

    Assert.assertEquals(1, taskManager.getNumActiveTasks());
  }

  @Test
  public void testExpiringSkipHealthchecks() {
    initRequest();
    initHCDeploy();

    SingularityTask firstTask = startTask(firstDeploy);

    Assert.assertTrue(healthchecker.cancelHealthcheck(firstTask.getTaskId().getId()));

    requestResource.skipHealthchecks(requestId, new SingularitySkipHealthchecksRequest(Optional.of(true), Optional.of(1L), Optional.<String> absent(), Optional.<String> absent()));

    statusUpdate(firstTask, TaskState.TASK_FAILED);

    SingularityTask secondTask = startTask(firstDeploy);

    Assert.assertFalse(healthchecker.cancelHealthcheck(secondTask.getTaskId().getId()));

    statusUpdate(secondTask, TaskState.TASK_FAILED);

    expiringUserActionPoller.runActionOnPoll();

    SingularityTask thirdTask = startTask(firstDeploy);

    Assert.assertTrue(healthchecker.cancelHealthcheck(thirdTask.getTaskId().getId()));
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

    requestResource.bounce(requestId, Optional.of(new SingularityBounceRequest(Optional.<Boolean> absent(), Optional.of(true), Optional.<Long> absent(), Optional.<String> absent(), Optional.<String>absent())));

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

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(Optional.of(200), Optional.of(1000L), System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent(), secondTask.getTaskId()));

    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assert.assertEquals(0, taskManager.getNumCleanupTasks());
    Assert.assertEquals(1, taskManager.getNumActiveTasks());
  }

  @Test
  public void testWaitAfterTaskWorks() {
    initRequest();
    initFirstDeploy();

    SingularityTask task = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    statusUpdate(task, TaskState.TASK_FAILED);

    Assert.assertTrue(taskManager.getPendingTaskIds().get(0).getNextRunAt() - System.currentTimeMillis() < 1000L);

    resourceOffers();

    long extraWait = 100000L;

    saveAndSchedule(request.toBuilder().setWaitAtLeastMillisAfterTaskFinishesForReschedule(Optional.of(extraWait)).setInstances(Optional.of(2)));
    resourceOffers();

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_FAILED);

    Assert.assertTrue(taskManager.getPendingTaskIds().get(0).getNextRunAt() - System.currentTimeMillis() > 1000L);
    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testFrozenSlaveTransitions() {
    initRequest();
    initFirstDeploy();

    resourceOffers();

    // test transitions out of frozen
    Assert.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.FROZEN, Optional.<String> absent(), Optional.of("user1")));
    Assert.assertEquals(StateChangeResult.FAILURE_ALREADY_AT_STATE, slaveManager.changeState("slave1", MachineState.FROZEN, Optional.<String> absent(), Optional.of("user1")));
    Assert.assertEquals(StateChangeResult.FAILURE_ILLEGAL_TRANSITION, slaveManager.changeState("slave1", MachineState.DECOMMISSIONING, Optional.<String> absent(), Optional.of("user1")));
    Assert.assertEquals(StateChangeResult.FAILURE_ILLEGAL_TRANSITION, slaveManager.changeState("slave1", MachineState.DECOMMISSIONED, Optional.<String> absent(), Optional.of("user1")));
    Assert.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.ACTIVE, Optional.<String> absent(), Optional.of("user1")));

    // test transitions into frozen
    Assert.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave2", MachineState.STARTING_DECOMMISSION, Optional.<String> absent(), Optional.of("user2")));
    Assert.assertEquals(StateChangeResult.FAILURE_ILLEGAL_TRANSITION, slaveManager.changeState("slave2", MachineState.FROZEN, Optional.<String> absent(), Optional.of("user2")));
    Assert.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave2", MachineState.DECOMMISSIONING, Optional.<String> absent(), Optional.of("user2")));
    Assert.assertEquals(StateChangeResult.FAILURE_ILLEGAL_TRANSITION, slaveManager.changeState("slave2", MachineState.FROZEN, Optional.<String> absent(), Optional.of("user2")));
    Assert.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave2", MachineState.DECOMMISSIONED, Optional.<String> absent(), Optional.of("user2")));
    Assert.assertEquals(StateChangeResult.FAILURE_ILLEGAL_TRANSITION, slaveManager.changeState("slave2", MachineState.FROZEN, Optional.<String> absent(), Optional.of("user2")));
    Assert.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave2", MachineState.ACTIVE, Optional.<String> absent(), Optional.of("user2")));
    Assert.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave2", MachineState.FROZEN, Optional.<String> absent(), Optional.of("user2")));
  }

  @Test
  public void testFrozenSlaveDoesntLaunchTasks() {
    initRequest();
    initFirstDeploy();

    resourceOffers();

    Assert.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.FROZEN, Optional.<String> absent(), Optional.of("user1")));

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)));

    resourceOffers();

    Assert.assertEquals(0, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());
    Assert.assertEquals(2, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size());
  }

  @Test
  public void testUnfrozenSlaveLaunchesTasks() {
    initRequest();
    initFirstDeploy();

    resourceOffers();

    Assert.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.FROZEN, Optional.<String> absent(), Optional.of("user1")));

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)).setSlavePlacement(Optional.of(SlavePlacement.SEPARATE)));

    resourceOffers();

    Assert.assertEquals(0, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());
    Assert.assertEquals(1, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size());

    Assert.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.ACTIVE, Optional.<String> absent(), Optional.of("user1")));

    resourceOffers();

    Assert.assertEquals(1, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());
    Assert.assertEquals(1, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size());
  }

  @Test
  public void testFrozenSlaveCanBeDecommissioned() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)));

    resourceOffers();

    // freeze slave1
    Assert.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.FROZEN, Optional.<String> absent(), Optional.of("user1")));

    // mark tasks as running
    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }
    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }

    // assert Request is spread over the two slaves
    Assert.assertEquals(1, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());
    Assert.assertEquals(1, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size());

    // decommission frozen slave1
    Assert.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.STARTING_DECOMMISSION, Optional.<String> absent(), Optional.of("user1")));

    resourceOffers();
    cleaner.drainCleanupQueue();

    // assert slave1 is decommissioning
    Assert.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getState() == MachineState.DECOMMISSIONING);

    // mark tasks as running
    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }

    // all tasks should have moved
    cleaner.drainCleanupQueue();

    // kill decommissioned task
    statusUpdate(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).get(0), TaskState.TASK_KILLED);

    // assert all tasks on slave2 + slave1 is decommissioned
    Assert.assertEquals(0, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());
    Assert.assertEquals(2, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size());
    Assert.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getState() == MachineState.DECOMMISSIONED);
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

    Assert.assertEquals(DeployState.OVERDUE, deployManager.getDeployResult(requestId, deployId).get().getDeployState());
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

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(Optional.of(500), Optional.of(1000L), System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent(), task.getTaskId()));
    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(Optional.of(500), Optional.of(1000L), System.currentTimeMillis() + 1, Optional.<String> absent(), Optional.<String> absent(), task.getTaskId()));

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
  public void testMaxTasksPerOffer() {
    configuration.setMaxTasksPerOffer(3);

    initRequest();
    initFirstDeploy();

    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(20)).build());
    scheduler.drainPendingQueue(stateCacheProvider.get());

    sms.resourceOffers(driver, Arrays.asList(createOffer(36, 12024)));

    Assert.assertTrue(taskManager.getActiveTasks().size() == 3);

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1"), createOffer(20, 20000, "slave2", "host2")));

    Assert.assertTrue(taskManager.getActiveTasks().size() == 9);

    configuration.setMaxTasksPerOffer(0);

    resourceOffers();

    Assert.assertTrue(taskManager.getActiveTasks().size() == 20);
  }

  @Test
  public void testRequestedPorts() {
    final SingularityDeployBuilder deployBuilder = dockerDeployWithPorts(3);

    initRequest();
    initAndFinishDeploy(request, deployBuilder);
    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(2)).build());
    scheduler.drainPendingQueue(stateCacheProvider.get());

    String[] portRangeWithNoRequestedPorts = {"65:70"};
    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String> absent(), Collections.<String, String>emptyMap(), portRangeWithNoRequestedPorts)));
    Assert.assertEquals(0, taskManager.getActiveTasks().size());

    String[] portRangeWithSomeRequestedPorts = {"80:82"};
    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String> absent(), Collections.<String, String>emptyMap(), portRangeWithSomeRequestedPorts)));
    Assert.assertEquals(0, taskManager.getActiveTasks().size());

    String[] portRangeWithRequestedButNotEnoughPorts = {"80:80", "8080:8080"};
    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String> absent(), Collections.<String, String>emptyMap(), portRangeWithRequestedButNotEnoughPorts)));
    Assert.assertEquals(0, taskManager.getActiveTasks().size());

    String[] portRangeWithNeededPorts = {"80:83", "8080:8080"};
    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String> absent(), Collections.<String, String>emptyMap(), portRangeWithNeededPorts)));
    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());
  }

  private SingularityDeployBuilder dockerDeployWithPorts(int numPorts) {
    final SingularityDockerPortMapping literalMapping = new SingularityDockerPortMapping(Optional.<SingularityPortMappingType>absent(), 80, Optional.of(SingularityPortMappingType.LITERAL), 8080, Optional.<String>absent());
    final SingularityDockerPortMapping offerMapping = new SingularityDockerPortMapping(Optional.<SingularityPortMappingType>absent(), 81, Optional.of(SingularityPortMappingType.FROM_OFFER), 0, Optional.of("udp"));
    final SingularityContainerInfo containerInfo = new SingularityContainerInfo(
      SingularityContainerType.DOCKER,
      Optional.<List<SingularityVolume>>absent(),
      Optional.of(
        new SingularityDockerInfo("docker-image",
          true,
          SingularityDockerNetworkType.BRIDGE,
          Optional.of(Arrays.asList(literalMapping, offerMapping)),
          Optional.of(false),
          Optional.<Map<String, String>>of(ImmutableMap.of("env", "var=value"))
        )));
    final SingularityDeployBuilder deployBuilder = new SingularityDeployBuilder(requestId, "test-docker-ports-deploy");
    deployBuilder.setContainerInfo(Optional.of(containerInfo)).setResources(Optional.of(new Resources(1, 64, numPorts, 0)));
    return deployBuilder;
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

  @Test
  public void testQueueMultipleOneOffs() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId, RequestType.ON_DEMAND);
    requestResource.postRequest(bldr.build());
    deploy("on_demand_deploy");
    deployChecker.checkDeploys();


    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, "on_demand_deploy", System.currentTimeMillis(), Optional.<String>absent(), PendingType.ONEOFF,
        Optional.<List<String>>absent(), Optional.<String>absent(), Optional.<Boolean>absent(), Optional.<String>absent(), Optional.<String>absent()));
    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, "on_demand_deploy", System.currentTimeMillis(), Optional.<String>absent(), PendingType.ONEOFF,
        Optional.<List<String>>absent(), Optional.<String>absent(), Optional.<Boolean>absent(), Optional.<String>absent(), Optional.<String>absent()));

    scheduler.drainPendingQueue(stateCacheProvider.get());

    Assert.assertEquals(2, taskManager.getPendingTaskIds().size());
  }

  @Test
  public void testCronScheduleChanges() throws Exception {
    final String requestId = "test-change-cron";
    final String oldSchedule = "*/5 * * * *";
    final String oldScheduleQuartz = "0 */5 * * * ?";
    final String newSchedule = "*/30 * * * *";
    final String newScheduleQuartz = "0 */30 * * * ?";

    SingularityRequest request = new SingularityRequestBuilder(requestId, RequestType.SCHEDULED)
        .setSchedule(Optional.of(oldSchedule))
        .build();

    request = validator.checkSingularityRequest(request, Optional.<SingularityRequest>absent(), Optional.<SingularityDeploy>absent(), Optional.<SingularityDeploy>absent());

    saveRequest(request);

    Assert.assertEquals(oldScheduleQuartz, requestManager.getRequest(requestId).get().getRequest().getQuartzScheduleSafe());

    initAndFinishDeploy(request, "1");

    scheduler.drainPendingQueue(stateCacheProvider.get());

    final SingularityRequest newRequest = request.toBuilder()
        .setSchedule(Optional.of(newSchedule))
        .setQuartzSchedule(Optional.<String>absent())
        .build();

    final SingularityDeploy newDeploy = new SingularityDeployBuilder(request.getId(), "2").setCommand(Optional.of("sleep 100")).build();

    deployResource.deploy(new SingularityDeployRequest(newDeploy, Optional.<Boolean>absent(), Optional.<String>absent(), Optional.of(newRequest)));

    deployChecker.checkDeploys();

    scheduler.drainPendingQueue(stateCacheProvider.get());

    Assert.assertEquals(newScheduleQuartz, requestManager.getRequest(requestId).get().getRequest().getQuartzScheduleSafe());
  }

  @Test
  public void testUsesNewRequestDataFromPendingDeploy() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)));

    scheduler.drainPendingQueue(stateCacheProvider.get());

    Assert.assertEquals(2, taskManager.getPendingTaskIds().size());
    Assert.assertEquals(2, requestManager.getRequest(requestId).get().getRequest().getInstancesSafe());

    SingularityRequest request = requestResource.getRequest(requestId).getRequest();
    SingularityRequest newRequest = request.toBuilder().setInstances(Optional.of(1)).build();

    String deployId = "test_new_request_data";
    SingularityDeploy deploy = new SingularityDeployBuilder(request.getId(), deployId).setCommand(Optional.of("sleep 100")).build();

    deployResource.deploy(new SingularityDeployRequest(deploy, Optional.<Boolean>absent(), Optional.<String>absent(), Optional.of(newRequest)));

    deployChecker.checkDeploys();
    scheduler.drainPendingQueue(stateCacheProvider.get());

    List<SingularityPendingTaskId> pendingTaskIdsForNewDeploy = new ArrayList<>();
    for (SingularityPendingTaskId pendingTaskId : taskManager.getPendingTaskIds()) {
      if (pendingTaskId.getDeployId().equals(deployId)) {
        pendingTaskIdsForNewDeploy.add(pendingTaskId);
      }
    }

    Assert.assertEquals(1, pendingTaskIdsForNewDeploy.size());
    Assert.assertEquals(2, requestManager.getRequest(requestId).get().getRequest().getInstancesSafe());

    resourceOffers();
    for (SingularityTaskId taskId : taskManager.getActiveTaskIdsForDeploy(requestId, deployId)) {
      statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_RUNNING);
    }
    deployChecker.checkDeploys();

    Assert.assertEquals(1, requestManager.getRequest(requestId).get().getRequest().getInstancesSafe());
  }

  @Test(expected = ConflictException.class)
  public void testCannotUpdateRequestDuringPendingDeployWithNewData() {
    initRequest();
    SingularityRequest request = requestResource.getRequest(requestId).getRequest();
    SingularityRequest newRequest = request.toBuilder().setInstances(Optional.of(1)).build();

    String deployId = "test_new_request_data";
    SingularityDeploy deploy = new SingularityDeployBuilder(request.getId(), deployId).setCommand(Optional.of("sleep 100")).build();

    deployResource.deploy(new SingularityDeployRequest(deploy, Optional.<Boolean> absent(), Optional.<String> absent(), Optional.of(newRequest)));

    requestResource.postRequest(newRequest);
  }

  @Test
  public void testObsoletePendingRequestsRemoved() {
    initRequest();
    initFirstDeploy();
    SingularityTask taskOne = startTask(firstDeploy);
    requestResource.pause(requestId, Optional.<SingularityPauseRequest> absent());
    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, firstDeployId, System.currentTimeMillis(), Optional.<String>absent(), PendingType.NEW_DEPLOY, Optional.<Boolean>absent(), Optional.<String>absent()));

    Assert.assertEquals(requestManager.getPendingRequests().size(), 1);
    scheduler.drainPendingQueue(stateCacheProvider.get());

    Assert.assertEquals(requestManager.getPendingRequests().size(), 0);
  }

}
