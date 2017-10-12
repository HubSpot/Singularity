package com.hubspot.singularity.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskState;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.mesos.MesosUtils;
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
import com.hubspot.singularity.ScheduleType;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityPriorityFreezeParent;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestLbCleanup;
import com.hubspot.singularity.SingularityShellCommand;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.api.SingularityDeleteRequestRequest;
import com.hubspot.singularity.api.SingularityDeployRequest;
import com.hubspot.singularity.api.SingularityKillTaskRequest;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityPriorityFreeze;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.api.SingularityUnpauseRequest;
import com.hubspot.singularity.data.AbstractMachineManager.StateChangeResult;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.mesos.OfferCache;
import com.hubspot.singularity.mesos.SingularityMesosTaskPrioritizer;
import com.hubspot.singularity.scheduler.SingularityDeployHealthHelper.DeployHealth;
import com.hubspot.singularity.scheduler.SingularityTaskReconciliation.ReconciliationState;
import com.jayway.awaitility.Awaitility;

public class SingularitySchedulerTest extends SingularitySchedulerTestBase {
  @Inject
  private SingularityValidator validator;

  @Inject
  private SingularityDeployHealthHelper deployHealthHelper;

  @Inject
  private SingularityMesosTaskPrioritizer taskPrioritizer;

  @Inject
  private SingularitySchedulerPoller schedulerPoller;

  @Inject
  private OfferCache offerCache;

  public SingularitySchedulerTest() {
    super(false);
  }

  private SingularityPendingTask pendingTask(String requestId, String deployId, PendingType pendingType) {
    return new SingularityPendingTask(new SingularityPendingTaskId(requestId, deployId, System.currentTimeMillis(), 1, pendingType, System.currentTimeMillis()),
        Optional.<List<String>> absent(), Optional.<String> absent(), Optional.<String> absent(), Optional.<Boolean> absent(), Optional.<String> absent(), Optional.<Resources>absent(), Optional.<String>absent());
  }

  @Test
  public void testOfferCacheRescindOffers() {
    configuration.setCacheOffers(true);
    configuration.setOfferCacheSize(2);

    List<Offer> offers2 = resourceOffers(); // cached as well

    sms.rescind(offers2.get(0).getId());
    sms.rescind(offers2.get(1).getId());

    initRequest();
    initFirstDeploy();

    requestResource.postRequest(request.toBuilder().setSlavePlacement(Optional.of(SlavePlacement.SEPARATE)).setInstances(Optional.of(2)).build());

    schedulerPoller.runActionOnPoll();

    Assert.assertEquals(0, taskManager.getActiveTasks().size());

    resourceOffers();

    int numTasks = taskManager.getActiveTasks().size();

    Assert.assertEquals(2, numTasks);

    startAndDeploySecondRequest();

    schedulerPoller.runActionOnPoll();

    Assert.assertEquals(numTasks, taskManager.getActiveTasks().size());

    resourceOffers();

    Assert.assertTrue(taskManager.getActiveTasks().size() > numTasks);
  }

  @Test
  public void testOfferCache() {
    configuration.setCacheOffers(true);
    configuration.setOfferCacheSize(2);
    List<Offer> offers2 = resourceOffers();

    sms.rescind(offers2.get(0).getId());

    initRequest();
    initFirstDeploy();

    requestResource.postRequest(request.toBuilder().setSlavePlacement(Optional.of(SlavePlacement.SEPARATE)).setInstances(Optional.of(2)).build());

    schedulerPoller.runActionOnPoll();

    Assert.assertEquals(1, taskManager.getActiveTasks().size());

    resourceOffers();

    Assert.assertEquals(2, taskManager.getActiveTasks().size());
  }

