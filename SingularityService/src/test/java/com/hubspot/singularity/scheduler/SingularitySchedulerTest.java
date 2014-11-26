package com.hubspot.singularity.scheduler;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.TaskStatus;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestInstances;
import com.hubspot.singularity.SingularitySchedulerTestBase;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.scheduler.SingularityTaskReconciliation.ReconciliationState;

public class SingularitySchedulerTest extends SingularitySchedulerTestBase {

  @Test
  public void testSchedulerIsolatesPendingTasksBasedOnDeploy() {
    initRequest();
    initFirstDeploy();
    initSecondDeploy();

    SingularityPendingTask p1 = new SingularityPendingTask(new SingularityPendingTaskId(requestId, firstDeployId, System.currentTimeMillis(), 1, PendingType.ONEOFF, System.currentTimeMillis()), Optional.<String> absent());
    SingularityPendingTask p2 = new SingularityPendingTask(new SingularityPendingTaskId(requestId, firstDeployId, System.currentTimeMillis(), 1, PendingType.TASK_DONE, System.currentTimeMillis()), Optional.<String> absent());
    SingularityPendingTask p3 = new SingularityPendingTask(new SingularityPendingTaskId(requestId, secondDeployId, System.currentTimeMillis(), 1, PendingType.TASK_DONE, System.currentTimeMillis()), Optional.<String> absent());

    List<SingularityPendingTask> pendingTasks = Arrays.asList(p1, p2, p3);

    taskManager.createPendingTasks(pendingTasks);

    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, secondDeployId, System.currentTimeMillis(), PendingType.NEW_DEPLOY));

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
    SingularityPendingTask taskOne = new SingularityPendingTask(taskIdOne, Optional.<String> absent());

    SingularityPendingTaskId taskIdTwo = new SingularityPendingTaskId(requestId, firstDeployId, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1), 2, PendingType.IMMEDIATE, System.currentTimeMillis());
    SingularityPendingTask taskTwo = new SingularityPendingTask(taskIdTwo, Optional.<String> absent());

    SingularityPendingTaskId taskIdThree = new SingularityPendingTaskId(requestId, secondDeployId, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3), 1, PendingType.IMMEDIATE, System.currentTimeMillis());
    SingularityPendingTask taskThree = new SingularityPendingTask(taskIdThree, Optional.<String> absent());

    SingularityPendingTaskId taskIdFour = new SingularityPendingTaskId(requestId + "hi", firstDeployId, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3), 5, PendingType.IMMEDIATE, System.currentTimeMillis());
    SingularityPendingTask taskFour = new SingularityPendingTask(taskIdFour, Optional.<String> absent());

    taskManager.createPendingTasks(Arrays.asList(taskOne, taskTwo, taskThree, taskFour));

    launchTask(request, secondDeploy, 1, TaskState.TASK_RUNNING);

    deployChecker.checkDeploys();

    List<SingularityPendingTaskId> pendingTaskIds = taskManager.getPendingTaskIds();

    Assert.assertTrue(!pendingTaskIds.contains(taskIdOne));
    Assert.assertTrue(!pendingTaskIds.contains(taskIdTwo));

    Assert.assertTrue(pendingTaskIds.contains(taskIdThree));
    Assert.assertTrue(pendingTaskIds.contains(taskIdFour));
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

    requestResource.pause(requestId, Optional.<String> absent(), Optional.of(new SingularityPauseRequest(Optional.of("testuser"), Optional.of(false))));

    cleaner.drainCleanupQueue();

    Assert.assertTrue(taskManager.getKilledTaskIdRecords().isEmpty());
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assert.assertTrue(requestManager.getCleanupRequests().isEmpty());

    statusUpdate(firstTask, TaskState.TASK_FINISHED);

    // make sure something new isn't scheduled!
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
  }

  @Test
  public void testKilledTaskIdRecords() {
    initScheduledRequest();
    initFirstDeploy();

    SingularityTask firstTask = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    requestResource.deleteRequest(requestId, Optional.<String> absent());

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

    requestManager.activate(request.toBuilder().setKillOldNonLongRunningTasksAfterMillis(Optional.<Long> of(0L)).build(), RequestHistoryType.CREATED, System.currentTimeMillis(), Optional.<String> absent());

    cleaner.drainCleanupQueue();

    Assert.assertTrue(!taskManager.getKilledTaskIdRecords().isEmpty());
    Assert.assertTrue(taskManager.getCleanupTasks().isEmpty());
  }

  @Test
  public void testSchedulerCanBatchOnOffers() {
    initRequest();
    initFirstDeploy();

    requestResource.submit(request.toBuilder().setInstances(Optional.of(3)).build(), Optional.<String> absent());
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

    requestResource.submit(request.toBuilder().setInstances(Optional.of(10)).build(), Optional.<String> absent());
    scheduler.drainPendingQueue(stateCacheProvider.get());

    sms.resourceOffers(driver, Arrays.asList(createOffer(2, 1024), createOffer(1, 1024)));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 3);
    Assert.assertTrue(taskManager.getPendingTaskIds().size() == 7);
  }

  @Test
  public void testSchedulerRandomizesOffers() {
    initRequest();
    initFirstDeploy();

    requestResource.submit(request.toBuilder().setInstances(Optional.of(15)).build(), Optional.<String> absent());
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
    requestResource.submit(request.toBuilder().setQuartzSchedule(Optional.of(schedule)).build(), Optional.<String> absent());
    scheduler.drainPendingQueue(stateCacheProvider.get());

    Assert.assertTrue(requestResource.getActiveRequests().isEmpty());
    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.FINISHED);
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());

    schedule = "*/1 * * * * ?";
    requestResource.submit(request.toBuilder().setQuartzSchedule(Optional.of(schedule)).build(), Optional.<String> absent());
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
    requestId = "oneoffRequest";
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId);
    bldr.setDaemon(Optional.of(Boolean.FALSE));
    requestResource.submit(bldr.build(), Optional.<String> absent());
    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());

    deploy("d2");
    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());

    deployChecker.checkDeploys();

    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());

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

    requestManager.activate(request.toBuilder().setInstances(Optional.of(4)).build(), RequestHistoryType.CREATED, System.currentTimeMillis(), Optional.<String> absent());

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

    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 3);
  }

  @Test
  public void testSlavePlacementOptimisticSingleOffer() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(3)).setSlavePlacement(Optional.of(SlavePlacement.OPTIMISTIC)));

    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1"), createOffer(20, 20000, "slave2", "host2")));

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
  public void testLBCleanup() {
    initLoadBalancedRequest();
    initFirstDeploy();

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
    Assert.assertTrue(taskManager.getLBCleanupTasks().isEmpty());
    lbUpdate = taskManager.getLoadBalancerState(task.getTaskId(), LoadBalancerRequestType.REMOVE);

    Assert.assertTrue(lbUpdate.isPresent());
    Assert.assertTrue(lbUpdate.get().getLoadBalancerState() == BaragonRequestState.SUCCESS);
    Assert.assertTrue(lbUpdate.get().getLoadBalancerRequestId().getAttemptNumber() == 2);

  }

  @Test
  public void testUnpauseoOnDeploy() {
    initRequest();
    initFirstDeploy();

    requestManager.pause(request, System.currentTimeMillis(), Optional.<String> absent());

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

    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.ACTIVE);
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

    saveLastActiveTaskStatus(taskOne, Optional.<TaskStatus> absent(), -1000);

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

    SingularityDeploy deploy1 = initDeploy(request1, "r1d1");
    SingularityDeploy deploy2 = initDeploy(request2, "r2d2");
    SingularityDeploy deploy3 = initDeploy(request3, "r3d3");

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

    requestManager.createCleanupRequest(new SingularityRequestCleanup(Optional.<String> absent(), RequestCleanupType.PAUSING, System.currentTimeMillis(), Optional.<Boolean> absent(), requestId, Optional.<String> absent()));

    cleaner.drainCleanupQueue();

    Assert.assertTrue(!requestManager.getCleanupRequests().isEmpty());

    configuration.setCleanupEverySeconds(0);

    sleep(1);
    cleaner.drainCleanupQueue();

    Assert.assertTrue(requestManager.getCleanupRequests().isEmpty());
  }

  @Test
  public void testBounce() {
    initRequest();

    requestResource.updateInstances(requestId, Optional.<String> absent(), new SingularityRequestInstances(requestId, Optional.of(3)));

    initFirstDeploy();

    SingularityTask taskOne = startTask(firstDeploy, 1);
    SingularityTask taskTwo = startTask(firstDeploy, 2);
    SingularityTask taskThree = startTask(firstDeploy, 3);

    requestResource.bounce(requestId, user);

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

}