  @Test
  public void testOfferCombination() {
    configuration.setCacheOffers(true);
    configuration.setOfferCacheSize(2);

    // Each are half of needed memory
    Offer offer1 = createOffer(1, 64, "slave1", "host1");
    Offer offer2 = createOffer(1, 64, "slave1", "host1");
    sms.resourceOffers(ImmutableList.of(offer1, offer2));

    initRequest();
    initFirstDeploy();
    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, firstDeployId, System.currentTimeMillis(), Optional.absent(), PendingType.TASK_DONE,
        Optional.absent(), Optional.absent()));

    schedulerPoller.runActionOnPoll();

    Assert.assertEquals(1, taskManager.getActiveTasks().size());

    Assert.assertEquals(2, taskManager.getActiveTasks().get(0).getOffers().size());
  }

  @Test
  public void testLeftoverCachedOffersAreReturnedToCache() throws Exception {
    configuration.setCacheOffers(true);

    Offer neededOffer = createOffer(1, 128, "slave1", "host1", Optional.absent(), Collections.emptyMap(), new String[]{"80:81"});
    Offer extraOffer = createOffer(4, 256, "slave1", "host1", Optional.absent(), Collections.emptyMap(), new String[]{"83:84"});

    sms.resourceOffers(ImmutableList.of(neededOffer, extraOffer));

    initRequest();

    firstDeploy = initAndFinishDeploy(request, new SingularityDeployBuilder(request.getId(), firstDeployId)
        .setCommand(Optional.of("sleep 100")), Optional.of(new Resources(1, 128, 2, 0))
    );

    requestManager.addToPendingQueue(
        new SingularityPendingRequest(
            requestId,
            firstDeployId,
            System.currentTimeMillis(),
            Optional.absent(),
            PendingType.TASK_DONE,
            Optional.absent(),
            Optional.absent()
        )
    );

    schedulerPoller.runActionOnPoll();

    List<Offer> cachedOffers = offerCache.peekOffers();
    Assert.assertEquals(1, cachedOffers.size());
  }

  @Test
  public void testLeftoverNewOffersAreCached() {
    configuration.setCacheOffers(true);

    Offer neededOffer = createOffer(1, 128, "slave1", "host1");
    Offer extraOffer = createOffer(4, 256, "slave1", "host1");

    initRequest();
    initFirstDeploy();

    requestManager.addToPendingQueue(
        new SingularityPendingRequest(
            requestId,
            firstDeployId,
            System.currentTimeMillis(),
            Optional.absent(),
            PendingType.TASK_DONE,
            Optional.absent(),
            Optional.absent()
        )
    );

    sms.resourceOffers(ImmutableList.of(neededOffer, extraOffer));

    List<Offer> cachedOffers = offerCache.peekOffers();
    Assert.assertEquals(1, cachedOffers.size());
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

    scheduler.drainPendingQueue();

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
  public void testCleanerLeavesPausedRequestTasksByDemand() {
    initScheduledRequest();
    initFirstDeploy();

    SingularityTask firstTask = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);
    createAndSchedulePendingTask(firstDeployId);

    requestResource.pause(requestId, Optional.of(new SingularityPauseRequest(Optional.of(false), Optional.<Long> absent(), Optional.<String> absent(), Optional.<String>absent(), Optional.<SingularityShellCommand>absent())));

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

    taskResource.killTask(firstTask.getTaskId().getId(), Optional.absent());

    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assert.assertEquals(0, taskManager.getNumCleanupTasks());
    Assert.assertEquals(0, taskManager.getNumActiveTasks());
  }

  @Test
  public void testTaskDestroy() {
    initRequest();
    initFirstDeploy();

    SingularityTask firstTask = startTask(firstDeploy, 1);
    SingularityTask secondTask = startTask(firstDeploy, 2);
    SingularityTask thirdTask = startTask(firstDeploy, 3);

    taskResource.killTask(secondTask.getTaskId().getId(), Optional.of(
        new SingularityKillTaskRequest(Optional.of(true), Optional.of("kill -9 bb"), Optional.<String> absent(), Optional.<Boolean> absent(), Optional.<SingularityShellCommand> absent())));

    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assert.assertEquals(2, taskManager.getNumActiveTasks());
    Assert.assertEquals(0, requestManager.getCleanupRequests().size());
    Assert.assertEquals(RequestState.ACTIVE, requestManager.getRequest(requestId).get().getState());
  }

  @Test
  public void testTaskBounce() {
    initRequest();
    initFirstDeploy();

    SingularityTask firstTask = startTask(firstDeploy);

    taskResource.killTask(firstTask.getTaskId().getId(), Optional.of(
        new SingularityKillTaskRequest(Optional.<Boolean> absent(), Optional.of("msg"), Optional.<String> absent(), Optional.of(true), Optional.<SingularityShellCommand>absent())));

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

    launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    requestResource.deleteRequest(requestId, Optional.<SingularityDeleteRequestRequest> absent());

    Assert.assertTrue(requestManager.getCleanupRequests().size() == 1);

    cleaner.drainCleanupQueue();

    Assert.assertTrue(!taskManager.getKilledTaskIdRecords().isEmpty());

    killKilledTasks();
    cleaner.drainCleanupQueue();

    Assert.assertTrue(requestManager.getCleanupRequests().isEmpty());
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
    scheduler.drainPendingQueue();

    List<Offer> oneOffer = Arrays.asList(createOffer(12, 1024));
    sms.resourceOffers(oneOffer);

    Assert.assertTrue(taskManager.getActiveTasks().size() == 3);
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assert.assertTrue(requestManager.getPendingRequests().isEmpty());
  }

  @Test
  public void testSchedulerExhaustsOffers() {
    initRequest();
    initFirstDeploy();

    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(10)).build());
    scheduler.drainPendingQueue();

    sms.resourceOffers(Arrays.asList(createOffer(2, 1024), createOffer(1, 1024)));

    Assert.assertEquals(3, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(7, taskManager.getPendingTaskIds().size());
  }

  @Test
  public void testSchedulerRandomizesOffers() {
    initRequest();
    initFirstDeploy();

    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(15)).build());
    scheduler.drainPendingQueue();

    sms.resourceOffers(Arrays.asList(createOffer(20, 1024), createOffer(20, 1024)));

    Assert.assertEquals(15, taskManager.getActiveTaskIds().size());

    Set<String> offerIds = Sets.newHashSet();

    for (SingularityTask activeTask : taskManager.getActiveTasks()) {
      offerIds.addAll(activeTask.getOffers().stream().map((o) -> o.getId().getValue()).collect(Collectors.toList()));
    }

    Assert.assertEquals(2, offerIds.size());
  }

  @Test
  public void testSchedulerHandlesFinishedTasks() {
    initScheduledRequest();
    initFirstDeploy();

    schedule = "*/1 * * * * ? 1995";

    // cause it to be pending
    requestResource.postRequest(request.toBuilder().setQuartzSchedule(Optional.of(schedule)).build());
    scheduler.drainPendingQueue();

    Assert.assertTrue(requestResource.getActiveRequests(false).isEmpty());
    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.FINISHED);
    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());

    schedule = "*/1 * * * * ?";
    requestResource.postRequest(request.toBuilder().setQuartzSchedule(Optional.of(schedule)).build());
    scheduler.drainPendingQueue();

    Assert.assertTrue(!requestResource.getActiveRequests(false).isEmpty());
    Assert.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.ACTIVE);

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
    sms.resourceOffers(Arrays.asList(createOffer(1, 129, "slave1", "host1", Optional.of("rack1"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 129, "slave2", "host2", Optional.of("rack1"))));

    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());

    Assert.assertEquals("host1", taskManager.getActiveTaskIds().get(0).getSanitizedHost());

    Assert.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.STARTING_DECOMMISSION, Optional.<String> absent(), Optional.of("user1")));

    sms.resourceOffers(Arrays.asList(createOffer(1, 129, "slave2", "host2", Optional.of("rack1"))));

    cleaner.drainCleanupQueue();

    sms.resourceOffers(Arrays.asList(createOffer(1, 129, "slave2", "host2", Optional.of("rack1"))));

    cleaner.drainCleanupQueue();

    // task should not move!
    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assert.assertEquals("host1", taskManager.getActiveTaskIds().get(0).getSanitizedHost());
    Assert.assertTrue(taskManager.getKilledTaskIdRecords().isEmpty());
    Assert.assertTrue(taskManager.getCleanupTaskIds().size() == 1);
  }

  @Test
  public void testCustomResourcesWithRunNowRequest() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId, RequestType.ON_DEMAND);
    requestResource.postRequest(bldr.build());
    deploy("d2");

    SingularityRunNowRequest runNowRequest = new SingularityRunNowRequest(Optional.<String>absent(), Optional.<Boolean>absent(), Optional.<String>absent(), Optional.<List<String>>absent(), Optional.of(new Resources(2, 2, 0)), Optional.<Long>absent());
    requestResource.scheduleImmediately(requestId, runNowRequest);

    scheduler.drainPendingQueue();

    SingularityPendingTask pendingTaskWithResourcs = taskManager.getPendingTasks().get(0);
    Assert.assertTrue(pendingTaskWithResourcs.getResources().isPresent());
    Assert.assertEquals(pendingTaskWithResourcs.getResources().get().getCpus(), 2, 0.0);

    sms.resourceOffers(Arrays.asList(createOffer(5, 5, "slave1", "host1", Optional.of("rack1"))));

    SingularityTask task = taskManager.getActiveTasks().get(0);
    Assert.assertEquals(MesosUtils.getNumCpus(task.getMesosTask().getResources(), Optional.<String>absent()), 2.0, 0.0);
  }

  @Test
  public void testRunOnceRunOnlyOnce() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId, RequestType.RUN_ONCE);
    request = bldr.build();
    saveRequest(request);

    deployResource.deploy(new SingularityDeployRequest(new SingularityDeployBuilder(requestId, "d1").setCommand(Optional.of("cmd")).build(), Optional.<Boolean> absent(), Optional.<String> absent()));

    scheduler.drainPendingQueue();

    deployChecker.checkDeploys();

    resourceOffers();

    Assert.assertTrue(deployManager.getRequestDeployState(requestId).get().getActiveDeploy().isPresent());
    Assert.assertTrue(!deployManager.getRequestDeployState(requestId).get().getPendingDeploy().isPresent());

    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    resourceOffers();

    Assert.assertTrue(taskManager.getActiveTaskIds().isEmpty());

    deployResource.deploy(new SingularityDeployRequest(new SingularityDeployBuilder(requestId, "d2").setCommand(Optional.of("cmd")).build(), Optional.<Boolean>absent(), Optional.<String> absent()));

    scheduler.drainPendingQueue();

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

    scheduler.drainPendingQueue();

    resourceOffers();
    Assert.assertEquals(2, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testRunOnceDontMoveDuringDecomission() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId, RequestType.RUN_ONCE);
    request = bldr.build();
    saveRequest(request);

    deployResource.deploy(new SingularityDeployRequest(new SingularityDeployBuilder(requestId, "d1").setCommand(Optional.of("cmd")).build(), Optional.<Boolean> absent(), Optional.<String> absent()));

    scheduler.drainPendingQueue();

    deployChecker.checkDeploys();

    validateTaskDoesntMoveDuringDecommission();
  }

  @Test
  public void testDecommissionDoesntKillPendingDeploy() {
    initRequest();

    deployResource.deploy(new SingularityDeployRequest(new SingularityDeployBuilder(requestId, "d1").setCommand(Optional.of("cmd")).build(), Optional.<Boolean> absent(), Optional.<String> absent()));

    scheduler.drainPendingQueue();
    deployChecker.checkDeploys();
    resourceOffers();

    Assert.assertEquals(1, taskManager.getNumActiveTasks());

    slaveResource.decommissionSlave(taskManager.getActiveTasks().get(0).getAgentId().getValue(), null);

    scheduler.checkForDecomissions();

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

    scheduler.drainPendingQueue();
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
  public void testLbCleanupDoesNotRemoveBeforeAdd() {
    initLoadBalancedRequest();
    initFirstDeploy();
    SingularityTask taskOne = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    initSecondDeploy();
    SingularityTask taskTwo = launchTask(request, secondDeploy, 1, TaskState.TASK_RUNNING);
    testingLbClient.setNextBaragonRequestState(BaragonRequestState.WAITING);
    deployChecker.checkDeploys();

    // First task from old deploy is still starting, never got added to LB so it should not have a removal request
    Assert.assertFalse(taskManager.getLoadBalancerState(taskOne.getTaskId(), LoadBalancerRequestType.ADD).isPresent());
    Assert.assertFalse(taskManager.getLoadBalancerState(taskOne.getTaskId(), LoadBalancerRequestType.REMOVE).isPresent());

    // Second task should have an add request
    Assert.assertTrue(taskManager.getLoadBalancerState(taskTwo.getTaskId(), LoadBalancerRequestType.ADD).isPresent());

    testingLbClient.setNextBaragonRequestState(BaragonRequestState.SUCCESS);
    deployChecker.checkDeploys();

    // First task from old deploy should still have no LB updates, but should have a cleanup
    Assert.assertFalse(taskManager.getLoadBalancerState(taskOne.getTaskId(), LoadBalancerRequestType.ADD).isPresent());
    Assert.assertFalse(taskManager.getLoadBalancerState(taskOne.getTaskId(), LoadBalancerRequestType.REMOVE).isPresent());
    Assert.assertTrue(taskManager.getCleanupTaskIds().contains(taskOne.getTaskId()));
  }

  @Test
  public void testLbCleanupSkippedOnSkipRemoveFlag() {
    configuration.setDeleteRemovedRequestsFromLoadBalancer(true);
    initLoadBalancedRequest();
    initLoadBalancedDeploy();
    startTask(firstDeploy);

    boolean removeFromLoadBalancer = false;
    SingularityDeleteRequestRequest deleteRequest = new SingularityDeleteRequestRequest(Optional.absent(), Optional.absent(), Optional.of(removeFromLoadBalancer));

    requestResource.deleteRequest(requestId, Optional.of(deleteRequest));

    testingLbClient.setNextBaragonRequestState(BaragonRequestState.WAITING);

    Assert.assertFalse("Tasks should get cleaned up", requestManager.getCleanupRequests().isEmpty());
    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assert.assertFalse("The request should get cleaned up", requestManager.getCleanupRequests().isEmpty());
    cleaner.drainCleanupQueue();

    Assert.assertTrue("The request should not be removed from the load balancer", requestManager.getLbCleanupRequestIds().isEmpty());
  }

  @Test
  public void testLbCleanupOccursOnRequestDelete() {
    configuration.setDeleteRemovedRequestsFromLoadBalancer(true);
    initLoadBalancedRequest();
    initLoadBalancedDeploy();
    startTask(firstDeploy);

    requestResource.deleteRequest(requestId, Optional.absent());

    testingLbClient.setNextBaragonRequestState(BaragonRequestState.WAITING);

    Assert.assertFalse("Tasks should get cleaned up", requestManager.getCleanupRequests().isEmpty());
    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assert.assertFalse("The request should get cleaned up", requestManager.getCleanupRequests().isEmpty());
    cleaner.drainCleanupQueue();

    Assert.assertFalse("The request should get removed from the load balancer", requestManager.getLbCleanupRequestIds().isEmpty());
  }

  @Test
  public void testReconciliation() {
    Assert.assertTrue(!taskReconciliation.isReconciliationRunning());

    configuration.setCheckReconcileWhenRunningEveryMillis(1);

    initRequest();
    initFirstDeploy();

    Assert.assertTrue(taskReconciliation.startReconciliation() == ReconciliationState.STARTED);
    Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !taskReconciliation.isReconciliationRunning());

    SingularityTask taskOne = launchTask(request, firstDeploy, 1, TaskState.TASK_STARTING);
    SingularityTask taskTwo = launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);

    saveLastActiveTaskStatus(taskOne, Optional.absent(), -1000);

    Assert.assertTrue(taskReconciliation.startReconciliation() == ReconciliationState.STARTED);
    Assert.assertTrue(taskReconciliation.startReconciliation() == ReconciliationState.ALREADY_RUNNING);

    Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> taskReconciliation.isReconciliationRunning());

    saveLastActiveTaskStatus(taskOne, Optional.of(buildTaskStatus(taskOne)), +1000);

    Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> taskReconciliation.isReconciliationRunning());

    saveLastActiveTaskStatus(taskTwo, Optional.of(buildTaskStatus(taskTwo)), +1000);

    Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !taskReconciliation.isReconciliationRunning());
  }


  @Test
  public void testSchedulerPriority() {
    final SingularityRequest lowPriorityRequest = new SingularityRequestBuilder("lowPriorityRequest", RequestType.WORKER).setTaskPriorityLevel(Optional.of(.25)).build();
    saveRequest(lowPriorityRequest);
    final SingularityRequest mediumPriorityRequest = new SingularityRequestBuilder("mediumPriorityRequest", RequestType.WORKER).setTaskPriorityLevel(Optional.of(.5)).build();
    saveRequest(mediumPriorityRequest);
    final SingularityRequest highPriorityRequest = new SingularityRequestBuilder("highPriorityRequest", RequestType.WORKER).setTaskPriorityLevel(Optional.of(.75)).build();
    saveRequest(highPriorityRequest);

    final SingularityDeploy lowPriorityDeploy = initAndFinishDeploy(lowPriorityRequest, "lowPriorityDeploy");
    final SingularityDeploy mediumPriorityDeploy = initAndFinishDeploy(mediumPriorityRequest, "mediumPriorityDeploy");
    final SingularityDeploy highPriorityDeploy = initAndFinishDeploy(highPriorityRequest, "highPriorityDeploy");

    // Task requests launched at ~ the same time should be in priority order
    long now = System.currentTimeMillis();
    List<SingularityTaskRequest> requestsByPriority = Arrays.asList(
      buildTaskRequest(lowPriorityRequest, lowPriorityDeploy, now),
      buildTaskRequest(mediumPriorityRequest, mediumPriorityDeploy, now),
      buildTaskRequest(highPriorityRequest, highPriorityDeploy, now));

    List<SingularityTaskRequest> sortedRequestsByPriority = taskPrioritizer.getSortedDueTasks(requestsByPriority);

    Assert.assertEquals(sortedRequestsByPriority.get(0).getRequest().getId(), highPriorityRequest.getId());
    Assert.assertEquals(sortedRequestsByPriority.get(1).getRequest().getId(), mediumPriorityRequest.getId());
    Assert.assertEquals(sortedRequestsByPriority.get(2).getRequest().getId(), lowPriorityRequest.getId());

    // A lower priority task that is long overdue should be run before a higher priority task
    now = System.currentTimeMillis();
    List<SingularityTaskRequest> requestsByOverdueAndPriority = Arrays.asList(
      buildTaskRequest(lowPriorityRequest, lowPriorityDeploy, now - 120000), // 2 min overdue
      buildTaskRequest(mediumPriorityRequest, mediumPriorityDeploy, now - 30000), // 60s overdue
      buildTaskRequest(highPriorityRequest, highPriorityDeploy, now)); // Not overdue

    List<SingularityTaskRequest> sortedRequestsByOverdueAndPriority = taskPrioritizer.getSortedDueTasks(requestsByOverdueAndPriority);

    Assert.assertEquals(sortedRequestsByOverdueAndPriority.get(0).getRequest().getId(), lowPriorityRequest.getId());
    Assert.assertEquals(sortedRequestsByOverdueAndPriority.get(1).getRequest().getId(), mediumPriorityRequest.getId());
    Assert.assertEquals(sortedRequestsByOverdueAndPriority.get(2).getRequest().getId(), highPriorityRequest.getId());
  }

  @Test
  public void badPauseExpires() {
    initRequest();

    requestManager.createCleanupRequest(new SingularityRequestCleanup(Optional.<String>absent(), RequestCleanupType.PAUSING, System.currentTimeMillis(),
        Optional.<Boolean>absent(), Optional.absent(), requestId, Optional.<String>absent(), Optional.<Boolean> absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<SingularityShellCommand>absent()));

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
    Assert.assertEquals(requestId, requestManager.getPausedRequests(false).iterator().next().getRequest().getId());

    requestResource.unpause(requestId, Optional.<SingularityUnpauseRequest> absent());

    resourceOffers();

    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assert.assertEquals(0, taskManager.getPendingTasks().size());

    Assert.assertEquals(RequestState.ACTIVE, requestManager.getRequest(requestId).get().getState());
    Assert.assertEquals(requestId, requestManager.getActiveRequests(false).iterator().next().getRequest().getId());
  }

  @Test
  public void testBounce() {
    initRequest();

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(3), Optional.<Long> absent(), Optional.<Boolean> absent(), Optional.<String> absent(), Optional.<String>absent(), Optional.<Boolean>absent(), Optional.<Boolean>absent()));

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
  public void testIncrementalBounceShutsDownOldTasksPerNewHealthyTask() {
    initRequest();

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(3), Optional.<Long> absent(), Optional.<Boolean> absent(), Optional.<String> absent(), Optional.<String>absent(), Optional.<Boolean>absent(), Optional.<Boolean>absent()));

    initFirstDeploy();

    startTask(firstDeploy, 1);
    startTask(firstDeploy, 2);
    startTask(firstDeploy, 3);

    requestResource.bounce(requestId,
        Optional.of(new SingularityBounceRequest(Optional.of(true), Optional.absent(), Optional.of(1L), Optional.absent(), Optional.of("msg"), Optional.absent())));

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
  public void testBounceOnPendingInstancesReleasesLock() {
    initRequest();
    initFirstDeploy();

    SingularityTask task = startTask(firstDeploy, 1);
    statusUpdate(task, TaskState.TASK_FAILED);
    killKilledTasks();

    Assert.assertEquals("Bounce starts when tasks have not yet been launched", 0, taskManager.getActiveTaskIds().size());

    requestResource.bounce(requestId, Optional.of(new SingularityBounceRequest(Optional.absent(), Optional.of(true), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent())));

    // It acquires a lock on the bounce
    Assert.assertTrue("Lock on bounce should be acquired during bounce", requestManager.getExpiringBounce(requestId).isPresent());

    cleaner.drainCleanupQueue();

    scheduler.drainPendingQueue();
    resourceOffers();

    for (SingularityTaskId singularityTaskId : taskManager.getActiveTaskIds()) {
      taskManager.saveTaskHistoryUpdate(new SingularityTaskHistoryUpdate(singularityTaskId, System.currentTimeMillis(), ExtendedTaskState.TASK_RUNNING, Optional.absent(), Optional.absent(), Collections.emptySet()));
    }

    cleaner.drainCleanupQueue();
    killKilledTasks();

    // It finishes with one task running and the bounce released
    Assert.assertEquals("Should end bounce with target number of tasks", 1, taskManager.getActiveTaskIds().size());
    for (SingularityTaskId singularityTaskId : taskManager.getActiveTaskIds()) {
      String statusMessage = taskManager.getTaskHistoryUpdates(singularityTaskId)
          .get(0)
          .getStatusMessage()
          .get();
      Assert.assertTrue("Task was started by bounce", statusMessage.contains("BOUNCE"));
    }
    Assert.assertFalse("Lock on bounce should be released after bounce", requestManager.getExpiringBounce(requestId).isPresent());
  }



  @Test
  public void testBounceOnRunningInstancesReleasesLock() {
    initRequest();
    initFirstDeploy();

    startTask(firstDeploy, 1);
    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());

    requestResource.bounce(requestId, Optional.of(new SingularityBounceRequest(Optional.absent(), Optional.of(true), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent())));
    cleaner.drainCleanupQueue();

    // It acquires a lock on the bounce
    Assert.assertTrue("Lock on bounce should be acquired during bounce", requestManager.getExpiringBounce(requestId).isPresent());

    scheduler.drainPendingQueue();
    resourceOffers();

    for (SingularityTaskId singularityTaskId : taskManager.getActiveTaskIds()) {
      taskManager.saveTaskHistoryUpdate(new SingularityTaskHistoryUpdate(singularityTaskId, System.currentTimeMillis(), ExtendedTaskState.TASK_RUNNING, Optional.absent(), Optional.absent(), Collections.emptySet()));
    }

    Assert.assertTrue("Need to start at least 1 instance to begin killing old instances", taskManager.getActiveTaskIds().size() >= 2);
    cleaner.drainCleanupQueue();
    killKilledTasks();


    // It finishes with one task running and the bounce released
    Assert.assertEquals("Should end bounce with target number of tasks", 1, taskManager.getActiveTaskIds().size());
    for (SingularityTaskId singularityTaskId : taskManager.getActiveTaskIds()) {
      String statusMessage = taskManager.getTaskHistoryUpdates(singularityTaskId)
          .get(0)
          .getStatusMessage()
          .get();
      Assert.assertTrue("Task was started by bounce", statusMessage.contains("BOUNCE"));
    }
    Assert.assertFalse("Lock on bounce should be released after bounce", requestManager.getExpiringBounce(requestId).isPresent());
  }

  @Test
  public void testIncrementalBounce() {
    initRequest();
    resourceOffers(2); // set up slaves so scale validate will pass

    SingularityRequest request = requestResource.getRequest(requestId).getRequest();

    requestResource.postRequest(request.toBuilder()
        .setSlavePlacement(Optional.of(SlavePlacement.SEPARATE_BY_REQUEST))
        .setInstances(Optional.of(2)).build()
        );

    initHCDeploy();

    SingularityTask taskOne = startSeparatePlacementTask(firstDeploy, 1);
    SingularityTask taskTwo = startSeparatePlacementTask(firstDeploy, 2);

    requestManager.createCleanupRequest(new SingularityRequestCleanup(user, RequestCleanupType.INCREMENTAL_BOUNCE, System.currentTimeMillis(),
        Optional.<Boolean>absent(), Optional.absent(), requestId, Optional.of(firstDeployId), Optional.<Boolean> absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<SingularityShellCommand>absent()));

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
    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(200), Optional.of(1000L), System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent(), taskThree.getTaskId(), Optional.<Boolean>absent()));

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
    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(200), Optional.of(1000L), System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent(), taskFour.getTaskId(), Optional.<Boolean>absent()));

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

    Mockito.verify(mailer, Mockito.times(0)).sendTaskOverdueMail(ArgumentMatchers.<Optional<SingularityTask>> any(), ArgumentMatchers.<SingularityTaskId> any(), ArgumentMatchers.<SingularityRequest> any(), ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong());

    configuration.setWarnIfScheduledJobIsRunningForAtLeastMillis(TimeUnit.HOURS.toMillis(1));

    scheduledJobPoller.runActionOnPoll();

    Mockito.verify(mailer, Mockito.times(1)).sendTaskOverdueMail(ArgumentMatchers.<Optional<SingularityTask>> any(), ArgumentMatchers.<SingularityTaskId> any(), ArgumentMatchers.<SingularityRequest> any(), ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong());

    scheduledJobPoller.runActionOnPoll();

    Mockito.verify(mailer, Mockito.times(1)).sendTaskOverdueMail(ArgumentMatchers.<Optional<SingularityTask>> any(), ArgumentMatchers.<SingularityTaskId> any(), ArgumentMatchers.<SingularityRequest> any(), ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong());

    statusUpdate(firstTask, TaskState.TASK_FINISHED);

    Optional<SingularityDeployStatistics> deployStatistics = deployManager.getDeployStatistics(requestId, firstDeployId);

    long oldAvg = deployStatistics.get().getAverageRuntimeMillis().get();

    Assert.assertTrue(deployStatistics.get().getNumTasks() == 1);
    Assert.assertTrue(deployStatistics.get().getAverageRuntimeMillis().get() > 1 && deployStatistics.get().getAverageRuntimeMillis().get() < TimeUnit.DAYS.toMillis(1));

    configuration.setWarnIfScheduledJobIsRunningForAtLeastMillis(1);

    SingularityTask secondTask = launchTask(request, firstDeploy, now - 500, 1, TaskState.TASK_RUNNING);

    scheduledJobPoller.runActionOnPoll();

    Mockito.verify(mailer, Mockito.times(1)).sendTaskOverdueMail(ArgumentMatchers.<Optional<SingularityTask>> any(), ArgumentMatchers.<SingularityTaskId> any(), ArgumentMatchers.<SingularityRequest> any(), ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong());

    statusUpdate(secondTask, TaskState.TASK_FINISHED);

    deployStatistics = deployManager.getDeployStatistics(requestId, firstDeployId);

    Assert.assertTrue(deployStatistics.get().getNumTasks() == 2);
    Assert.assertTrue(deployStatistics.get().getAverageRuntimeMillis().get() > 1 && deployStatistics.get().getAverageRuntimeMillis().get() < oldAvg);

    saveRequest(request.toBuilder().setScheduledExpectedRuntimeMillis(Optional.of(1L)).build());

    SingularityTask thirdTask = launchTask(request, firstDeploy, now - 502, 1, TaskState.TASK_RUNNING);

    scheduledJobPoller.runActionOnPoll();

    Mockito.verify(mailer, Mockito.times(2)).sendTaskOverdueMail(ArgumentMatchers.<Optional<SingularityTask>> any(), ArgumentMatchers.<SingularityTaskId> any(), ArgumentMatchers.<SingularityRequest> any(), ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong());

    taskManager.deleteTaskHistory(thirdTask.getTaskId());

    scheduledJobPoller.runActionOnPoll();

    Mockito.verify(mailer, Mockito.times(3)).sendTaskOverdueMail(ArgumentMatchers.<Optional<SingularityTask>> any(), ArgumentMatchers.<SingularityTaskId> any(), ArgumentMatchers.<SingularityRequest> any(), ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong());
  }

  @Test
  public void testTaskOddities() {
    // test unparseable status update
    TaskStatus.Builder bldr = TaskStatus.newBuilder()
        .setTaskId(TaskID.newBuilder().setValue("task"))
        .setAgentId(AgentID.newBuilder().setValue("slave1"))
        .setState(TaskState.TASK_RUNNING);

    // should not throw exception:
    sms.statusUpdate(bldr.build());

    initRequest();
    initFirstDeploy();

    SingularityTask taskOne = launchTask(request, firstDeploy, 1, TaskState.TASK_STARTING);

    taskManager.deleteTaskHistory(taskOne.getTaskId());

    Assert.assertTrue(taskManager.isActiveTask(taskOne.getTaskId().getId()));

    statusUpdate(taskOne, TaskState.TASK_RUNNING);
    statusUpdate(taskOne, TaskState.TASK_FAILED);

    Assert.assertTrue(!taskManager.isActiveTask(taskOne.getTaskId().getId()));
    System.out.println(taskManager.getTaskHistoryUpdates(taskOne.getTaskId()));

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

    scheduler.drainPendingQueue();

    requestResource.scheduleImmediately(requestId);

    scheduler.drainPendingQueue();

    Assert.assertEquals(2, taskManager.getPendingTaskIds().size());

    resourceOffers();

    Assert.assertEquals(4, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testRunNowScheduledJobDoesNotRetry() {
    initScheduledRequest();
    SingularityRequest request = requestResource.getRequest(requestId).getRequest();
    SingularityRequest newRequest = request.toBuilder().setNumRetriesOnFailure(Optional.of(2)).build();
    requestResource.postRequest(newRequest);
    initFirstDeploy();

    requestResource.scheduleImmediately(requestId, new SingularityRunNowRequest(Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.<Long>absent()));
    resourceOffers();

    SingularityTask task = taskManager.getActiveTasks().get(0);
    statusUpdate(task, TaskState.TASK_FAILED);

    SingularityDeployStatistics deployStatistics = deployManager.getDeployStatistics(task.getTaskId().getRequestId(), task.getTaskId().getDeployId()).get();

    Assert.assertEquals(TaskState.TASK_FAILED, deployStatistics.getLastTaskState().get().toTaskState().get());
    Assert.assertEquals(PendingType.TASK_DONE, taskManager.getPendingTaskIds().get(0).getPendingType());
    Assert.assertEquals(1, deployStatistics.getNumFailures());
    Assert.assertEquals(0, deployStatistics.getNumSequentialRetries());
    Assert.assertEquals(Optional.<Long>absent(), deployStatistics.getAverageRuntimeMillis());
  }

  @Test
  public void testOnDemandRunNowJobRespectsSpecifiedRunAtTime() {
    initOnDemandRequest();
    initFirstDeploy();

    long requestedLaunchTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10);

    requestResource.scheduleImmediately(
        requestId,
        new SingularityRunNowRequest(
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Optional.of(requestedLaunchTime)
        )
    );

    scheduler.drainPendingQueue();

    SingularityPendingTaskId task = taskManager.getPendingTaskIds().get(0);
    long runAt = task.getNextRunAt();

    Assert.assertEquals(requestedLaunchTime, runAt);
  }

  @Test
  public void testScheduledRunNowJobRespectsSpecifiedRunAtTime() {
    initScheduledRequest();
    initFirstDeploy();

    long requestedLaunchTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10);

    requestResource.scheduleImmediately(
        requestId,
        new SingularityRunNowRequest(
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Optional.of(requestedLaunchTime)
        )
    );

    scheduler.drainPendingQueue();

    SingularityPendingTaskId task = taskManager.getPendingTaskIds().get(0);
    long runAt = task.getNextRunAt();

    Assert.assertEquals(requestedLaunchTime, runAt);
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
        Optional.<String> absent(), Optional.<String>absent(), Optional.<Boolean>absent(), Optional.<Boolean>absent()));

    resourceOffers();
    cleaner.drainCleanupQueue();

    Assert.assertEquals(3, taskManager.getKilledTaskIdRecords().size());

    for (SingularityKilledTaskIdRecord taskId : taskManager.getKilledTaskIdRecords()) {
      Assert.assertTrue(taskId.getTaskId().getInstanceNo() > 2);

      scheduler.drainPendingQueue();
    }
  }

  @Test
  public void testRequestsInPendingQueueAreOrderedByTimestamp() {
    long now = System.currentTimeMillis();
    initRequestWithType(RequestType.SCHEDULED, false);
    startFirstDeploy();
    SingularityPendingRequest pendingDeployRequest = new SingularityPendingRequest(requestId, firstDeploy.getId(), now, Optional.absent(), PendingType.NEW_DEPLOY,
        firstDeploy.getSkipHealthchecksOnDeploy(), Optional.absent());
    SingularityPendingRequest pendingRunNowRequest = new SingularityPendingRequest(requestId, firstDeploy.getId(), now + 200, Optional.absent(), PendingType.IMMEDIATE,
        firstDeploy.getSkipHealthchecksOnDeploy(), Optional.absent());
    requestManager.addToPendingQueue(pendingDeployRequest);


    requestManager.addToPendingQueue(pendingRunNowRequest);

    Assert.assertEquals(2, requestManager.getPendingRequests().size());
    // Was added first
    Assert.assertEquals(PendingType.NEW_DEPLOY, requestManager.getPendingRequests().get(0).getPendingType());
    // Was added second
    Assert.assertEquals(PendingType.IMMEDIATE, requestManager.getPendingRequests().get(1).getPendingType());

    resourceOffers();
  }

  @Test
  public void testImmediateRequestsAreConsistentlyDeleted() {
    long now = System.currentTimeMillis();
    initRequestWithType(RequestType.SCHEDULED, false);
    startFirstDeploy();
    SingularityPendingRequest pendingDeployRequest = new SingularityPendingRequest(requestId, firstDeploy.getId(), now, Optional.absent(), PendingType.NEW_DEPLOY,
        firstDeploy.getSkipHealthchecksOnDeploy(), Optional.absent());
    SingularityPendingRequest pendingRunNowRequest = new SingularityPendingRequest(requestId, firstDeploy.getId(), now + 200, Optional.absent(), PendingType.IMMEDIATE,
        firstDeploy.getSkipHealthchecksOnDeploy(), Optional.absent());

    requestManager.addToPendingQueue(pendingDeployRequest);
    requestManager.addToPendingQueue(pendingRunNowRequest);

    // Pending queue has two requests: NEW_DEPLOY & IMMEDIATE
    Assert.assertEquals(2, requestManager.getPendingRequests().size());
    finishNewTaskChecks();

    requestManager.deletePendingRequest(pendingDeployRequest);

    // Just the immediate run
    Assert.assertEquals(1, requestManager.getPendingRequests().size());

    requestManager.deletePendingRequest(pendingRunNowRequest);

    // Immediate run was successfully deleted
    Assert.assertEquals(0, requestManager.getPendingRequests().size());
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
  public void testRemovedRequestData() {
    long now = System.currentTimeMillis();

    initRequest();
    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, firstDeployId);
    db.setMaxTaskRetries(Optional.of(1));
    initDeploy(db, now);

    deployChecker.checkDeploys();
    Assert.assertEquals(DeployState.WAITING, deployManager.getPendingDeploys().get(0).getCurrentDeployState());

    requestManager.startDeletingRequest(request, Optional.absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent());
    requestManager.markDeleted(request, now, Optional.<String>absent(), Optional.<String>absent());
    deployChecker.checkDeploys();
    SingularityDeployResult deployResult = deployManager.getDeployResult(requestId, firstDeployId).get();
    Assert.assertEquals(DeployState.FAILED, deployResult.getDeployState());
    Assert.assertTrue(deployResult.getMessage().get().contains("MISSING"));
  }

  @Test
  public void itCorrectlyUpdatesRequestDeletingStateHistory() {
    initRequest();
    Assert.assertEquals(RequestState.ACTIVE, requestManager.getRequest(requestId).get().getState());
    Assert.assertEquals(1, requestManager.getRequestHistory(requestId).size());

    requestManager.startDeletingRequest(request, Optional.absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("the cake is a lie"));
    Assert.assertEquals(RequestState.DELETING, requestManager.getRequest(requestId).get().getState());
    Assert.assertEquals(2, requestManager.getRequestHistory(requestId).size());

    cleaner.drainCleanupQueue();
    Assert.assertEquals(3, requestManager.getRequestHistory(requestId).size());

    List<RequestHistoryType> historyTypes = new ArrayList<>();
    for (SingularityRequestHistory request : requestManager.getRequestHistory(requestId)) {
      historyTypes.add(request.getEventType());
    }
    Assert.assertTrue(historyTypes.contains(RequestHistoryType.CREATED));
    Assert.assertTrue(historyTypes.contains(RequestHistoryType.DELETING));
    Assert.assertTrue(historyTypes.contains(RequestHistoryType.DELETED));
  }


  @Test
  public void itSetsRequestStateToDeletedAfterAllTasksAreCleanedUp() {
    initRequest();

    SingularityRequest request = requestResource.getRequest(requestId).getRequest();
    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(2)).build());
    initFirstDeploy();

    launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);
    launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);

    Assert.assertEquals(requestId, requestManager.getActiveRequests().iterator().next().getRequest().getId());
    Assert.assertEquals(2, taskManager.getActiveTaskIds().size());

    requestManager.startDeletingRequest(request, Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent());

    Assert.assertEquals(requestId, requestManager.getCleanupRequests().get(0).getRequestId());
    Assert.assertEquals(RequestState.DELETING, requestManager.getRequest(requestId).get().getState());

    cleaner.drainCleanupQueue();
    Assert.assertEquals(0, taskManager.getCleanupTaskIds().size());
    killKilledTasks();
    cleaner.drainCleanupQueue();
    Assert.assertFalse(requestManager.getRequest(requestId).isPresent());
  }

  @Test
  public void itSetsRequestStateToDeletedIfTaskCleanupFails() {
    initRequest();

    SingularityRequest request = requestResource.getRequest(requestId).getRequest();
    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(2)).build());
    initFirstDeploy();

    SingularityTask firstTask = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);
    launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);

    Assert.assertEquals(requestId, requestManager.getActiveRequests().iterator().next().getRequest().getId());
    Assert.assertEquals(2, taskManager.getActiveTaskIds().size());

    requestManager.startDeletingRequest(request, Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent());

    Assert.assertEquals(requestId, requestManager.getCleanupRequests().get(0).getRequestId());
    Assert.assertEquals(RequestState.DELETING, requestManager.getRequest(requestId).get().getState());

    statusUpdate(firstTask, TaskState.TASK_FAILED);
    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());

    cleaner.drainCleanupQueue();
    Assert.assertEquals(0, taskManager.getCleanupTaskIds().size());
    killKilledTasks();
    cleaner.drainCleanupQueue();
    Assert.assertFalse(requestManager.getRequest(requestId).isPresent());
  }

  @Test
  public void testMaxTasksPerOffer() {
    configuration.setMaxTasksPerOffer(3);

    initRequest();
    initFirstDeploy();

    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(20)).build());
    scheduler.drainPendingQueue();

    sms.resourceOffers(Arrays.asList(createOffer(36, 12024)));

    Assert.assertTrue(taskManager.getActiveTasks().size() == 3);

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1"), createOffer(20, 20000, "slave2", "host2")));

    Assert.assertTrue(taskManager.getActiveTasks().size() == 9);

    configuration.setMaxTasksPerOffer(0);

    resourceOffers();

    Assert.assertTrue(taskManager.getActiveTasks().size() == 20);
  }

  @Test
  public void testRequestedPorts() {
    final SingularityDeployBuilder deployBuilder = dockerDeployWithPorts();

    initRequest();
    initAndFinishDeploy(request, deployBuilder, Optional.of(new Resources(1, 64, 3, 0)));
    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(2)).build());
    scheduler.drainPendingQueue();

    String[] portRangeWithNoRequestedPorts = {"65:70"};
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String> absent(), Collections.<String, String>emptyMap(), portRangeWithNoRequestedPorts)));
    Assert.assertEquals(0, taskManager.getActiveTasks().size());

    String[] portRangeWithSomeRequestedPorts = {"80:82"};
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String> absent(), Collections.<String, String>emptyMap(), portRangeWithSomeRequestedPorts)));
    Assert.assertEquals(0, taskManager.getActiveTasks().size());

    String[] portRangeWithRequestedButNotEnoughPorts = {"80:80", "8080:8080"};
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String> absent(), Collections.<String, String>emptyMap(), portRangeWithRequestedButNotEnoughPorts)));
    Assert.assertEquals(0, taskManager.getActiveTasks().size());

    String[] portRangeWithNeededPorts = {"80:83", "8080:8080"};
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String> absent(), Collections.<String, String>emptyMap(), portRangeWithNeededPorts)));
    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());
  }

  private SingularityDeployBuilder dockerDeployWithPorts() {
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
          Optional.of(ImmutableMap.of("env", "var=value")),
          Optional.absent())
        ));
    final SingularityDeployBuilder deployBuilder = new SingularityDeployBuilder(requestId, "test-docker-ports-deploy");
    deployBuilder.setContainerInfo(Optional.of(containerInfo));
    return deployBuilder;
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

    scheduler.drainPendingQueue();

    Assert.assertEquals(2, taskManager.getPendingTaskIds().size());
  }

  @Test
  public void testPriorityFreezeKillsActiveTasks() {
    final SingularityRequest lowPriorityRequest = new SingularityRequestBuilder("lowPriorityRequest", RequestType.WORKER).setTaskPriorityLevel(Optional.of(.25)).build();
    saveRequest(lowPriorityRequest);
    final SingularityRequest mediumPriorityRequest = new SingularityRequestBuilder("mediumPriorityRequest", RequestType.WORKER).setTaskPriorityLevel(Optional.of(.5)).build();
    saveRequest(mediumPriorityRequest);
    final SingularityRequest highPriorityRequest = new SingularityRequestBuilder("highPriorityRequest", RequestType.WORKER).setTaskPriorityLevel(Optional.of(.75)).build();
    saveRequest(highPriorityRequest);

    final SingularityDeploy lowPriorityDeploy = initAndFinishDeploy(lowPriorityRequest, "lowPriorityDeploy");
    final SingularityDeploy mediumPriorityDeploy = initAndFinishDeploy(mediumPriorityRequest, "mediumPriorityDeploy");
    SingularityDeploy highPriorityDeploy = initAndFinishDeploy(highPriorityRequest, "highPriorityDeploy");

    final SingularityTask lowPriorityTask = launchTask(lowPriorityRequest, lowPriorityDeploy, 2, 1, TaskState.TASK_RUNNING);
    final SingularityTask mediumPriorityTask = launchTask(mediumPriorityRequest, mediumPriorityDeploy, 1, 1, TaskState.TASK_RUNNING);
    final SingularityTask highPriorityTask = launchTask(highPriorityRequest, highPriorityDeploy, 10, 1, TaskState.TASK_RUNNING);

    // priority freeze of .5 means that lowPriorityRequest's task should have a cleanup
    priorityResource.createPriorityFreeze(new SingularityPriorityFreeze(.5, true, Optional.of("test"), Optional.<String>absent()));

    // perform the killing
    priorityKillPoller.runActionOnPoll();

    // assert lowPriorityRequest has a PRIORITY_KILL task cleanup and that mediumPriorityRequest and highPriorityRequest should not have cleanups
    Assert.assertEquals(TaskCleanupType.PRIORITY_KILL, taskManager.getTaskCleanup(lowPriorityTask.getTaskId().getId()).get().getCleanupType());

    Assert.assertEquals(false, taskManager.getTaskCleanup(mediumPriorityTask.getTaskId().getId()).isPresent());
    Assert.assertEquals(false, taskManager.getTaskCleanup(highPriorityTask.getTaskId().getId()).isPresent());

    // kill task(s) with cleanups
    cleaner.drainCleanupQueue();
    killKilledTasks();

    // assert lowPriorityTask was killed, mediumPriorityTask and highPriorityTask are still running
    Assert.assertEquals(ExtendedTaskState.TASK_KILLED, taskManager.getTaskHistory(lowPriorityTask.getTaskId()).get().getLastTaskUpdate().get().getTaskState());
    Assert.assertEquals(ExtendedTaskState.TASK_RUNNING, taskManager.getTaskHistory(mediumPriorityTask.getTaskId()).get().getLastTaskUpdate().get().getTaskState());
    Assert.assertEquals(ExtendedTaskState.TASK_RUNNING, taskManager.getTaskHistory(highPriorityTask.getTaskId()).get().getLastTaskUpdate().get().getTaskState());

    // assert lowPriorityRequest has a pending task
    final SingularityPendingTaskId pendingTaskId = taskManager.getPendingTaskIds().get(0);
    Assert.assertEquals(PendingType.TASK_DONE, pendingTaskId.getPendingType());
    Assert.assertEquals(lowPriorityRequest.getId(), pendingTaskId.getRequestId());

    // end the priority freeze
    priorityResource.deleteActivePriorityFreeze();

    // launch task(s)
    scheduler.drainPendingQueue();
    resourceOffers();

    // assert lowPriorityRequest has a new task running
    Assert.assertNotEquals(lowPriorityTask.getTaskId(), taskManager.getActiveTaskIdsForRequest(lowPriorityRequest.getId()).get(0).getId());
  }

  @Test
  public void testPriorityFreezeDoesntLaunchTasks() {
    // deploy lowPriorityRequest (affected by priority freeze)
    final SingularityRequest lowPriorityRequest = new SingularityRequestBuilder("lowPriorityRequest", RequestType.ON_DEMAND).setTaskPriorityLevel(Optional.of(.25)).build();
    saveRequest(lowPriorityRequest);
    deployResource.deploy(
        new SingularityDeployRequest(new SingularityDeployBuilder(lowPriorityRequest.getId(), "d1").setCommand(Optional.of("cmd")).build(), Optional.<Boolean>absent(), Optional.<String>absent()));

    // deploy medium priority request (NOT affected by priority freeze)
    final SingularityRequest mediumPriorityRequest = new SingularityRequestBuilder("mediumPriorityRequest", RequestType.ON_DEMAND).setTaskPriorityLevel(Optional.of(.5)).build();
    saveRequest(mediumPriorityRequest);
    deployResource.deploy(
        new SingularityDeployRequest(new SingularityDeployBuilder(mediumPriorityRequest.getId(), "d2").setCommand(Optional.of("cmd")).build(), Optional.<Boolean>absent(), Optional.<String>absent()));

    // create priority freeze
    priorityManager.createPriorityFreeze(
        new SingularityPriorityFreezeParent(new SingularityPriorityFreeze(0.3, true, Optional.<String>absent(), Optional.<String>absent()), System.currentTimeMillis(), Optional.<String>absent()));

    // launch both tasks
    requestResource.scheduleImmediately(lowPriorityRequest.getId());
    requestResource.scheduleImmediately(mediumPriorityRequest.getId());

    // drain pending queue
    scheduler.drainPendingQueue();
    resourceOffers();

    // assert that lowPriorityRequest has a pending task
    Assert.assertEquals(1, taskManager.getPendingTaskIds().size());
    Assert.assertEquals(lowPriorityRequest.getId(), taskManager.getPendingTaskIds().get(0).getRequestId());

    // assert that only mediumPriorityRequest has an active task
    Assert.assertEquals(0, taskManager.getActiveTaskIdsForRequest(lowPriorityRequest.getId()).size());
    Assert.assertEquals(1, taskManager.getActiveTaskIdsForRequest(mediumPriorityRequest.getId()).size());

    // delete priority freeze
    Assert.assertEquals(SingularityDeleteResult.DELETED, priorityManager.deleteActivePriorityFreeze());

    // drain pending
    scheduler.drainPendingQueue();
    resourceOffers();

    // check that both requests have active tasks
    Assert.assertEquals(1, taskManager.getActiveTaskIdsForRequest(lowPriorityRequest.getId()).size());
    Assert.assertEquals(1, taskManager.getActiveTaskIdsForRequest(mediumPriorityRequest.getId()).size());
  }

  @Test
  public void testObsoletePendingRequestsRemoved() {
    initRequest();
    initFirstDeploy();
    SingularityTask taskOne = startTask(firstDeploy);
    requestResource.pause(requestId, Optional.<SingularityPauseRequest> absent());
    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, firstDeployId, System.currentTimeMillis(), Optional.<String>absent(), PendingType.NEW_DEPLOY, Optional.<Boolean>absent(), Optional.<String>absent()));

    Assert.assertEquals(requestManager.getPendingRequests().size(), 1);
    scheduler.drainPendingQueue();

    Assert.assertEquals(requestManager.getPendingRequests().size(), 0);
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

    scheduler.drainPendingQueue();

    final SingularityRequest newRequest = request.toBuilder()
      .setSchedule(Optional.of(newSchedule))
      .setQuartzSchedule(Optional.<String>absent())
      .build();

    final SingularityDeploy newDeploy = new SingularityDeployBuilder(request.getId(), "2").setCommand(Optional.of("sleep 100")).build();

    deployResource.deploy(new SingularityDeployRequest(newDeploy, Optional.<Boolean>absent(), Optional.<String>absent(), Optional.of(newRequest)));

    deployChecker.checkDeploys();

    scheduler.drainPendingQueue();

    Assert.assertEquals(newScheduleQuartz, requestManager.getRequest(requestId).get().getRequest().getQuartzScheduleSafe());
  }

  @Test
  public void testImmediateRunReplacesScheduledTask() {
    initScheduledRequest();

    SingularityDeploy deploy = SingularityDeploy.newBuilder(requestId, firstDeployId)
        .setCommand(Optional.of("sleep 100"))
        .build();
    SingularityDeployRequest singularityDeployRequest = new SingularityDeployRequest(deploy, Optional.absent(), Optional.absent(), Optional.absent());
    deployResource.deploy(singularityDeployRequest);

    scheduler.drainPendingQueue();

    SingularityPendingTask task1 = createAndSchedulePendingTask(firstDeployId);

    Assert.assertEquals(1, taskManager.getPendingTaskIds().size());
    Assert.assertEquals(PendingType.NEW_DEPLOY, taskManager.getPendingTaskIds().get(0).getPendingType());

    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, deploy.getId(), System.currentTimeMillis(), Optional.absent(), PendingType.IMMEDIATE,
        deploy.getSkipHealthchecksOnDeploy(), Optional.absent()));
    scheduler.drainPendingQueue();

    Assert.assertEquals(1, taskManager.getPendingTaskIds().size());
    Assert.assertEquals(PendingType.IMMEDIATE, taskManager.getPendingTaskIds().get(0).getPendingType());
  }

  @Test
  public void testSchedulerDropsMultipleScheduledTaskInstances() {
    initScheduledRequest();

    SingularityDeploy deploy = SingularityDeploy.newBuilder(requestId, firstDeployId)
        .setCommand(Optional.of("sleep 100"))
        .build();
    SingularityDeployRequest singularityDeployRequest = new SingularityDeployRequest(deploy, Optional.absent(), Optional.absent(), Optional.absent());
    deployResource.deploy(singularityDeployRequest);


    scheduler.drainPendingQueue();
    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, firstDeployId, Instant.now().plus(3, ChronoUnit.DAYS).toEpochMilli(), Optional.absent(), PendingType.NEW_DEPLOY, Optional.absent(), Optional.absent()));

    SingularityRunNowRequest runNowRequest = new SingularityRunNowRequest(Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent());
    requestResource.scheduleImmediately(requestId, runNowRequest);




    Assert.assertEquals("Both requests make it into the pending queue", 2, requestManager.getPendingRequests().size());
    Assert.assertEquals(PendingType.IMMEDIATE, requestManager.getPendingRequests().get(0).getPendingType());
    Assert.assertEquals(PendingType.NEW_DEPLOY, requestManager.getPendingRequests().get(1).getPendingType());


    scheduler.drainPendingQueue();
    Assertions.assertThat(taskManager.getPendingTaskIds())
        .describedAs("Only the immediate request gets run")
        .hasSize(1)
        .extracting(SingularityPendingTaskId::getPendingType)
        .containsExactly(PendingType.IMMEDIATE);


    Assertions.assertThat(requestManager.getPendingRequests())
        .describedAs("The scheduled request is dropped from the pending queue")
        .hasSize(0);
  }

  @Test(expected = WebApplicationException.class)
  public void testInvalidQuartzTimeZoneErrors() {
    SingularityRequest req = new SingularityRequestBuilder(requestId, RequestType.SCHEDULED)
        .setQuartzSchedule(Optional.of("*/1 * * * * ? 2020"))
        .setScheduleType(Optional.of(ScheduleType.QUARTZ))
        .setScheduleTimeZone(Optional.of("invalid_timezone"))
        .build();

    requestResource.postRequest(req);
  }

  @Test
  public void testDifferentQuartzTimeZones() {
    final Optional<String> schedule = Optional.of("* 30 14 22 3 ? 2083");

    SingularityRequest requestEST = new SingularityRequestBuilder("est_id", RequestType.SCHEDULED)
        .setSchedule(schedule)
        .setScheduleType(Optional.of(ScheduleType.QUARTZ))
        .setScheduleTimeZone(Optional.of("EST")) // fixed in relation to GMT
        .build();

    SingularityRequest requestGMT = new SingularityRequestBuilder("gmt_id", RequestType.SCHEDULED)
        .setSchedule(schedule)
        .setScheduleType(Optional.of(ScheduleType.QUARTZ))
        .setScheduleTimeZone(Optional.of("GMT"))
        .build();

    requestResource.postRequest(requestEST);
    requestResource.postRequest(requestGMT);

    SingularityDeploy deployEST = new SingularityDeployBuilder(requestEST.getId(), "est_deploy_id")
        .setCommand(Optional.of("sleep 1"))
        .build();

    SingularityDeploy deployGMT = new SingularityDeployBuilder(requestGMT.getId(), "gmt_deploy_id")
        .setCommand(Optional.of("sleep 1"))
        .build();

    deployResource.deploy(new SingularityDeployRequest(deployEST, Optional.<Boolean>absent(), Optional.<String>absent(), Optional.<SingularityRequest>absent()));
    deployResource.deploy(new SingularityDeployRequest(deployGMT, Optional.<Boolean>absent(), Optional.<String>absent(), Optional.<SingularityRequest>absent()));

    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();

    final long nextRunEST;
    final long nextRunGMT;
    final long fiveHoursInMilliseconds = TimeUnit.HOURS.toMillis(5);
    final List<SingularityPendingTaskId> pendingTaskIds = taskManager.getPendingTaskIds();
    if (pendingTaskIds.get(0).getRequestId().equals(requestEST.getId())) {
      nextRunEST = pendingTaskIds.get(0).getNextRunAt();
      nextRunGMT = pendingTaskIds.get(1).getNextRunAt();
    } else {
      nextRunEST = pendingTaskIds.get(1).getNextRunAt();
      nextRunGMT = pendingTaskIds.get(0).getNextRunAt();
    }

    // GMT happens first, so EST is a larger timestamp
    Assert.assertEquals(nextRunEST - nextRunGMT, fiveHoursInMilliseconds);
  }

  @Test
  public void testDeployCleanupOverwritesTaskBounceCleanup() {
    initRequest();
    initFirstDeploy();
    final SingularityTask oldTask = startTask(firstDeploy);

    taskResource
      .killTask(oldTask.getTaskId().getId(), Optional.of(new SingularityKillTaskRequest(Optional.<Boolean> absent(), Optional.<String> absent(), Optional.<String> absent(), Optional.of(true), Optional.<SingularityShellCommand>absent())));

    final Optional<SingularityTaskCleanup> taskCleanup = taskManager.getTaskCleanup(oldTask.getTaskId().getId());
    Assert.assertTrue(taskCleanup.isPresent());
    Assert.assertEquals(TaskCleanupType.USER_REQUESTED_TASK_BOUNCE, taskCleanup.get().getCleanupType());

    initSecondDeploy();
    startTask(secondDeploy);
    deployChecker.checkDeploys();

    Assert.assertEquals(DeployState.SUCCEEDED, deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState());
    Assert.assertEquals(TaskCleanupType.DEPLOY_STEP_FINISHED, taskManager.getTaskCleanup(oldTask.getTaskId().getId()).get().getCleanupType());

    cleaner.drainCleanupQueue();

    Assert.assertFalse(taskManager.getTaskCleanup(oldTask.getTaskId().getId()).isPresent());
  }

  @Test
  public void testCleanerFindsTasksWithSkippedHealthchecks() {
    initRequest();
    resourceOffers(2); // set up slaves so scale validate will pass

    SingularityRequest request = requestResource.getRequest(requestId).getRequest();

    long now = System.currentTimeMillis();

    requestManager.saveHistory(new SingularityRequestHistory(now, Optional.<String>absent(), RequestHistoryType.UPDATED,
      request.toBuilder()
        .setSkipHealthchecks(Optional.of(true))
        .setInstances(Optional.of(2))
        .build(),
      Optional.<String>absent()));

    firstDeploy = initDeploy(new SingularityDeployBuilder(request.getId(), firstDeployId).setCommand(Optional.of("sleep 100")).setHealthcheckUri(Optional.of("http://uri")), System.currentTimeMillis());

    SingularityTask taskOne = launchTask(request, firstDeploy, now + 1000, now + 2000, 1, TaskState.TASK_RUNNING);

    finishDeploy(new SingularityDeployMarker(requestId, firstDeployId, now + 2000, Optional.<String> absent(), Optional.<String> absent()), firstDeploy);

    SingularityRequest updatedRequest = request.toBuilder()
      .setSkipHealthchecks(Optional.<Boolean>absent())
      .setInstances(Optional.of(2))
      .build();

    requestManager.saveHistory(new SingularityRequestHistory(now + 3000, Optional.<String>absent(), RequestHistoryType.UPDATED,
      updatedRequest, Optional.<String>absent()));

    SingularityTask newTaskTwoWithCheck = prepTask(updatedRequest, firstDeploy, now + 4000, 2);
    taskManager.createTaskAndDeletePendingTask(newTaskTwoWithCheck);
    statusUpdate(newTaskTwoWithCheck, TaskState.TASK_RUNNING, Optional.of(now + 5000));
    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(200), Optional.of(1000L), now + 6000, Optional.<String> absent(), Optional.<String> absent(), newTaskTwoWithCheck.getTaskId(), Optional.<Boolean>absent()));

    SingularityTask unhealthyTaskThree = prepTask(updatedRequest, firstDeploy, now + 4000, 3);
    taskManager.createTaskAndDeletePendingTask(unhealthyTaskThree);
    statusUpdate(unhealthyTaskThree, TaskState.TASK_RUNNING, Optional.of(now + 5000));

    List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIdsForRequest(requestId);
    List<SingularityTaskId> healthyTaskIds = deployHealthHelper.getHealthyTasks(updatedRequest, Optional.of(firstDeploy), activeTaskIds, false);
    Assert.assertTrue(!healthyTaskIds.contains(unhealthyTaskThree.getTaskId()));
    Assert.assertEquals(2, healthyTaskIds.size()); // Healthchecked and skip-healthchecked tasks should both be here
    Assert.assertEquals(DeployHealth.WAITING, deployHealthHelper.getDeployHealth(updatedRequest, Optional.of(firstDeploy), activeTaskIds, false));

    taskManager.saveHealthcheckResult(new SingularityTaskHealthcheckResult(Optional.of(200), Optional.of(1000L), now + 6000, Optional.<String> absent(), Optional.<String> absent(), unhealthyTaskThree.getTaskId(), Optional.<Boolean>absent()));
    Assert.assertEquals(DeployHealth.HEALTHY, deployHealthHelper.getDeployHealth(updatedRequest, Optional.of(firstDeploy), activeTaskIds, false));
  }

  @Test
  public void testScaleWithBounceDoesNotLaunchExtraInstances() {
    initRequest();
    initFirstDeploy();
    launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(5), Optional.of(1L), Optional.<Boolean> absent(), Optional.<String> absent(), Optional.<String>absent(), Optional.of(true), Optional.<Boolean>absent()));

    Assert.assertEquals(1, requestManager.getCleanupRequests().size());
    cleaner.drainCleanupQueue();
    Assert.assertEquals(1, taskManager.getNumCleanupTasks());

    scheduler.drainPendingQueue();
    Assert.assertEquals(5, taskManager.getPendingTaskIds().size());
  }

  @Test
  public void testAcceptOffersWithRoleForRequestWithRole() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId, RequestType.ON_DEMAND);
    bldr.setRequiredRole(Optional.of("test-role"));
    requestResource.postRequest(bldr.build());
    deploy("d2");

    SingularityRunNowRequest runNowRequest = new SingularityRunNowRequest(Optional.<String>absent(), Optional.<Boolean>absent(), Optional.<String>absent(), Optional.<List<String>>absent(), Optional.of(new Resources(2, 2, 0)), Optional.<Long>absent());
    requestResource.scheduleImmediately(requestId, runNowRequest);

    scheduler.drainPendingQueue();

    SingularityPendingTask pendingTaskWithResources = taskManager.getPendingTasks().get(0);
    Assert.assertTrue(pendingTaskWithResources.getResources().isPresent());
    Assert.assertEquals(pendingTaskWithResources.getResources().get().getCpus(), 2, 0.0);

    sms.resourceOffers(Arrays.asList(createOffer(5, 5)));

    pendingTaskWithResources = taskManager.getPendingTasks().get(0);
    Assert.assertTrue(pendingTaskWithResources.getResources().isPresent());
    Assert.assertEquals(pendingTaskWithResources.getResources().get().getCpus(), 2, 0.0);

    sms.resourceOffers(Arrays.asList(createOffer(5, 5, Optional.of("test-role"))));
    SingularityTask task = taskManager.getActiveTasks().get(0);
    Assert.assertEquals(MesosUtils.getNumCpus(task.getMesosTask().getResources(), Optional.of("test-role")), 2.0, 0.0);
  }

  @Test
  public void testNotAcceptOfferWithRoleForRequestWithoutRole() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId, RequestType.ON_DEMAND);
    requestResource.postRequest(bldr.build());
    deploy("d2");

    SingularityRunNowRequest runNowRequest = new SingularityRunNowRequest(Optional.<String>absent(), Optional.<Boolean>absent(), Optional.<String>absent(), Optional.<List<String>>absent(), Optional.of(new Resources(2, 2, 0)), Optional.<Long>absent());
    requestResource.scheduleImmediately(requestId, runNowRequest);

    scheduler.drainPendingQueue();

    SingularityPendingTask pendingTaskWithResources = taskManager.getPendingTasks().get(0);
    Assert.assertTrue(pendingTaskWithResources.getResources().isPresent());
    Assert.assertEquals(pendingTaskWithResources.getResources().get().getCpus(), 2, 0.0);

    sms.resourceOffers(Arrays.asList(createOffer(5, 5, Optional.of("test-role"))));

    pendingTaskWithResources = taskManager.getPendingTasks().get(0);
    Assert.assertTrue(pendingTaskWithResources.getResources().isPresent());
    Assert.assertEquals(pendingTaskWithResources.getResources().get().getCpus(), 2, 0.0);
  }

  @Test
  public void testMaxOnDemandTasks() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId, RequestType.ON_DEMAND);
    bldr.setInstances(Optional.of(1));
    requestResource.postRequest(bldr.build());
    deploy("on_demand_deploy");
    deployChecker.checkDeploys();


    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, "on_demand_deploy", System.currentTimeMillis(), Optional.<String>absent(), PendingType.ONEOFF,
        Optional.<List<String>>absent(), Optional.<String>absent(), Optional.<Boolean>absent(), Optional.<String>absent(), Optional.<String>absent()));
    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, "on_demand_deploy", System.currentTimeMillis(), Optional.<String>absent(), PendingType.ONEOFF,
        Optional.<List<String>>absent(), Optional.<String>absent(), Optional.<Boolean>absent(), Optional.<String>absent(), Optional.<String>absent()));

    scheduler.drainPendingQueue();

    resourceOffers();

    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());
  }
}
