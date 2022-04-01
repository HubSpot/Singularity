package com.hubspot.singularity.scheduler;

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
import com.hubspot.mesos.protos.MesosTaskState;
import com.hubspot.singularity.AgentPlacement;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.LoadBalancerRequestState;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
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
import com.hubspot.singularity.SingularityLoadBalancerUpdate.LoadBalancerMethod;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskBuilder;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityPriorityFreezeParent;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestLbCleanup;
import com.hubspot.singularity.SingularityRunNowRequestBuilder;
import com.hubspot.singularity.SingularityShellCommand;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.api.SingularityDeleteRequestRequest;
import com.hubspot.singularity.api.SingularityDeployRequest;
import com.hubspot.singularity.api.SingularityKillTaskRequest;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityPriorityFreeze;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.data.AbstractMachineManager.StateChangeResult;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.singularity.mesos.OfferCache;
import com.hubspot.singularity.mesos.SingularityMesosStatusUpdateHandler;
import com.hubspot.singularity.mesos.SingularityMesosTaskPrioritizer;
import com.hubspot.singularity.scheduler.SingularityDeployHealthHelper.DeployHealth;
import com.hubspot.singularity.scheduler.SingularityTaskReconciliation.ReconciliationState;
import com.jayway.awaitility.Awaitility;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.ws.rs.WebApplicationException;
import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskState;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.Protos.TaskStatus.Reason;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

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

  @Inject
  private MesosProtosUtils mesosProtosUtils;

  @Inject
  SingularityMesosStatusUpdateHandler updateHandler;

  public SingularitySchedulerTest() {
    super(false);
  }

  private SingularityPendingTask pendingTask(
    String requestId,
    String deployId,
    PendingType pendingType
  ) {
    return new SingularityPendingTaskBuilder()
      .setPendingTaskId(
        new SingularityPendingTaskId(
          requestId,
          deployId,
          System.currentTimeMillis(),
          1,
          pendingType,
          System.currentTimeMillis()
        )
      )
      .build();
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

    requestResource.postRequest(
      request
        .toBuilder()
        .setAgentPlacement(Optional.of(AgentPlacement.SEPARATE))
        .setInstances(Optional.of(2))
        .build(),
      singularityUser
    );

    schedulerPoller.runActionOnPoll();

    Assertions.assertEquals(0, taskManager.getActiveTasks().size());

    resourceOffers();

    int numTasks = taskManager.getActiveTasks().size();

    Assertions.assertEquals(2, numTasks);

    startAndDeploySecondRequest();

    schedulerPoller.runActionOnPoll();

    Assertions.assertEquals(numTasks, taskManager.getActiveTasks().size());

    resourceOffers();

    Assertions.assertTrue(taskManager.getActiveTasks().size() > numTasks);
  }

  @Test
  public void testSchedulerIsolatesPendingTasksBasedOnDeploy() {
    initRequest();
    initFirstDeploy();
    initSecondDeploy();

    SingularityPendingTask p1 = pendingTask(requestId, firstDeployId, PendingType.ONEOFF);
    SingularityPendingTask p2 = pendingTask(
      requestId,
      firstDeployId,
      PendingType.TASK_DONE
    );
    SingularityPendingTask p3 = pendingTask(
      requestId,
      secondDeployId,
      PendingType.TASK_DONE
    );

    taskManager.savePendingTask(p1);
    taskManager.savePendingTask(p2);
    taskManager.savePendingTask(p3);

    requestManager.addToPendingQueue(
      new SingularityPendingRequest(
        requestId,
        secondDeployId,
        System.currentTimeMillis(),
        Optional.<String>empty(),
        PendingType.NEW_DEPLOY,
        Optional.<Boolean>empty(),
        Optional.<String>empty()
      )
    );

    scheduler.drainPendingQueue();

    // we expect there to be 3 pending tasks :

    List<SingularityPendingTask> returnedScheduledTasks = taskManager.getPendingTasks();

    Assertions.assertEquals(3, returnedScheduledTasks.size());
    Assertions.assertTrue(returnedScheduledTasks.contains(p1));
    Assertions.assertTrue(returnedScheduledTasks.contains(p2));
    Assertions.assertTrue(!returnedScheduledTasks.contains(p3));

    boolean found = false;

    for (SingularityPendingTask pendingTask : returnedScheduledTasks) {
      if (pendingTask.getPendingTaskId().getDeployId().equals(secondDeployId)) {
        found = true;
        Assertions.assertEquals(
          PendingType.NEW_DEPLOY,
          pendingTask.getPendingTaskId().getPendingType()
        );
      }
    }

    Assertions.assertTrue(found);
  }

  @Test
  public void testCleanerLeavesPausedRequestTasksByDemand() {
    initScheduledRequest();
    initFirstDeploy();

    SingularityTask firstTask = launchTask(
      request,
      firstDeploy,
      1,
      TaskState.TASK_RUNNING
    );
    createAndSchedulePendingTask(firstDeployId);

    requestResource.pause(
      requestId,
      Optional.of(
        new SingularityPauseRequest(
          Optional.of(false),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        )
      ),
      singularityUser
    );

    cleaner.drainCleanupQueue();

    Assertions.assertTrue(taskManager.getKilledTaskIdRecords().isEmpty());
    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assertions.assertTrue(requestManager.getCleanupRequests().isEmpty());

    statusUpdate(firstTask, TaskState.TASK_FINISHED);

    // make sure something new isn't scheduled!
    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());
  }

  @Test
  public void testTaskKill() {
    initRequest();
    initFirstDeploy();

    SingularityTask firstTask = startTask(firstDeploy);

    taskResource.killTask(
      firstTask.getTaskId().getId(),
      Optional.empty(),
      singularityUser
    );

    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assertions.assertEquals(0, taskManager.getNumCleanupTasks());
    Assertions.assertEquals(0, taskManager.getNumActiveTasks());
  }

  @Test
  public void testTaskDestroy() {
    initRequest();
    initFirstDeploy();

    SingularityTask firstTask = startTask(firstDeploy, 1);
    SingularityTask secondTask = startTask(firstDeploy, 2);
    SingularityTask thirdTask = startTask(firstDeploy, 3);

    taskResource.killTask(
      secondTask.getTaskId().getId(),
      Optional.of(
        new SingularityKillTaskRequest(
          Optional.of(true),
          Optional.of("kill -9 bb"),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        )
      ),
      singularityUser
    );

    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assertions.assertEquals(2, taskManager.getNumActiveTasks());
    System.out.println(requestManager.getCleanupRequests());
    Assertions.assertEquals(0, requestManager.getCleanupRequests().size());
    Assertions.assertEquals(
      RequestState.ACTIVE,
      requestManager.getRequest(requestId).get().getState()
    );
  }

  @Test
  public void testTaskBounce() {
    initRequest();
    initFirstDeploy();

    SingularityTask firstTask = startTask(firstDeploy);

    taskResource.killTask(
      firstTask.getTaskId().getId(),
      Optional.of(
        new SingularityKillTaskRequest(
          Optional.empty(),
          Optional.of("msg"),
          Optional.empty(),
          Optional.of(true),
          Optional.empty()
        )
      ),
      singularityUser
    );

    cleaner.drainCleanupQueue();

    killKilledTasks();

    Assertions.assertEquals(1, taskManager.getNumCleanupTasks());
    Assertions.assertEquals(0, taskManager.getKilledTaskIdRecords().size());

    resourceOffers();
    runLaunchedTasks();

    Assertions.assertEquals(1, taskManager.getNumCleanupTasks());
    Assertions.assertEquals(0, taskManager.getKilledTaskIdRecords().size());
    Assertions.assertEquals(2, taskManager.getNumActiveTasks());

    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assertions.assertEquals(0, taskManager.getNumCleanupTasks());
    Assertions.assertEquals(1, taskManager.getNumActiveTasks());
  }

  @Test
  public void testBounceWithLoadBalancer() {
    initLoadBalancedRequest();
    initFirstDeploy();
    configuration.setNewTaskCheckerBaseDelaySeconds(1000000);

    SingularityTask taskOne = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    saveLoadBalancerState(
      LoadBalancerRequestState.SUCCESS,
      taskOne.getTaskId(),
      LoadBalancerRequestType.ADD
    );

    requestResource.bounce(requestId, Optional.empty(), singularityUser);

    cleaner.drainCleanupQueue();
    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(2, taskManager.getNumActiveTasks());

    List<SingularityTaskId> tasks = taskManager.getActiveTaskIds();
    tasks.remove(taskOne.getTaskId());

    SingularityTaskId taskTwo = tasks.get(0);

    cleaner.drainCleanupQueue();

    runLaunchedTasks();

    cleaner.drainCleanupQueue();

    Assertions.assertEquals(0, taskManager.getKilledTaskIdRecords().size());
    Assertions.assertEquals(2, taskManager.getNumActiveTasks());

    // add to LB:
    saveLoadBalancerState(
      LoadBalancerRequestState.SUCCESS,
      taskTwo,
      LoadBalancerRequestType.ADD
    );

    cleaner.drainCleanupQueue();

    Assertions.assertEquals(0, taskManager.getKilledTaskIdRecords().size());
    Assertions.assertEquals(2, taskManager.getNumActiveTasks());

    saveLoadBalancerState(
      LoadBalancerRequestState.SUCCESS,
      taskOne.getTaskId(),
      LoadBalancerRequestType.REMOVE
    );

    cleaner.drainCleanupQueue();

    Assertions.assertEquals(1, taskManager.getKilledTaskIdRecords().size());

    killKilledTasks();

    Assertions.assertEquals(1, taskManager.getNumActiveTasks());
  }

  @Test
  public void testKilledTaskIdRecords() {
    initScheduledRequest();
    initFirstDeploy();

    launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    requestResource.deleteRequest(requestId, Optional.empty(), singularityUser);

    Assertions.assertTrue(requestManager.getCleanupRequests().size() == 1);

    cleaner.drainCleanupQueue();

    Assertions.assertTrue(!taskManager.getKilledTaskIdRecords().isEmpty());

    killKilledTasks();
    cleaner.drainCleanupQueue();

    Assertions.assertTrue(requestManager.getCleanupRequests().isEmpty());
    Assertions.assertTrue(taskManager.getKilledTaskIdRecords().isEmpty());
  }

  @Test
  public void testLongRunningTaskKills() {
    initScheduledRequest();
    initFirstDeploy();

    launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    initSecondDeploy();
    deployChecker.checkDeploys();

    Assertions.assertTrue(taskManager.getKilledTaskIdRecords().isEmpty());
    Assertions.assertTrue(!taskManager.getCleanupTasks().isEmpty());

    cleaner.drainCleanupQueue();

    Assertions.assertTrue(taskManager.getKilledTaskIdRecords().isEmpty());
    Assertions.assertTrue(!taskManager.getCleanupTasks().isEmpty());

    requestManager.activate(
      request
        .toBuilder()
        .setKillOldNonLongRunningTasksAfterMillis(Optional.<Long>of(0L))
        .build(),
      RequestHistoryType.CREATED,
      System.currentTimeMillis(),
      Optional.<String>empty(),
      Optional.<String>empty()
    );

    cleaner.drainCleanupQueue();

    Assertions.assertTrue(!taskManager.getKilledTaskIdRecords().isEmpty());
    Assertions.assertTrue(taskManager.getCleanupTasks().isEmpty());
  }

  @Test
  public void testSchedulerCanBatchOnOffers() {
    initRequest();
    initFirstDeploy();

    requestResource.postRequest(
      request.toBuilder().setInstances(Optional.of(3)).build(),
      singularityUser
    );
    scheduler.drainPendingQueue();

    List<Offer> oneOffer = Arrays.asList(createOffer(12, 1024, 5000));
    sms.resourceOffers(oneOffer).join();

    Assertions.assertTrue(taskManager.getActiveTasks().size() == 3);
    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assertions.assertTrue(requestManager.getPendingRequests().isEmpty());
  }

  @Test
  public void testSchedulerExhaustsOffers() {
    initRequest();
    initFirstDeploy();

    requestResource.postRequest(
      request.toBuilder().setInstances(Optional.of(10)).build(),
      singularityUser
    );
    scheduler.drainPendingQueue();

    sms
      .resourceOffers(
        Arrays.asList(createOffer(2, 1024, 2048), createOffer(1, 1024, 2048))
      )
      .join();

    Assertions.assertEquals(3, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals(7, taskManager.getPendingTaskIds().size());
  }

  @Test
  public void testSchedulerRandomizesOffers() {
    initRequest();
    initFirstDeploy();

    requestResource.postRequest(
      request.toBuilder().setInstances(Optional.of(15)).build(),
      singularityUser
    );
    scheduler.drainPendingQueue();

    sms
      .resourceOffers(
        Arrays.asList(createOffer(20, 1024, 20000), createOffer(20, 1024, 20000))
      )
      .join();

    Assertions.assertEquals(15, taskManager.getActiveTaskIds().size());

    Set<String> offerIds = Sets.newHashSet();

    for (SingularityTask activeTask : taskManager.getActiveTasks()) {
      offerIds.addAll(
        activeTask
          .getOffers()
          .stream()
          .map(o -> o.getId().getValue())
          .collect(Collectors.toList())
      );
    }

    Assertions.assertEquals(2, offerIds.size());
  }

  @Test
  public void testSchedulerHandlesFinishedTasks() {
    initScheduledRequest();
    initFirstDeploy();

    schedule = "*/1 * * * * ? 1995";

    // cause it to be pending
    requestResource.postRequest(
      request.toBuilder().setQuartzSchedule(Optional.of(schedule)).build(),
      singularityUser
    );
    scheduler.drainPendingQueue();

    Assertions.assertTrue(
      requestResource
        .getActiveRequests(
          singularityUser,
          false,
          false,
          false,
          10,
          Collections.emptyList()
        )
        .isEmpty()
    );
    Assertions.assertTrue(
      requestManager.getRequest(requestId).get().getState() == RequestState.FINISHED
    );
    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());

    schedule = "*/1 * * * * ?";
    requestResource.postRequest(
      request.toBuilder().setQuartzSchedule(Optional.of(schedule)).build(),
      singularityUser
    );
    scheduler.drainPendingQueue();

    Assertions.assertTrue(
      !requestResource
        .getActiveRequests(
          singularityUser,
          false,
          false,
          false,
          10,
          Collections.emptyList()
        )
        .isEmpty()
    );
    Assertions.assertTrue(
      requestManager.getRequest(requestId).get().getState() == RequestState.ACTIVE
    );

    Assertions.assertTrue(!taskManager.getPendingTaskIds().isEmpty());
  }

  @Test
  public void testNewlyDeployedScheduledTasksAreScheduledAfterStartup() {
    initScheduledRequest();
    initFirstDeploy();

    SingularityTask runningTask = launchTask(
      request,
      firstDeploy,
      1,
      TaskState.TASK_RUNNING
    );

    long now = System.currentTimeMillis();

    initSecondDeploy();
    requestManager.addToPendingQueue(
      new SingularityPendingRequest(
        requestId,
        secondDeployId,
        now,
        Optional.empty(),
        PendingType.STARTUP,
        Optional.empty(),
        Optional.empty()
      )
    );
    deployChecker.checkDeploys();

    resourceOffers();

    // There's an instance running, so we shouldn't schedule a pending task yet
    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());

    statusUpdate(runningTask, TaskState.TASK_FINISHED);

    scheduler.drainPendingQueue();

    // Now a pending task should be scheduled with the new deploy
    Assertions.assertEquals(1, taskManager.getPendingTaskIds().size());
    Assertions.assertEquals(
      PendingType.NEW_DEPLOY,
      taskManager.getPendingTaskIds().get(0).getPendingType()
    );
    Assertions.assertEquals(
      secondDeployId,
      taskManager.getPendingTaskIds().get(0).getDeployId()
    );
  }

  @Test
  public void testFinishedRequestCanBeDeployed() {
    initScheduledRequest();
    initFirstDeploy();

    schedule = "*/1 * * * * ? 1995";

    // cause it to be pending
    requestResource.postRequest(
      request.toBuilder().setQuartzSchedule(Optional.of(schedule)).build(),
      singularityUser
    );
    scheduler.drainPendingQueue();

    Assertions.assertTrue(
      requestResource
        .getActiveRequests(
          singularityUser,
          false,
          false,
          false,
          10,
          Collections.emptyList()
        )
        .isEmpty()
    );
    Assertions.assertTrue(
      requestManager.getRequest(requestId).get().getState() == RequestState.FINISHED
    );

    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, secondDeployId);
    initDeploy(db, System.currentTimeMillis());
    deployChecker.checkDeploys();
    Assertions.assertEquals(
      RequestState.ACTIVE,
      requestManager.getRequest(requestId).get().getState()
    );
    Assertions.assertEquals(1, requestManager.getPendingRequests().size());
  }

  @Test
  public void testOneOffsDontRunByThemselves() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(
      requestId,
      RequestType.ON_DEMAND
    );
    requestResource.postRequest(bldr.build(), singularityUser);
    Assertions.assertTrue(requestManager.getPendingRequests().isEmpty());

    deploy("d2");
    Assertions.assertTrue(requestManager.getPendingRequests().isEmpty());

    deployChecker.checkDeploys();

    Assertions.assertTrue(requestManager.getPendingRequests().isEmpty());

    requestResource.scheduleImmediately(
      singularityUser,
      requestId,
      ((SingularityRunNowRequest) null)
    );

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_FINISHED);

    scheduler.drainPendingQueue();
    resourceOffers();
    Assertions.assertEquals(0, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals(0, taskManager.getPendingTaskIds().size());

    requestResource.scheduleImmediately(
      singularityUser,
      requestId,
      ((SingularityRunNowRequest) null)
    );

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    scheduler.drainPendingQueue();
    resourceOffers();
    Assertions.assertEquals(0, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals(0, taskManager.getPendingTaskIds().size());
  }

  @Test
  public void testOneOffsDontMoveDuringDecomission() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(
      requestId,
      RequestType.ON_DEMAND
    );
    requestResource.postRequest(bldr.build(), singularityUser);
    deploy("d2");

    requestResource.scheduleImmediately(
      singularityUser,
      requestId,
      ((SingularityRunNowRequest) null)
    );

    validateTaskDoesntMoveDuringDecommission();
  }

  private void validateTaskDoesntMoveDuringDecommission() {
    scheduler.drainPendingQueue();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    scheduler.drainPendingQueue();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent2", "host2", Optional.of("rack1")))
      )
      .join();

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());

    Assertions.assertEquals(
      "host1",
      taskManager.getActiveTaskIds().get(0).getSanitizedHost()
    );

    Assertions.assertEquals(
      StateChangeResult.SUCCESS,
      agentManager.changeState(
        "agent1",
        MachineState.STARTING_DECOMMISSION,
        Optional.<String>empty(),
        Optional.of("user1")
      )
    );

    scheduler.checkForDecomissions();
    scheduler.drainPendingQueue();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent2", "host2", Optional.of("rack1")))
      )
      .join();

    cleaner.drainCleanupQueue();

    scheduler.drainPendingQueue();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent2", "host2", Optional.of("rack1")))
      )
      .join();

    cleaner.drainCleanupQueue();

    // task should not move!
    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals(
      "host1",
      taskManager.getActiveTaskIds().get(0).getSanitizedHost()
    );
    Assertions.assertTrue(taskManager.getKilledTaskIdRecords().isEmpty());
    Assertions.assertTrue(taskManager.getCleanupTaskIds().size() == 1);
  }

  @Test
  public void testCustomResourcesWithRunNowRequest() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(
      requestId,
      RequestType.ON_DEMAND
    );
    requestResource.postRequest(bldr.build(), singularityUser);
    deploy("d2");

    SingularityRunNowRequest runNowRequest = new SingularityRunNowRequestBuilder()
      .setResources(new Resources(2, 2, 0))
      .build();
    requestResource.scheduleImmediately(singularityUser, requestId, runNowRequest);

    scheduler.drainPendingQueue();

    SingularityPendingTask pendingTaskWithResourcs = taskManager.getPendingTasks().get(0);
    Assertions.assertTrue(pendingTaskWithResourcs.getResources().isPresent());
    Assertions.assertEquals(
      pendingTaskWithResourcs.getResources().get().getCpus(),
      2,
      0.0
    );

    sms
      .resourceOffers(
        Arrays.asList(createOffer(5, 5, 5, "agent1", "host1", Optional.of("rack1")))
      )
      .join();

    SingularityTask task = taskManager.getActiveTasks().get(0);
    Assertions.assertEquals(
      MesosUtils.getNumCpus(
        mesosProtosUtils.toResourceList(task.getMesosTask().getResources()),
        Optional.<String>empty()
      ),
      2.0,
      0.0
    );
  }

  @Test
  public void testRunOnceRunOnlyOnce() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(
      requestId,
      RequestType.RUN_ONCE
    );
    request = bldr.build();
    saveRequest(request);

    deployResource.deploy(
      new SingularityDeployRequest(
        new SingularityDeployBuilder(requestId, "d1")
          .setCommand(Optional.of("cmd"))
          .build(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    scheduler.drainPendingQueue();

    deployChecker.checkDeploys();

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertTrue(
      deployManager.getRequestDeployState(requestId).get().getActiveDeploy().isPresent()
    );
    Assertions.assertTrue(
      !deployManager.getRequestDeployState(requestId).get().getPendingDeploy().isPresent()
    );

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertTrue(taskManager.getActiveTaskIds().isEmpty());

    deployResource.deploy(
      new SingularityDeployRequest(
        new SingularityDeployBuilder(requestId, "d2")
          .setCommand(Optional.of("cmd"))
          .build(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    scheduler.drainPendingQueue();

    deployChecker.checkDeploys();

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertTrue(
      deployManager.getRequestDeployState(requestId).get().getActiveDeploy().isPresent()
    );
    Assertions.assertTrue(
      !deployManager.getRequestDeployState(requestId).get().getPendingDeploy().isPresent()
    );

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_FINISHED);

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertTrue(taskManager.getActiveTaskIds().isEmpty());
  }

  @Test
  public void testMultipleRunOnceTasks() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(
      requestId,
      RequestType.RUN_ONCE
    );
    request = bldr.build();
    saveRequest(request);

    deployResource.deploy(
      new SingularityDeployRequest(
        new SingularityDeployBuilder(requestId, "d1")
          .setCommand(Optional.of("cmd"))
          .build(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );
    deployChecker.checkDeploys();
    Assertions.assertEquals(1, requestManager.getSizeOfPendingQueue());

    deployResource.deploy(
      new SingularityDeployRequest(
        new SingularityDeployBuilder(requestId, "d2")
          .setCommand(Optional.of("cmd"))
          .build(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );
    deployChecker.checkDeploys();
    Assertions.assertEquals(2, requestManager.getSizeOfPendingQueue());

    scheduler.drainPendingQueue();

    resourceOffers();
    Assertions.assertEquals(2, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testRunOnceDontMoveDuringDecomission() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(
      requestId,
      RequestType.RUN_ONCE
    );
    request = bldr.build();
    saveRequest(request);

    deployResource.deploy(
      new SingularityDeployRequest(
        new SingularityDeployBuilder(requestId, "d1")
          .setCommand(Optional.of("cmd"))
          .build(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    scheduler.drainPendingQueue();

    deployChecker.checkDeploys();

    validateTaskDoesntMoveDuringDecommission();
  }

  @Test
  public void testDecommissionDoesntKillPendingDeploy() {
    initRequest();

    deployResource.deploy(
      new SingularityDeployRequest(
        new SingularityDeployBuilder(requestId, "d1")
          .setCommand(Optional.of("cmd"))
          .build(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    scheduler.drainPendingQueue();
    deployChecker.checkDeploys();
    resourceOffers();

    Assertions.assertEquals(1, taskManager.getNumActiveTasks());

    agentResource.decommissionAgent(
      singularityUser,
      taskManager.getActiveTasks().get(0).getAgentId().getValue(),
      null
    );

    scheduler.checkForDecomissions();

    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assertions.assertEquals(1, taskManager.getNumActiveTasks());
    Assertions.assertEquals(1, taskManager.getNumCleanupTasks());
    Assertions.assertEquals(0, taskManager.getKilledTaskIdRecords().size());

    configuration.setPendingDeployHoldTaskDuringDecommissionMillis(1);

    try {
      Thread.sleep(2);
    } catch (InterruptedException e) {}

    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assertions.assertEquals(0, taskManager.getNumActiveTasks());
    Assertions.assertEquals(0, taskManager.getNumCleanupTasks());
  }

  @Test
  public void testRetries() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(
      requestId,
      RequestType.RUN_ONCE
    );
    request = bldr.setNumRetriesOnFailure(Optional.of(2)).build();
    saveRequest(request);

    deployResource.deploy(
      new SingularityDeployRequest(
        new SingularityDeployBuilder(requestId, "d1")
          .setCommand(Optional.of("cmd"))
          .build(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    scheduler.drainPendingQueue();
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertTrue(taskManager.getActiveTaskIds().isEmpty());
  }

  @Test
  public void testRetriesWithOverrides() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(
      requestId,
      RequestType.ON_DEMAND
    );
    request = bldr.setNumRetriesOnFailure(Optional.of(2)).build();
    saveRequest(request);

    deployResource.deploy(
      new SingularityDeployRequest(
        new SingularityDeployBuilder(requestId, "d1")
          .setCommand(Optional.of("cmd"))
          .build(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    scheduler.drainPendingQueue();
    deployChecker.checkDeploys();

    requestResource.scheduleImmediately(
      singularityUser,
      requestId,
      new SingularityRunNowRequestBuilder()
        .setCommandLineArgs(Collections.singletonList("extraFlag"))
        .setResources(new Resources(17, 1337, 0))
        .build()
    );

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());

    Resources resourcesForRunningTask = taskManager
      .getActiveTasks()
      .get(0)
      .getTaskRequest()
      .getPendingTask()
      .getResources()
      .get();
    Assertions.assertEquals(
      Optional.of(Collections.singletonList("extraFlag")),
      taskManager
        .getActiveTasks()
        .get(0)
        .getTaskRequest()
        .getPendingTask()
        .getCmdLineArgsList()
    );
    Assertions.assertEquals(17, resourcesForRunningTask.getCpus(), 0.01);
    Assertions.assertEquals(1337, resourcesForRunningTask.getMemoryMb(), 0.01);

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());

    resourcesForRunningTask =
      taskManager
        .getActiveTasks()
        .get(0)
        .getTaskRequest()
        .getPendingTask()
        .getResources()
        .get();
    Assertions.assertEquals(
      Optional.of(Collections.singletonList("extraFlag")),
      taskManager
        .getActiveTasks()
        .get(0)
        .getTaskRequest()
        .getPendingTask()
        .getCmdLineArgsList()
    );
    Assertions.assertEquals(17, resourcesForRunningTask.getCpus(), 0.01);
    Assertions.assertEquals(1337, resourcesForRunningTask.getMemoryMb(), 0.01);

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());

    resourcesForRunningTask =
      taskManager
        .getActiveTasks()
        .get(0)
        .getTaskRequest()
        .getPendingTask()
        .getResources()
        .get();
    Assertions.assertEquals(
      Optional.of(Collections.singletonList("extraFlag")),
      taskManager
        .getActiveTasks()
        .get(0)
        .getTaskRequest()
        .getPendingTask()
        .getCmdLineArgsList()
    );
    Assertions.assertEquals(17, resourcesForRunningTask.getCpus(), 0.01);
    Assertions.assertEquals(1337, resourcesForRunningTask.getMemoryMb(), 0.01);

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertTrue(taskManager.getActiveTaskIds().isEmpty());
  }

  @Test
  public void testRetriesWithNewDeploys() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(
      requestId,
      RequestType.RUN_ONCE
    );
    request = bldr.setNumRetriesOnFailure(Optional.of(2)).build();
    saveRequest(request);

    deployResource.deploy(
      new SingularityDeployRequest(
        new SingularityDeployBuilder(requestId, "d1")
          .setCommand(Optional.of("cmd"))
          .build(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    scheduler.drainPendingQueue();
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());

    deployResource.deploy(
      new SingularityDeployRequest(
        new SingularityDeployBuilder(requestId, "d2")
          .setCommand(Optional.of("cmd"))
          .build(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    scheduler.drainPendingQueue();
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals("d2", taskManager.getActiveTaskIds().get(0).getDeployId());

    deployResource.deploy(
      new SingularityDeployRequest(
        new SingularityDeployBuilder(requestId, "d3")
          .setCommand(Optional.of("cmd"))
          .build(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    scheduler.drainPendingQueue();
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals("d3", taskManager.getActiveTaskIds().get(0).getDeployId());

    deployResource.deploy(
      new SingularityDeployRequest(
        new SingularityDeployBuilder(requestId, "d4")
          .setCommand(Optional.of("cmd"))
          .build(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_LOST);

    scheduler.drainPendingQueue();
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue();
    resourceOffers();

    // TODO - new deploys have new deploy statistics, so we lose track of the # of retries
    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals("d4", taskManager.getActiveTaskIds().get(0).getDeployId());
  }

  /* @Test
  public void testCooldownAfterSequentialFailures() {
    initRequest();
    initFirstDeploy();

    Assertions.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.ACTIVE);

    configuration.setFastFailureCooldownCount(2);

    SingularityTask firstTask = startTask(firstDeploy);
    SingularityTask secondTask = startTask(firstDeploy);

    statusUpdate(firstTask, TaskState.TASK_FAILED);

    Assertions.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.ACTIVE);

    statusUpdate(secondTask, TaskState.TASK_FAILED);

    Assertions.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.SYSTEM_COOLDOWN);

    cooldownChecker.checkCooldowns();

    Assertions.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.SYSTEM_COOLDOWN);

    SingularityTask thirdTask = startTask(firstDeploy);

    statusUpdate(thirdTask, TaskState.TASK_FINISHED);

    Assertions.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.ACTIVE);
  }

  @Test
  public void testCooldownOnlyWhenTasksRapidlyFail() {
    initRequest();
    initFirstDeploy();

    configuration.setFastFailureCooldownCount(2);

    SingularityTask firstTask = startTask(firstDeploy);
    statusUpdate(firstTask, TaskState.TASK_FAILED, Optional.of(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(5)));

    Assertions.assertTrue(requestManager.getRequest(requestId).get().getState() == RequestState.ACTIVE);

    SingularityTask secondTask = startTask(firstDeploy);
    statusUpdate(secondTask, TaskState.TASK_FAILED);

    Assertions.assertTrue(requestManager.getRequest(requestId).get().getState() != RequestState.SYSTEM_COOLDOWN);
  }*/

  @Test
  public void testLBCleanup() {
    initLoadBalancedRequest();
    initFirstDeploy();

    configuration.setLoadBalancerRemovalGracePeriodMillis(10000);

    SingularityTask task = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    saveLoadBalancerState(
      LoadBalancerRequestState.SUCCESS,
      task.getTaskId(),
      LoadBalancerRequestType.ADD
    );

    statusUpdate(task, TaskState.TASK_FAILED);

    Assertions.assertTrue(!taskManager.getLBCleanupTasks().isEmpty());

    testingLbClient.setNextRequestState(LoadBalancerRequestState.WAITING);

    cleaner.drainCleanupQueue();
    Assertions.assertTrue(!taskManager.getLBCleanupTasks().isEmpty());

    Optional<SingularityLoadBalancerUpdate> lbUpdate = taskManager.getLoadBalancerState(
      task.getTaskId(),
      LoadBalancerRequestType.REMOVE
    );

    Assertions.assertTrue(lbUpdate.isPresent());
    Assertions.assertTrue(
      lbUpdate.get().getLoadBalancerState() == LoadBalancerRequestState.WAITING
    );

    testingLbClient.setNextRequestState(LoadBalancerRequestState.FAILED);

    cleaner.drainCleanupQueue();
    Assertions.assertTrue(!taskManager.getLBCleanupTasks().isEmpty());

    lbUpdate =
      taskManager.getLoadBalancerState(task.getTaskId(), LoadBalancerRequestType.REMOVE);

    Assertions.assertTrue(lbUpdate.isPresent());
    Assertions.assertTrue(
      lbUpdate.get().getLoadBalancerState() == LoadBalancerRequestState.FAILED
    );

    testingLbClient.setNextRequestState(LoadBalancerRequestState.SUCCESS);

    cleaner.drainCleanupQueue();

    Assertions.assertTrue(!taskManager.getLBCleanupTasks().isEmpty());

    configuration.setLoadBalancerRemovalGracePeriodMillis(0);
    cleaner.drainCleanupQueue();

    Assertions.assertTrue(taskManager.getLBCleanupTasks().isEmpty());
    lbUpdate =
      taskManager.getLoadBalancerState(task.getTaskId(), LoadBalancerRequestType.REMOVE);

    Assertions.assertTrue(lbUpdate.isPresent());
    Assertions.assertTrue(
      lbUpdate.get().getLoadBalancerState() == LoadBalancerRequestState.SUCCESS
    );
    Assertions.assertTrue(
      lbUpdate.get().getLoadBalancerRequestId().getAttemptNumber() == 2
    );
  }

  @Test
  public void testLbCleanupDoesNotRemoveBeforeAdd() {
    initLoadBalancedRequest();
    initFirstDeploy();
    SingularityTask taskOne = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    initSecondDeploy();
    SingularityTask taskTwo = launchTask(
      request,
      secondDeploy,
      1,
      TaskState.TASK_RUNNING
    );
    testingLbClient.setNextRequestState(LoadBalancerRequestState.WAITING);
    deployChecker.checkDeploys();

    // First task from old deploy is still starting, never got added to LB so it should not have a removal request
    Assertions.assertFalse(
      taskManager
        .getLoadBalancerState(taskOne.getTaskId(), LoadBalancerRequestType.ADD)
        .isPresent()
    );
    Assertions.assertFalse(
      taskManager
        .getLoadBalancerState(taskOne.getTaskId(), LoadBalancerRequestType.REMOVE)
        .isPresent()
    );

    // Second task should have an add request
    Assertions.assertTrue(
      taskManager
        .getLoadBalancerState(taskTwo.getTaskId(), LoadBalancerRequestType.ADD)
        .isPresent()
    );

    testingLbClient.setNextRequestState(LoadBalancerRequestState.SUCCESS);
    deployChecker.checkDeploys();
    deployChecker.checkDeploys();

    // First task from old deploy should still have no LB updates, but should have a cleanup
    Assertions.assertFalse(
      taskManager
        .getLoadBalancerState(taskOne.getTaskId(), LoadBalancerRequestType.ADD)
        .isPresent()
    );
    Assertions.assertFalse(
      taskManager
        .getLoadBalancerState(taskOne.getTaskId(), LoadBalancerRequestType.REMOVE)
        .isPresent()
    );
    Assertions.assertTrue(taskManager.getCleanupTaskIds().contains(taskOne.getTaskId()));
  }

  @Test
  public void testTaskLaunchesInRackSensitiveWithKillingTask() {
    try {
      configuration.setExpectedRacksCount(Optional.of(3));
      // Set up hosts + racks
      sms
        .resourceOffers(
          Arrays.asList(
            createOffer(1, 1, 1, "agent1", "host1", Optional.of("rack1")),
            createOffer(1, 1, 1, "agent2", "host2", Optional.of("rack2")),
            createOffer(1, 1, 1, "agent3", "host3", Optional.of("rack3"))
          )
        )
        .join();
      initRequest();
      initFirstDeploy();
      requestResource.postRequest(
        request
          .toBuilder()
          .setInstances(Optional.of(3))
          .setRackSensitive(Optional.of(true))
          .setAgentPlacement(Optional.of(AgentPlacement.SEPARATE))
          .build(),
        SingularityUser.DEFAULT_USER
      );
      schedulerPoller.runActionOnPoll();
      Assertions.assertEquals(3, taskManager.getPendingTaskIds().size());
      sms
        .resourceOffers(
          Arrays.asList(
            createOffer(2, 2000, 2000, "agent1", "host1", Optional.of("rack1")),
            createOffer(2, 2000, 2000, "agent2", "host2", Optional.of("rack2")),
            createOffer(2, 2000, 2000, "agent3", "host3", Optional.of("rack3"))
          )
        )
        .join();
      Assertions.assertEquals(3, taskManager.getActiveTaskIds().size());
      SingularityTaskId taskTwo = taskManager
        .getActiveTaskIds()
        .stream()
        .filter(t -> t.getInstanceNo() == 2)
        .findFirst()
        .get();
      SingularityTaskId taskThree = taskManager
        .getActiveTaskIds()
        .stream()
        .filter(t -> t.getInstanceNo() == 3)
        .findFirst()
        .get();
      requestResource.postRequest(
        request
          .toBuilder()
          .setInstances(Optional.of(2))
          .setRackSensitive(Optional.of(true))
          .setAgentPlacement(Optional.of(AgentPlacement.SEPARATE))
          .build(),
        SingularityUser.DEFAULT_USER
      );
      taskManager.createTaskCleanup(
        new SingularityTaskCleanup(
          Optional.empty(),
          TaskCleanupType.USER_REQUESTED,
          System.currentTimeMillis(),
          taskTwo,
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        )
      );
      taskManager.createTaskCleanup(
        new SingularityTaskCleanup(
          Optional.empty(),
          TaskCleanupType.USER_REQUESTED,
          System.currentTimeMillis(),
          taskThree,
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        )
      );
      cleaner.drainCleanupQueue();
      schedulerPoller.runActionOnPoll();
      //Mimic tasks that have been killed (and their cleanup removed) but are slow to shut down
      taskManager.deleteCleanupTask(taskTwo.toString());
      taskManager.deleteCleanupTask(taskThree.toString());
      taskManager.saveKilledRecord(
        new SingularityKilledTaskIdRecord(
          taskTwo,
          System.currentTimeMillis(),
          System.currentTimeMillis(),
          Optional.empty(),
          Optional.empty(),
          1
        )
      );
      taskManager.saveKilledRecord(
        new SingularityKilledTaskIdRecord(
          taskTwo,
          System.currentTimeMillis(),
          System.currentTimeMillis(),
          Optional.empty(),
          Optional.empty(),
          1
        )
      );
      Assertions.assertEquals(3, taskManager.getActiveTaskIds().size());
      Assertions.assertEquals(1, taskManager.getPendingTaskIds().size());
      sms
        .resourceOffers(
          Arrays.asList(
            createOffer(2, 2000, 2000, "agent4", "host4", Optional.of("rack1")),
            createOffer(2, 2000, 2000, "agent5", "host5", Optional.of("rack2")),
            createOffer(2, 2000, 2000, "agent6", "host6", Optional.of("rack3"))
          )
        )
        .join();
      Assertions.assertEquals(4, taskManager.getActiveTaskIds().size());
    } finally {
      configuration.setExpectedRacksCount(Optional.empty());
    }
  }

  @Test
  public void testLbCleanupSkippedOnSkipRemoveFlag() {
    configuration.setDeleteRemovedRequestsFromLoadBalancer(true);
    initLoadBalancedRequest();
    initLoadBalancedDeploy();
    startTask(firstDeploy);

    boolean removeFromLoadBalancer = false;
    SingularityDeleteRequestRequest deleteRequest = new SingularityDeleteRequestRequest(
      Optional.empty(),
      Optional.empty(),
      Optional.of(removeFromLoadBalancer)
    );

    requestResource.deleteRequest(requestId, Optional.of(deleteRequest), singularityUser);

    testingLbClient.setNextRequestState(LoadBalancerRequestState.WAITING);

    Assertions.assertFalse(
      requestManager.getCleanupRequests().isEmpty(),
      "Tasks should get cleaned up"
    );
    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assertions.assertFalse(
      requestManager.getCleanupRequests().isEmpty(),
      "The request should get cleaned up"
    );
    cleaner.drainCleanupQueue();

    Assertions.assertTrue(
      requestManager.getLbCleanupRequestIds().isEmpty(),
      "The request should not be removed from the load balancer"
    );
  }

  @Test
  public void testLbCleanupOccursOnRequestDelete() {
    configuration.setDeleteRemovedRequestsFromLoadBalancer(true);
    initLoadBalancedRequest();
    initLoadBalancedDeploy();
    startTask(firstDeploy);

    requestResource.deleteRequest(requestId, Optional.empty(), singularityUser);

    testingLbClient.setNextRequestState(LoadBalancerRequestState.WAITING);

    Assertions.assertFalse(
      requestManager.getCleanupRequests().isEmpty(),
      "Tasks should get cleaned up"
    );
    cleaner.drainCleanupQueue();
    killKilledTasks();

    Assertions.assertFalse(
      requestManager.getCleanupRequests().isEmpty(),
      "The request should get cleaned up"
    );
    cleaner.drainCleanupQueue();

    Assertions.assertFalse(
      requestManager.getLbCleanupRequestIds().isEmpty(),
      "The request should get removed from the load balancer"
    );
  }

  @Test
  public void testReconciliation() {
    Assertions.assertTrue(!taskReconciliation.isReconciliationRunning());

    configuration.setCheckReconcileWhenRunningEveryMillis(1);

    initRequest();
    initFirstDeploy();

    Assertions.assertTrue(
      taskReconciliation.startReconciliation() == ReconciliationState.STARTED
    );
    Awaitility
      .await()
      .atMost(10, TimeUnit.SECONDS)
      .until(() -> !taskReconciliation.isReconciliationRunning());

    SingularityTask taskOne = launchTask(
      request,
      firstDeploy,
      1,
      TaskState.TASK_STARTING
    );
    SingularityTask taskTwo = launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);

    saveLastActiveTaskStatus(taskOne, Optional.empty(), -1000);

    Assertions.assertTrue(
      taskReconciliation.startReconciliation() == ReconciliationState.STARTED
    );
    Assertions.assertTrue(
      taskReconciliation.startReconciliation() == ReconciliationState.ALREADY_RUNNING
    );

    Awaitility
      .await()
      .atMost(10, TimeUnit.SECONDS)
      .until(() -> taskReconciliation.isReconciliationRunning());

    saveLastActiveTaskStatus(taskOne, Optional.of(buildTaskStatus(taskOne)), +1000);

    Awaitility
      .await()
      .atMost(10, TimeUnit.SECONDS)
      .until(() -> taskReconciliation.isReconciliationRunning());

    saveLastActiveTaskStatus(taskTwo, Optional.of(buildTaskStatus(taskTwo)), +1000);

    Awaitility
      .await()
      .atMost(10, TimeUnit.SECONDS)
      .until(() -> !taskReconciliation.isReconciliationRunning());
  }

  @Test
  public void testSchedulerPriority() {
    final SingularityRequest lowPriorityRequest = new SingularityRequestBuilder(
      "lowPriorityRequest",
      RequestType.WORKER
    )
      .setTaskPriorityLevel(Optional.of(.25))
      .build();
    saveRequest(lowPriorityRequest);
    final SingularityRequest mediumPriorityRequest = new SingularityRequestBuilder(
      "mediumPriorityRequest",
      RequestType.WORKER
    )
      .setTaskPriorityLevel(Optional.of(.5))
      .build();
    saveRequest(mediumPriorityRequest);
    final SingularityRequest highPriorityRequest = new SingularityRequestBuilder(
      "highPriorityRequest",
      RequestType.WORKER
    )
      .setTaskPriorityLevel(Optional.of(.75))
      .build();
    saveRequest(highPriorityRequest);

    final SingularityDeploy lowPriorityDeploy = initAndFinishDeploy(
      lowPriorityRequest,
      "lowPriorityDeploy"
    );
    final SingularityDeploy mediumPriorityDeploy = initAndFinishDeploy(
      mediumPriorityRequest,
      "mediumPriorityDeploy"
    );
    final SingularityDeploy highPriorityDeploy = initAndFinishDeploy(
      highPriorityRequest,
      "highPriorityDeploy"
    );

    // Task requests launched at ~ the same time should be in priority order
    long now = System.currentTimeMillis();
    List<SingularityTaskRequest> requestsByPriority = Arrays.asList(
      buildTaskRequest(lowPriorityRequest, lowPriorityDeploy, now),
      buildTaskRequest(mediumPriorityRequest, mediumPriorityDeploy, now),
      buildTaskRequest(highPriorityRequest, highPriorityDeploy, now)
    );

    List<SingularityTaskRequest> sortedRequestsByPriority = taskPrioritizer.getSortedDueTasks(
      requestsByPriority
    );

    Assertions.assertEquals(
      sortedRequestsByPriority.get(0).getRequest().getId(),
      highPriorityRequest.getId()
    );
    Assertions.assertEquals(
      sortedRequestsByPriority.get(1).getRequest().getId(),
      mediumPriorityRequest.getId()
    );
    Assertions.assertEquals(
      sortedRequestsByPriority.get(2).getRequest().getId(),
      lowPriorityRequest.getId()
    );

    // A lower priority task that is long overdue should be run before a higher priority task
    now = System.currentTimeMillis();
    List<SingularityTaskRequest> requestsByOverdueAndPriority = Arrays.asList(
      buildTaskRequest(lowPriorityRequest, lowPriorityDeploy, now - 120000), // 2 min overdue
      buildTaskRequest(mediumPriorityRequest, mediumPriorityDeploy, now - 30000), // 60s overdue
      buildTaskRequest(highPriorityRequest, highPriorityDeploy, now)
    ); // Not overdue

    List<SingularityTaskRequest> sortedRequestsByOverdueAndPriority = taskPrioritizer.getSortedDueTasks(
      requestsByOverdueAndPriority
    );

    Assertions.assertEquals(
      sortedRequestsByOverdueAndPriority.get(0).getRequest().getId(),
      lowPriorityRequest.getId()
    );
    Assertions.assertEquals(
      sortedRequestsByOverdueAndPriority.get(1).getRequest().getId(),
      mediumPriorityRequest.getId()
    );
    Assertions.assertEquals(
      sortedRequestsByOverdueAndPriority.get(2).getRequest().getId(),
      highPriorityRequest.getId()
    );
  }

  @Test
  public void badPauseExpires() {
    initRequest();

    requestManager.createCleanupRequest(
      new SingularityRequestCleanup(
        Optional.<String>empty(),
        RequestCleanupType.PAUSING,
        System.currentTimeMillis(),
        Optional.<Boolean>empty(),
        Optional.empty(),
        requestId,
        Optional.<String>empty(),
        Optional.<Boolean>empty(),
        Optional.<String>empty(),
        Optional.<String>empty(),
        Optional.<SingularityShellCommand>empty()
      )
    );

    cleaner.drainCleanupQueue();

    Assertions.assertTrue(!requestManager.getCleanupRequests().isEmpty());
    configuration.setCleanupEverySeconds(0);

    sleep(1);
    cleaner.drainCleanupQueue();

    Assertions.assertTrue(requestManager.getCleanupRequests().isEmpty());
  }

  @Test
  public void testPauseLbCleanup() {
    initLoadBalancedRequest();
    initFirstDeploy();

    requestManager.saveLbCleanupRequest(
      new SingularityRequestLbCleanup(
        requestId,
        Sets.newHashSet("test"),
        "/basepath",
        Collections.<String>emptyList(),
        Optional.<SingularityLoadBalancerUpdate>empty()
      )
    );

    requestManager.pause(
      request,
      System.currentTimeMillis(),
      Optional.<String>empty(),
      Optional.<String>empty()
    );

    testingLbClient.setNextRequestState(LoadBalancerRequestState.WAITING);

    cleaner.drainCleanupQueue();
    Assertions.assertTrue(!requestManager.getLbCleanupRequestIds().isEmpty());

    Optional<SingularityLoadBalancerUpdate> lbUpdate = requestManager
      .getLbCleanupRequest(requestId)
      .get()
      .getLoadBalancerUpdate();

    Assertions.assertTrue(lbUpdate.isPresent());
    Assertions.assertTrue(
      lbUpdate.get().getLoadBalancerState() == LoadBalancerRequestState.WAITING
    );

    testingLbClient.setNextRequestState(LoadBalancerRequestState.FAILED);

    cleaner.drainCleanupQueue();
    Assertions.assertTrue(!requestManager.getLbCleanupRequestIds().isEmpty());

    lbUpdate =
      requestManager.getLbCleanupRequest(requestId).get().getLoadBalancerUpdate();

    Assertions.assertTrue(lbUpdate.isPresent());
    Assertions.assertTrue(
      lbUpdate.get().getLoadBalancerState() == LoadBalancerRequestState.FAILED
    );

    testingLbClient.setNextRequestState(LoadBalancerRequestState.SUCCESS);

    cleaner.drainCleanupQueue();
    Assertions.assertTrue(requestManager.getLbCleanupRequestIds().isEmpty());
  }

  @Test
  public void testPause() {
    initRequest();
    initFirstDeploy();

    SingularityTask taskOne = startTask(firstDeploy);

    requestResource.pause(requestId, Optional.empty(), singularityUser);

    cleaner.drainCleanupQueue();

    Assertions.assertEquals(1, taskManager.getKilledTaskIdRecords().size());

    statusUpdate(taskOne, TaskState.TASK_KILLED);

    resourceOffers();

    Assertions.assertEquals(0, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals(0, taskManager.getPendingTasks().size());
    Assertions.assertEquals(
      RequestState.PAUSED,
      requestManager.getRequest(requestId).get().getState()
    );
    Assertions.assertEquals(
      requestId,
      requestManager.getPausedRequests(false).iterator().next().getRequest().getId()
    );

    requestResource.unpause(requestId, Optional.empty(), singularityUser);
    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assertions.assertEquals(0, taskManager.getPendingTasks().size());

    Assertions.assertEquals(
      RequestState.ACTIVE,
      requestManager.getRequest(requestId).get().getState()
    );
    Assertions.assertEquals(
      requestId,
      requestManager.getActiveRequests(false).iterator().next().getRequest().getId()
    );
  }

  @Test
  public void testBounce() {
    initRequest();

    requestResource.scale(
      requestId,
      new SingularityScaleRequest(
        Optional.of(3),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    initFirstDeploy();

    SingularityTask taskOne = startTask(firstDeploy, 1);
    SingularityTask taskTwo = startTask(firstDeploy, 2);
    SingularityTask taskThree = startTask(firstDeploy, 3);

    requestResource.bounce(requestId, Optional.empty(), singularityUser);

    Assertions.assertTrue(requestManager.cleanupRequestExists(requestId));

    cleaner.drainCleanupQueue();

    Assertions.assertTrue(!requestManager.cleanupRequestExists(requestId));
    Assertions.assertTrue(taskManager.getCleanupTaskIds().size() == 3);

    cleaner.drainCleanupQueue();

    Assertions.assertTrue(!requestManager.cleanupRequestExists(requestId));
    Assertions.assertTrue(taskManager.getCleanupTaskIds().size() == 3);

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 6);

    cleaner.drainCleanupQueue();

    Assertions.assertTrue(taskManager.getCleanupTaskIds().size() == 3);

    for (SingularityTask task : taskManager.getActiveTasks()) {
      if (
        !task.getTaskId().equals(taskOne.getTaskId()) &&
        !task.getTaskId().equals(taskTwo.getTaskId()) &&
        !task.getTaskId().equals(taskThree.getTaskId())
      ) {
        statusUpdate(task, TaskState.TASK_RUNNING, Optional.of(1L));
      }
    }

    cleaner.drainCleanupQueue();

    Assertions.assertTrue(taskManager.getCleanupTaskIds().isEmpty());
    Assertions.assertTrue(taskManager.getKilledTaskIdRecords().size() == 3);
  }

  @Test
  public void testIncrementalBounceShutsDownOldTasksPerNewHealthyTask() {
    initRequest();

    requestResource.scale(
      requestId,
      new SingularityScaleRequest(
        Optional.of(3),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    initFirstDeploy();

    startTask(firstDeploy, 1);
    startTask(firstDeploy, 2);
    startTask(firstDeploy, 3);

    requestResource.bounce(
      requestId,
      Optional.of(
        new SingularityBounceRequest(
          Optional.of(true),
          Optional.empty(),
          Optional.of(1L),
          Optional.empty(),
          Optional.of("msg"),
          Optional.empty()
        )
      ),
      singularityUser
    );

    Assertions.assertTrue(requestManager.cleanupRequestExists(requestId));

    cleaner.drainCleanupQueue();

    Assertions.assertTrue(!requestManager.cleanupRequestExists(requestId));
    Assertions.assertEquals(3, taskManager.getCleanupTaskIds().size());

    SingularityTask newTask = launchTask(
      request,
      firstDeploy,
      5,
      TaskState.TASK_STARTING
    );

    cleaner.drainCleanupQueue();

    Assertions.assertEquals(0, taskManager.getKilledTaskIdRecords().size());
    Assertions.assertEquals(4, taskManager.getActiveTaskIds().size());

    statusUpdate(newTask, TaskState.TASK_RUNNING);

    cleaner.drainCleanupQueue();

    Assertions.assertEquals(1, taskManager.getKilledTaskIdRecords().size());
    Assertions.assertEquals(4, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testBounceOnPendingInstancesReleasesLock() {
    initRequest();
    initFirstDeploy();

    SingularityTask task = startTask(firstDeploy, 1);
    statusUpdate(task, TaskState.TASK_FAILED);
    killKilledTasks();

    Assertions.assertEquals(
      0,
      taskManager.getActiveTaskIds().size(),
      "Bounce starts when tasks have not yet been launched"
    );

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

    // It acquires a lock on the bounce
    Assertions.assertTrue(
      requestManager.getExpiringBounce(requestId).isPresent(),
      "Lock on bounce should be acquired during bounce"
    );

    cleaner.drainCleanupQueue();

    scheduler.drainPendingQueue();
    resourceOffers();

    for (SingularityTaskId singularityTaskId : taskManager.getActiveTaskIds()) {
      taskManager.saveTaskHistoryUpdate(
        new SingularityTaskHistoryUpdate(
          singularityTaskId,
          System.currentTimeMillis(),
          ExtendedTaskState.TASK_RUNNING,
          Optional.empty(),
          Optional.empty(),
          Collections.emptySet()
        )
      );
    }

    cleaner.drainCleanupQueue();
    killKilledTasks();

    // It finishes with one task running and the bounce released
    Assertions.assertEquals(
      1,
      taskManager.getActiveTaskIds().size(),
      "Should end bounce with target number of tasks"
    );
    for (SingularityTaskId singularityTaskId : taskManager.getActiveTaskIds()) {
      String statusMessage = taskManager
        .getTaskHistoryUpdates(singularityTaskId)
        .get(0)
        .getStatusMessage()
        .get();
      Assertions.assertTrue(
        statusMessage.contains("BOUNCE"),
        "Task was started by bounce"
      );
    }
    Assertions.assertFalse(
      requestManager.getExpiringBounce(requestId).isPresent(),
      "Lock on bounce should be released after bounce"
    );
  }

  @Test
  public void testBounceOnRunningInstancesReleasesLock() {
    initRequest();
    initFirstDeploy();

    startTask(firstDeploy, 1);
    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());

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
    Assertions.assertTrue(requestManager.isBouncing(requestId));
    cleaner.drainCleanupQueue();

    // It acquires a lock on the bounce
    Assertions.assertTrue(
      requestManager.getExpiringBounce(requestId).isPresent(),
      "Lock on bounce should be acquired during bounce"
    );

    scheduler.drainPendingQueue();
    resourceOffers();

    for (SingularityTaskId singularityTaskId : taskManager.getActiveTaskIds()) {
      taskManager.saveTaskHistoryUpdate(
        new SingularityTaskHistoryUpdate(
          singularityTaskId,
          System.currentTimeMillis(),
          ExtendedTaskState.TASK_RUNNING,
          Optional.empty(),
          Optional.empty(),
          Collections.emptySet()
        )
      );
    }

    Assertions.assertTrue(
      taskManager.getActiveTaskIds().size() >= 2,
      "Need to start at least 1 instance to begin killing old instances"
    );
    Assertions.assertTrue(requestManager.isBouncing(requestId));
    cleaner.drainCleanupQueue();
    killKilledTasks();
    Assertions.assertFalse(requestManager.isBouncing(requestId));

    // It finishes with one task running and the bounce released
    Assertions.assertEquals(
      1,
      taskManager.getActiveTaskIds().size(),
      "Should end bounce with target number of tasks"
    );
    for (SingularityTaskId singularityTaskId : taskManager.getActiveTaskIds()) {
      String statusMessage = taskManager
        .getTaskHistoryUpdates(singularityTaskId)
        .get(0)
        .getStatusMessage()
        .get();
      Assertions.assertTrue(
        statusMessage.contains("BOUNCE"),
        "Task was started by bounce"
      );
    }
    Assertions.assertFalse(
      requestManager.getExpiringBounce(requestId).isPresent(),
      "Lock on bounce should be released after bounce"
    );
  }

  @Test
  public void testBounceReleasesLockWithAlternateCleanupType() {
    initRequest();
    initFirstDeploy();

    startTask(firstDeploy, 1);
    List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
    Assertions.assertEquals(1, activeTaskIds.size());
    SingularityTaskId firstTaskId = activeTaskIds.get(0);

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
    Assertions.assertTrue(requestManager.isBouncing(requestId));
    cleaner.drainCleanupQueue();

    scheduler.drainPendingQueue();
    resourceOffers();

    // Save a new cleanup type over the old one, and make sure the bounce lock still releases
    taskManager.saveTaskCleanup(
      new SingularityTaskCleanup(
        Optional.empty(),
        TaskCleanupType.USER_REQUESTED,
        System.currentTimeMillis(),
        firstTaskId,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      )
    );

    for (SingularityTaskId singularityTaskId : taskManager.getActiveTaskIds()) {
      taskManager.saveTaskHistoryUpdate(
        new SingularityTaskHistoryUpdate(
          singularityTaskId,
          System.currentTimeMillis(),
          ExtendedTaskState.TASK_RUNNING,
          Optional.empty(),
          Optional.empty(),
          Collections.emptySet()
        )
      );
    }
    Assertions.assertTrue(requestManager.isBouncing(requestId));
    cleaner.drainCleanupQueue();
    killKilledTasks();
    Assertions.assertFalse(requestManager.isBouncing(requestId));
  }

  @Test
  public void testIncrementalBounce() {
    initRequest();
    resourceOffers(3); // set up agents so scale validate will pass

    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();

    requestResource.postRequest(
      request
        .toBuilder()
        .setAgentPlacement(Optional.of(AgentPlacement.SEPARATE_BY_REQUEST))
        .setInstances(Optional.of(2))
        .build(),
      singularityUser
    );

    initHCDeploy();

    SingularityTask taskOne = startSeparatePlacementTask(firstDeploy, 1);
    SingularityTask taskTwo = startSeparatePlacementTask(firstDeploy, 2);

    requestManager.createCleanupRequest(
      new SingularityRequestCleanup(
        user,
        RequestCleanupType.INCREMENTAL_BOUNCE,
        System.currentTimeMillis(),
        Optional.<Boolean>empty(),
        Optional.empty(),
        requestId,
        Optional.of(firstDeployId),
        Optional.<Boolean>empty(),
        Optional.<String>empty(),
        Optional.<String>empty(),
        Optional.<SingularityShellCommand>empty()
      )
    );

    Assertions.assertTrue(requestManager.cleanupRequestExists(requestId));

    cleaner.drainCleanupQueue();

    Assertions.assertTrue(!requestManager.cleanupRequestExists(requestId));
    Assertions.assertEquals(2, taskManager.getCleanupTaskIds().size());

    scheduler.drainPendingQueue();
    resourceOffers(3);

    SingularityTask taskThree = null;

    for (SingularityTask task : taskManager.getActiveTasks()) {
      if (
        !task.getTaskId().equals(taskOne.getTaskId()) &&
        !task.getTaskId().equals(taskTwo.getTaskId())
      ) {
        taskThree = task;
      }
    }

    statusUpdate(taskThree, TaskState.TASK_RUNNING, Optional.of(1L));
    Assertions.assertEquals(3, taskManager.getActiveTaskIds().size());

    cleaner.drainCleanupQueue();

    // No old tasks should be killed before new ones pass healthchecks
    Assertions.assertEquals(2, taskManager.getCleanupTaskIds().size());
    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(200),
        Optional.of(1000L),
        System.currentTimeMillis(),
        Optional.<String>empty(),
        Optional.<String>empty(),
        taskThree.getTaskId(),
        Optional.<Boolean>empty()
      )
    );

    cleaner.drainCleanupQueue();
    Assertions.assertEquals(1, taskManager.getCleanupTaskIds().size());

    statusUpdate(taskOne, TaskState.TASK_KILLED);

    resourceOffers(3);

    SingularityTask taskFour = null;

    for (SingularityTask task : taskManager.getActiveTasks()) {
      if (
        !task.getTaskId().equals(taskOne.getTaskId()) &&
        !task.getTaskId().equals(taskTwo.getTaskId()) &&
        !task.getTaskId().equals(taskThree.getTaskId())
      ) {
        taskFour = task;
      }
    }

    statusUpdate(taskFour, TaskState.TASK_RUNNING, Optional.of(1L));
    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(200),
        Optional.of(1000L),
        System.currentTimeMillis(),
        Optional.<String>empty(),
        Optional.<String>empty(),
        taskFour.getTaskId(),
        Optional.<Boolean>empty()
      )
    );

    cleaner.drainCleanupQueue();

    Assertions.assertTrue(taskManager.getCleanupTaskIds().isEmpty());
  }

  @Test
  public void testScheduledNotification() {
    schedule = "0 0 * * * ?"; // run every hour
    initScheduledRequest();
    initFirstDeploy();

    configuration.setWarnIfScheduledJobIsRunningForAtLeastMillis(Long.MAX_VALUE);
    configuration.setWarnIfScheduledJobIsRunningPastNextRunPct(200);

    final long now = System.currentTimeMillis();

    SingularityTask firstTask = launchTask(
      request,
      firstDeploy,
      now - TimeUnit.HOURS.toMillis(3),
      1,
      TaskState.TASK_RUNNING
    );

    scheduledJobPoller.runActionOnPoll();

    Mockito
      .verify(mailer, Mockito.times(0))
      .sendTaskOverdueMail(
        ArgumentMatchers.<Optional<SingularityTask>>any(),
        ArgumentMatchers.<SingularityTaskId>any(),
        ArgumentMatchers.<SingularityRequest>any(),
        ArgumentMatchers.anyLong(),
        ArgumentMatchers.anyLong()
      );

    configuration.setWarnIfScheduledJobIsRunningForAtLeastMillis(
      TimeUnit.HOURS.toMillis(1)
    );

    scheduledJobPoller.runActionOnPoll();

    Mockito
      .verify(mailer, Mockito.times(1))
      .sendTaskOverdueMail(
        ArgumentMatchers.<Optional<SingularityTask>>any(),
        ArgumentMatchers.<SingularityTaskId>any(),
        ArgumentMatchers.<SingularityRequest>any(),
        ArgumentMatchers.anyLong(),
        ArgumentMatchers.anyLong()
      );

    scheduledJobPoller.runActionOnPoll();

    Mockito
      .verify(mailer, Mockito.times(1))
      .sendTaskOverdueMail(
        ArgumentMatchers.<Optional<SingularityTask>>any(),
        ArgumentMatchers.<SingularityTaskId>any(),
        ArgumentMatchers.<SingularityRequest>any(),
        ArgumentMatchers.anyLong(),
        ArgumentMatchers.anyLong()
      );

    statusUpdate(firstTask, TaskState.TASK_FINISHED);

    Optional<SingularityDeployStatistics> deployStatistics = deployManager.getDeployStatistics(
      requestId,
      firstDeployId
    );

    long oldAvg = deployStatistics.get().getAverageRuntimeMillis().get();

    Assertions.assertTrue(deployStatistics.get().getNumTasks() == 1);
    Assertions.assertTrue(
      deployStatistics.get().getAverageRuntimeMillis().get() > 1 &&
      deployStatistics.get().getAverageRuntimeMillis().get() < TimeUnit.DAYS.toMillis(1)
    );

    configuration.setWarnIfScheduledJobIsRunningForAtLeastMillis(1);

    SingularityTask secondTask = launchTask(
      request,
      firstDeploy,
      now - 500,
      1,
      TaskState.TASK_RUNNING
    );

    scheduledJobPoller.runActionOnPoll();

    Mockito
      .verify(mailer, Mockito.times(1))
      .sendTaskOverdueMail(
        ArgumentMatchers.<Optional<SingularityTask>>any(),
        ArgumentMatchers.<SingularityTaskId>any(),
        ArgumentMatchers.<SingularityRequest>any(),
        ArgumentMatchers.anyLong(),
        ArgumentMatchers.anyLong()
      );

    statusUpdate(secondTask, TaskState.TASK_FINISHED);

    deployStatistics = deployManager.getDeployStatistics(requestId, firstDeployId);

    Assertions.assertTrue(deployStatistics.get().getNumTasks() == 2);
    Assertions.assertTrue(
      deployStatistics.get().getAverageRuntimeMillis().get() > 1 &&
      deployStatistics.get().getAverageRuntimeMillis().get() < oldAvg
    );

    saveRequest(
      request.toBuilder().setScheduledExpectedRuntimeMillis(Optional.of(1L)).build()
    );

    SingularityTask thirdTask = launchTask(
      request,
      firstDeploy,
      now - 502,
      1,
      TaskState.TASK_RUNNING
    );

    scheduledJobPoller.runActionOnPoll();

    Mockito
      .verify(mailer, Mockito.times(2))
      .sendTaskOverdueMail(
        ArgumentMatchers.<Optional<SingularityTask>>any(),
        ArgumentMatchers.<SingularityTaskId>any(),
        ArgumentMatchers.<SingularityRequest>any(),
        ArgumentMatchers.anyLong(),
        ArgumentMatchers.anyLong()
      );

    taskManager.deleteTaskHistory(thirdTask.getTaskId());

    scheduledJobPoller.runActionOnPoll();

    Mockito
      .verify(mailer, Mockito.times(3))
      .sendTaskOverdueMail(
        ArgumentMatchers.<Optional<SingularityTask>>any(),
        ArgumentMatchers.<SingularityTaskId>any(),
        ArgumentMatchers.<SingularityRequest>any(),
        ArgumentMatchers.anyLong(),
        ArgumentMatchers.anyLong()
      );
  }

  @Test
  public void testTaskOddities() {
    // test unparseable status update
    TaskStatus.Builder bldr = TaskStatus
      .newBuilder()
      .setTaskId(TaskID.newBuilder().setValue("task"))
      .setAgentId(AgentID.newBuilder().setValue("agent1"))
      .setState(TaskState.TASK_RUNNING);

    // should not throw exception:
    sms.statusUpdate(bldr.build()).join();

    initRequest();
    initFirstDeploy();

    SingularityTask taskOne = launchTask(
      request,
      firstDeploy,
      1,
      TaskState.TASK_STARTING
    );

    taskManager.deleteTaskHistory(taskOne.getTaskId());

    Assertions.assertTrue(taskManager.isActiveTask(taskOne.getTaskId()));

    statusUpdate(taskOne, TaskState.TASK_RUNNING);
    statusUpdate(taskOne, TaskState.TASK_FAILED);

    Assertions.assertTrue(!taskManager.isActiveTask(taskOne.getTaskId()));

    Assertions.assertEquals(
      2,
      taskManager.getTaskHistoryUpdates(taskOne.getTaskId()).size()
    );
  }

  @Test
  public void testOnDemandTasksPersist() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(
      requestId,
      RequestType.ON_DEMAND
    );
    requestResource.postRequest(bldr.build(), singularityUser);
    deploy("d2");
    deployChecker.checkDeploys();

    requestResource.scheduleImmediately(
      singularityUser,
      requestId,
      ((SingularityRunNowRequest) null)
    );

    scheduler.drainPendingQueue();
    resourceOffers();

    requestResource.scheduleImmediately(
      singularityUser,
      requestId,
      ((SingularityRunNowRequest) null)
    );

    scheduler.drainPendingQueue();
    resourceOffers();

    Assertions.assertEquals(2, taskManager.getActiveTaskIds().size());

    requestResource.scheduleImmediately(
      singularityUser,
      requestId,
      ((SingularityRunNowRequest) null)
    );

    scheduler.drainPendingQueue();

    requestResource.scheduleImmediately(
      singularityUser,
      requestId,
      ((SingularityRunNowRequest) null)
    );

    scheduler.drainPendingQueue();

    Assertions.assertEquals(2, taskManager.getPendingTaskIds().size());

    resourceOffers();

    Assertions.assertEquals(4, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testRunNowScheduledJobDoesNotRetry() {
    initScheduledRequest();
    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();
    SingularityRequest newRequest = request
      .toBuilder()
      .setNumRetriesOnFailure(Optional.of(2))
      .build();
    requestResource.postRequest(newRequest, singularityUser);
    initFirstDeploy();

    requestResource.scheduleImmediately(
      singularityUser,
      requestId,
      new SingularityRunNowRequestBuilder().build()
    );
    scheduler.drainPendingQueue();
    resourceOffers();

    SingularityTask task = taskManager.getActiveTasks().get(0);
    statusUpdate(task, TaskState.TASK_FAILED);
    scheduler.drainPendingQueue();

    SingularityDeployStatistics deployStatistics = deployManager
      .getDeployStatistics(
        task.getTaskId().getRequestId(),
        task.getTaskId().getDeployId()
      )
      .get();

    Assertions.assertEquals(
      MesosTaskState.TASK_FAILED,
      deployStatistics.getLastTaskState().get().toTaskState().get()
    );
    Assertions.assertEquals(
      PendingType.TASK_DONE,
      taskManager.getPendingTaskIds().get(0).getPendingType()
    );
    Assertions.assertEquals(1, deployStatistics.getNumFailures());
    Assertions.assertEquals(0, deployStatistics.getNumSequentialRetries());
    Assertions.assertEquals(
      Optional.<Long>empty(),
      deployStatistics.getAverageRuntimeMillis()
    );
  }

  @Test
  public void testRunNowOnDemandJobDoesNotRetryAfterUserInitiatedPause() {
    initRequestWithType(RequestType.ON_DEMAND, false);
    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();
    SingularityRequest newRequest = request
      .toBuilder()
      .setNumRetriesOnFailure(Optional.of(2))
      .build();
    requestResource.postRequest(newRequest, singularityUser);
    initFirstDeploy();

    requestResource.scheduleImmediately(
      singularityUser,
      requestId,
      new SingularityRunNowRequestBuilder().setMessage("foo bar").build()
    );
    scheduler.drainPendingQueue();
    resourceOffers();

    SingularityTask task = taskManager.getActiveTasks().get(0);
    taskManager.saveTaskHistoryUpdate(
      new SingularityTaskHistoryUpdate(
        task.getTaskId(),
        System.currentTimeMillis(),
        ExtendedTaskState.TASK_CLEANING,
        Optional.of("PAUSE"),
        Optional.empty(),
        Collections.emptySet()
      )
    );

    statusUpdate(task, TaskState.TASK_KILLED);
    scheduler.drainPendingQueue();

    SingularityDeployStatistics deployStatistics = deployManager
      .getDeployStatistics(
        task.getTaskId().getRequestId(),
        task.getTaskId().getDeployId()
      )
      .get();

    Assertions.assertEquals(
      MesosTaskState.TASK_KILLED,
      deployStatistics.getLastTaskState().get().toTaskState().get()
    );
    Assertions.assertEquals(0, taskManager.getPendingTaskIds().size());
    Assertions.assertEquals(0, deployStatistics.getNumFailures());
    Assertions.assertEquals(0, deployStatistics.getNumSequentialRetries());
  }

  @Test
  public void testRunNowOnDemandJobDoesNotRetryAfterUserInitiatedKill() {
    initRequestWithType(RequestType.ON_DEMAND, false);
    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();
    SingularityRequest newRequest = request
      .toBuilder()
      .setNumRetriesOnFailure(Optional.of(2))
      .build();
    requestResource.postRequest(newRequest, singularityUser);
    initFirstDeploy();

    requestResource.scheduleImmediately(
      singularityUser,
      requestId,
      new SingularityRunNowRequestBuilder().setMessage("foo bar").build()
    );
    scheduler.drainPendingQueue();
    resourceOffers();

    SingularityTask task = taskManager.getActiveTasks().get(0);
    taskManager.saveTaskHistoryUpdate(
      new SingularityTaskHistoryUpdate(
        task.getTaskId(),
        System.currentTimeMillis(),
        ExtendedTaskState.TASK_CLEANING,
        Optional.of("USER_REQUESTED"),
        Optional.empty(),
        Collections.emptySet()
      )
    );

    statusUpdate(task, TaskState.TASK_KILLED);
    scheduler.drainPendingQueue();

    SingularityDeployStatistics deployStatistics = deployManager
      .getDeployStatistics(
        task.getTaskId().getRequestId(),
        task.getTaskId().getDeployId()
      )
      .get();

    Assertions.assertEquals(
      MesosTaskState.TASK_KILLED,
      deployStatistics.getLastTaskState().get().toTaskState().get()
    );
    Assertions.assertEquals(0, taskManager.getPendingTaskIds().size());
    Assertions.assertEquals(0, deployStatistics.getNumFailures());
    Assertions.assertEquals(0, deployStatistics.getNumSequentialRetries());
  }

  @Test
  public void testRunNowOnDemandJobMayRetryOnFailure() {
    initRequestWithType(RequestType.ON_DEMAND, false);
    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();
    SingularityRequest newRequest = request
      .toBuilder()
      .setNumRetriesOnFailure(Optional.of(2))
      .build();
    requestResource.postRequest(newRequest, singularityUser);
    initFirstDeploy();

    requestResource.scheduleImmediately(
      singularityUser,
      requestId,
      new SingularityRunNowRequestBuilder().setMessage("foo bar").build()
    );
    scheduler.drainPendingQueue();
    resourceOffers();

    SingularityTask task = taskManager.getActiveTasks().get(0);
    statusUpdate(task, TaskState.TASK_FAILED);
    scheduler.drainPendingQueue();

    SingularityDeployStatistics deployStatistics = deployManager
      .getDeployStatistics(
        task.getTaskId().getRequestId(),
        task.getTaskId().getDeployId()
      )
      .get();

    Assertions.assertEquals(
      MesosTaskState.TASK_FAILED,
      deployStatistics.getLastTaskState().get().toTaskState().get()
    );
    Assertions.assertEquals(
      PendingType.RETRY,
      taskManager.getPendingTaskIds().get(0).getPendingType()
    );
    Assertions.assertEquals(
      "foo bar",
      taskManager.getPendingTasks().get(0).getMessage().get()
    );
    Assertions.assertEquals(1, deployStatistics.getNumFailures());
    Assertions.assertEquals(1, deployStatistics.getNumSequentialRetries());
  }

  @Test
  public void testRunNowOnDemandJobsDoNotRetryAfterUserRequestedKill() {
    initRequestWithType(RequestType.ON_DEMAND, false);
    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();
    SingularityRequest newRequest = request
      .toBuilder()
      .setNumRetriesOnFailure(Optional.of(2))
      .build();
    requestResource.postRequest(newRequest, singularityUser);
    initFirstDeploy();

    requestResource.scheduleImmediately(
      singularityUser,
      requestId,
      new SingularityRunNowRequestBuilder().setMessage("foo bar").build()
    );
    scheduler.drainPendingQueue();
    resourceOffers();

    SingularityTask task = taskManager.getActiveTasks().get(0);
    taskManager.saveTaskCleanup(
      new SingularityTaskCleanup(
        Optional.of(singularityUser.getId()),
        TaskCleanupType.USER_REQUESTED,
        System.currentTimeMillis(),
        task.getTaskId(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      )
    );
    cleaner.drainCleanupQueue();
    statusUpdate(task, TaskState.TASK_KILLED);
    scheduler.drainPendingQueue();

    SingularityDeployStatistics deployStatistics = deployManager
      .getDeployStatistics(
        task.getTaskId().getRequestId(),
        task.getTaskId().getDeployId()
      )
      .get();

    Assertions.assertEquals(0, taskManager.getPendingTaskIds().size());
    Assertions.assertEquals(
      MesosTaskState.TASK_KILLED,
      deployStatistics.getLastTaskState().get().toTaskState().get()
    );
    Assertions.assertEquals(0, deployStatistics.getNumFailures());
    Assertions.assertEquals(0, deployStatistics.getNumSequentialRetries());
  }

  @Test
  public void testOnDemandRunNowJobRespectsSpecifiedRunAtTime() {
    initOnDemandRequest();
    initFirstDeploy();

    long requestedLaunchTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10);

    requestResource.scheduleImmediately(
      singularityUser,
      requestId,
      new SingularityRunNowRequestBuilder().setRunAt(requestedLaunchTime).build()
    );

    scheduler.drainPendingQueue();

    SingularityPendingTaskId task = taskManager.getPendingTaskIds().get(0);
    long runAt = task.getNextRunAt();

    Assertions.assertEquals(requestedLaunchTime, runAt);
  }

  @Test
  public void testScheduledRunNowJobRespectsSpecifiedRunAtTime() {
    initScheduledRequest();
    initFirstDeploy();

    long requestedLaunchTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10);

    requestResource.scheduleImmediately(
      singularityUser,
      requestId,
      new SingularityRunNowRequestBuilder().setRunAt(requestedLaunchTime).build()
    );

    scheduler.drainPendingQueue();

    SingularityPendingTaskId task = taskManager.getPendingTaskIds().get(0);
    long runAt = task.getNextRunAt();

    Assertions.assertEquals(requestedLaunchTime, runAt);
  }

  @Test
  public void testJobRescheduledWhenItFinishesDuringDecommission() {
    initScheduledRequest();
    initFirstDeploy();

    resourceOffers();

    SingularityTask task = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    agentManager.changeState(
      "agent1",
      MachineState.STARTING_DECOMMISSION,
      Optional.<String>empty(),
      Optional.of("user1")
    );

    cleaner.drainCleanupQueue();
    resourceOffers();
    cleaner.drainCleanupQueue();

    statusUpdate(task, TaskState.TASK_FINISHED);
    scheduler.drainPendingQueue();

    Assertions.assertTrue(!taskManager.getPendingTaskIds().isEmpty());
  }

  @Test
  public void testScaleDownTakesHighestInstances() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(5)));

    resourceOffers();

    Assertions.assertEquals(5, taskManager.getActiveTaskIds().size());

    requestResource.scale(
      requestId,
      new SingularityScaleRequest(
        Optional.of(2),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    resourceOffers();
    cleaner.drainCleanupQueue();

    Assertions.assertEquals(3, taskManager.getKilledTaskIdRecords().size());

    for (SingularityKilledTaskIdRecord taskId : taskManager.getKilledTaskIdRecords()) {
      Assertions.assertTrue(taskId.getTaskId().getInstanceNo() > 2);

      scheduler.drainPendingQueue();
    }
  }

  @Test
  public void testScaleDownTakesHighestInstancesWithPendingTask() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(5)));

    resourceOffers();

    Assertions.assertEquals(5, taskManager.getActiveTaskIds().size());

    SingularityTaskId instance2 = null;
    for (SingularityTaskId taskId : taskManager.getActiveTaskIds()) {
      if (taskId.getInstanceNo() == 2) {
        instance2 = taskId;
      }
    }

    statusUpdate(taskManager.getTask(instance2).get(), TaskState.TASK_KILLED);
    killKilledTasks();

    scheduler.drainPendingQueue();

    requestResource.scale(
      requestId,
      new SingularityScaleRequest(
        Optional.of(3),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );
    scheduler.drainPendingQueue();
    cleaner.drainCleanupQueue();

    // instances 4 and 5 should get killed
    Assertions.assertEquals(2, taskManager.getKilledTaskIdRecords().size());
    killKilledTasks();

    resourceOffers();

    // instances 1,2,3 should be active
    Assertions.assertEquals(3, taskManager.getActiveTaskIds().size());
    for (SingularityTaskId taskId : taskManager.getActiveTaskIds()) {
      Assertions.assertTrue(taskId.getInstanceNo() < 4);
    }
  }

  @Test
  public void testRequestsInPendingQueueAreOrderedByTimestamp() {
    long now = System.currentTimeMillis();
    initRequestWithType(RequestType.SCHEDULED, false);
    startFirstDeploy();
    SingularityPendingRequest pendingDeployRequest = new SingularityPendingRequest(
      requestId,
      firstDeploy.getId(),
      now,
      Optional.empty(),
      PendingType.NEW_DEPLOY,
      firstDeploy.getSkipHealthchecksOnDeploy(),
      Optional.empty()
    );
    SingularityPendingRequest pendingRunNowRequest = new SingularityPendingRequest(
      requestId,
      firstDeploy.getId(),
      now + 200,
      Optional.empty(),
      PendingType.IMMEDIATE,
      firstDeploy.getSkipHealthchecksOnDeploy(),
      Optional.empty()
    );
    requestManager.addToPendingQueue(pendingDeployRequest);

    requestManager.addToPendingQueue(pendingRunNowRequest);

    Assertions.assertEquals(2, requestManager.getPendingRequests().size());
    // Was added first
    Assertions.assertEquals(
      PendingType.NEW_DEPLOY,
      requestManager.getPendingRequests().get(0).getPendingType()
    );
    // Was added second
    Assertions.assertEquals(
      PendingType.IMMEDIATE,
      requestManager.getPendingRequests().get(1).getPendingType()
    );

    resourceOffers();
  }

  @Test
  public void testImmediateRequestsAreConsistentlyDeleted() {
    long now = System.currentTimeMillis();
    initRequestWithType(RequestType.SCHEDULED, false);
    startFirstDeploy();
    SingularityPendingRequest pendingDeployRequest = new SingularityPendingRequest(
      requestId,
      firstDeploy.getId(),
      now,
      Optional.empty(),
      PendingType.NEW_DEPLOY,
      firstDeploy.getSkipHealthchecksOnDeploy(),
      Optional.empty()
    );
    SingularityPendingRequest pendingRunNowRequest = new SingularityPendingRequest(
      requestId,
      firstDeploy.getId(),
      now + 200,
      Optional.empty(),
      PendingType.IMMEDIATE,
      firstDeploy.getSkipHealthchecksOnDeploy(),
      Optional.empty()
    );

    requestManager.addToPendingQueue(pendingDeployRequest);
    requestManager.addToPendingQueue(pendingRunNowRequest);

    // Pending queue has two requests: NEW_DEPLOY & IMMEDIATE
    Assertions.assertEquals(2, requestManager.getPendingRequests().size());

    requestManager.deletePendingRequest(pendingDeployRequest);

    // Just the immediate run
    Assertions.assertEquals(1, requestManager.getPendingRequests().size());

    requestManager.deletePendingRequest(pendingRunNowRequest);

    // Immediate run was successfully deleted
    Assertions.assertEquals(0, requestManager.getPendingRequests().size());
  }

  @Test
  public void testWaitAfterTaskWorks() {
    initRequest();
    initFirstDeploy();

    SingularityTask task = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    statusUpdate(task, TaskState.TASK_FAILED);
    scheduler.drainPendingQueue();

    Assertions.assertTrue(
      taskManager.getPendingTaskIds().get(0).getNextRunAt() -
      System.currentTimeMillis() <
      1000L
    );

    resourceOffers();

    long extraWait = 100000L;

    saveAndSchedule(
      request
        .toBuilder()
        .setWaitAtLeastMillisAfterTaskFinishesForReschedule(Optional.of(extraWait))
        .setInstances(Optional.of(2))
    );
    resourceOffers();

    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_FAILED);
    scheduler.drainPendingQueue();

    Assertions.assertTrue(
      taskManager.getPendingTaskIds().get(0).getNextRunAt() -
      System.currentTimeMillis() >
      1000L
    );
    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testRemovedRequestData() {
    long now = System.currentTimeMillis();

    initRequest();
    SingularityDeployBuilder db = new SingularityDeployBuilder(requestId, firstDeployId);
    db.setMaxTaskRetries(Optional.of(1));
    initDeploy(db, now);

    deployChecker.checkDeploys();
    Assertions.assertEquals(
      DeployState.WAITING,
      deployManager.getPendingDeploys().get(0).getCurrentDeployState()
    );

    requestManager.startDeletingRequest(
      request,
      Optional.empty(),
      Optional.<String>empty(),
      Optional.<String>empty(),
      Optional.<String>empty()
    );
    requestManager.markDeleted(
      request,
      now,
      Optional.<String>empty(),
      Optional.<String>empty()
    );
    deployChecker.checkDeploys();
    SingularityDeployResult deployResult = deployManager
      .getDeployResult(requestId, firstDeployId)
      .get();
    Assertions.assertEquals(DeployState.FAILED, deployResult.getDeployState());
    Assertions.assertTrue(deployResult.getMessage().get().contains("MISSING"));
  }

  @Test
  public void itCorrectlyUpdatesRequestDeletingStateHistory() {
    initRequest();
    Assertions.assertEquals(
      RequestState.ACTIVE,
      requestManager.getRequest(requestId).get().getState()
    );
    Assertions.assertEquals(1, requestManager.getRequestHistory(requestId).size());

    requestManager.startDeletingRequest(
      request,
      Optional.empty(),
      Optional.<String>empty(),
      Optional.<String>empty(),
      Optional.of("the cake is a lie")
    );
    Assertions.assertEquals(
      RequestState.DELETING,
      requestManager.getRequest(requestId).get().getState()
    );
    Assertions.assertEquals(2, requestManager.getRequestHistory(requestId).size());

    cleaner.drainCleanupQueue();
    Assertions.assertEquals(3, requestManager.getRequestHistory(requestId).size());

    List<RequestHistoryType> historyTypes = new ArrayList<>();
    for (SingularityRequestHistory request : requestManager.getRequestHistory(
      requestId
    )) {
      historyTypes.add(request.getEventType());
    }
    Assertions.assertTrue(historyTypes.contains(RequestHistoryType.CREATED));
    Assertions.assertTrue(historyTypes.contains(RequestHistoryType.DELETING));
    Assertions.assertTrue(historyTypes.contains(RequestHistoryType.DELETED));
  }

  @Test
  public void itSetsRequestStateToDeletedAfterAllTasksAreCleanedUp() {
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

    Assertions.assertEquals(
      requestId,
      requestManager.getActiveRequests().iterator().next().getRequest().getId()
    );
    Assertions.assertEquals(2, taskManager.getActiveTaskIds().size());

    requestManager.startDeletingRequest(
      request,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()
    );

    Assertions.assertEquals(
      requestId,
      requestManager.getCleanupRequests().get(0).getRequestId()
    );
    Assertions.assertEquals(
      RequestState.DELETING,
      requestManager.getRequest(requestId).get().getState()
    );

    cleaner.drainCleanupQueue();
    Assertions.assertEquals(0, taskManager.getCleanupTaskIds().size());
    killKilledTasks();
    cleaner.drainCleanupQueue();
    Assertions.assertFalse(requestManager.getRequest(requestId).isPresent());
  }

  @Test
  public void itSetsRequestStateToDeletedIfTaskCleanupFails() {
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
    launchTask(request, firstDeploy, 2, TaskState.TASK_RUNNING);

    Assertions.assertEquals(
      requestId,
      requestManager.getActiveRequests().iterator().next().getRequest().getId()
    );
    Assertions.assertEquals(2, taskManager.getActiveTaskIds().size());

    requestManager.startDeletingRequest(
      request,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()
    );

    Assertions.assertEquals(
      requestId,
      requestManager.getCleanupRequests().get(0).getRequestId()
    );
    Assertions.assertEquals(
      RequestState.DELETING,
      requestManager.getRequest(requestId).get().getState()
    );

    statusUpdate(firstTask, TaskState.TASK_FAILED);
    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());

    cleaner.drainCleanupQueue();
    Assertions.assertEquals(0, taskManager.getCleanupTaskIds().size());
    killKilledTasks();
    cleaner.drainCleanupQueue();
    Assertions.assertFalse(requestManager.getRequest(requestId).isPresent());
  }

  @Test
  public void testMaxTasksPerOffer() {
    configuration.setMaxTasksPerOffer(3);

    initRequest();
    initFirstDeploy();

    requestResource.postRequest(
      request.toBuilder().setInstances(Optional.of(20)).build(),
      singularityUser
    );
    scheduler.drainPendingQueue();

    sms.resourceOffers(Arrays.asList(createOffer(36, 12024, 50000))).join();

    Assertions.assertTrue(taskManager.getActiveTasks().size() == 3);

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(20, 20000, 50000, "agent1", "host1"),
          createOffer(20, 20000, 50000, "agent2", "host2")
        )
      )
      .join();

    Assertions.assertTrue(taskManager.getActiveTasks().size() == 9);

    configuration.setMaxTasksPerOffer(0);

    resourceOffers();

    Assertions.assertTrue(taskManager.getActiveTasks().size() == 20);
  }

  @Test
  public void testRequestedPorts() {
    final SingularityDeployBuilder deployBuilder = dockerDeployWithPorts();

    initRequest();
    initAndFinishDeploy(request, deployBuilder, Optional.of(new Resources(1, 64, 3, 0)));
    requestResource.postRequest(
      request.toBuilder().setInstances(Optional.of(2)).build(),
      singularityUser
    );
    scheduler.drainPendingQueue();

    String[] portRangeWithNoRequestedPorts = { "65:70" };
    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent1",
            "host1",
            Optional.<String>empty(),
            Collections.<String, String>emptyMap(),
            portRangeWithNoRequestedPorts
          )
        )
      )
      .join();
    Assertions.assertEquals(0, taskManager.getActiveTasks().size());

    String[] portRangeWithSomeRequestedPorts = { "80:82" };
    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent1",
            "host1",
            Optional.<String>empty(),
            Collections.<String, String>emptyMap(),
            portRangeWithSomeRequestedPorts
          )
        )
      )
      .join();
    Assertions.assertEquals(0, taskManager.getActiveTasks().size());

    String[] portRangeWithRequestedButNotEnoughPorts = { "80:80", "8080:8080" };
    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent1",
            "host1",
            Optional.<String>empty(),
            Collections.<String, String>emptyMap(),
            portRangeWithRequestedButNotEnoughPorts
          )
        )
      )
      .join();
    Assertions.assertEquals(0, taskManager.getActiveTasks().size());

    String[] portRangeWithNeededPorts = { "80:83", "8080:8080" };
    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent1",
            "host1",
            Optional.<String>empty(),
            Collections.<String, String>emptyMap(),
            portRangeWithNeededPorts
          )
        )
      )
      .join();
    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testPortUsage() {
    SingularityRequest ra = new SingularityRequestBuilder("a", RequestType.SERVICE)
      .setInstances(Optional.of(2))
      .build();
    SingularityRequest rb = new SingularityRequestBuilder("b", RequestType.SERVICE)
      .setInstances(Optional.of(2))
      .build();

    requestResource.postRequest(ra, singularityUser);
    requestResource.postRequest(rb, singularityUser);

    deployRequest(ra, "a0", new Resources(1, 1, 3));
    deployRequest(rb, "b0", new Resources(1, 1, 3));

    scheduler.drainPendingQueue();

    // mesos offers ports as an array of ranges - eg ["65:70", "80:80"]
    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent1",
            "host1",
            Optional.<String>empty(),
            Collections.<String, String>emptyMap(),
            new String[] {}
          )
        )
      )
      .join();
    Assertions.assertEquals(0, taskManager.getActiveTasks().size());

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent1",
            "host1",
            Optional.<String>empty(),
            Collections.<String, String>emptyMap(),
            new String[] { "60:61", "70:71", "80:81", "90:91", "100:101", "110:111" }
          )
        )
      )
      .join();
    Assertions.assertEquals(4, taskManager.getActiveTasks().size());
  }

  private SingularityDeployBuilder dockerDeployWithPorts() {
    final SingularityDockerPortMapping literalMapping = new SingularityDockerPortMapping(
      Optional.<SingularityPortMappingType>empty(),
      80,
      Optional.of(SingularityPortMappingType.LITERAL),
      8080,
      Optional.<String>empty()
    );
    final SingularityDockerPortMapping offerMapping = new SingularityDockerPortMapping(
      Optional.<SingularityPortMappingType>empty(),
      81,
      Optional.of(SingularityPortMappingType.FROM_OFFER),
      0,
      Optional.of("udp")
    );
    final SingularityContainerInfo containerInfo = new SingularityContainerInfo(
      SingularityContainerType.DOCKER,
      Optional.<List<SingularityVolume>>empty(),
      Optional.of(
        new SingularityDockerInfo(
          "docker-image",
          true,
          SingularityDockerNetworkType.BRIDGE,
          Optional.of(Arrays.asList(literalMapping, offerMapping)),
          Optional.of(false),
          Optional.of(ImmutableMap.of("env", "var=value")),
          Optional.empty()
        )
      )
    );
    final SingularityDeployBuilder deployBuilder = new SingularityDeployBuilder(
      requestId,
      "test-docker-ports-deploy"
    );
    deployBuilder.setContainerInfo(Optional.of(containerInfo));
    return deployBuilder;
  }

  @Test
  public void testQueueMultipleOneOffs() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(
      requestId,
      RequestType.ON_DEMAND
    );
    requestResource.postRequest(bldr.build(), singularityUser);
    deploy("on_demand_deploy");
    deployChecker.checkDeploys();
    long now = System.currentTimeMillis();

    requestManager.addToPendingQueue(
      new SingularityPendingRequest(
        requestId,
        "on_demand_deploy",
        now,
        Optional.<String>empty(),
        PendingType.ONEOFF,
        Optional.<List<String>>empty(),
        Optional.<String>empty(),
        Optional.<Boolean>empty(),
        Optional.<String>empty(),
        Optional.<String>empty()
      )
    );
    requestManager.addToPendingQueue(
      new SingularityPendingRequest(
        requestId,
        "on_demand_deploy",
        now + 1,
        Optional.<String>empty(),
        PendingType.ONEOFF,
        Optional.<List<String>>empty(),
        Optional.<String>empty(),
        Optional.<Boolean>empty(),
        Optional.<String>empty(),
        Optional.<String>empty()
      )
    );

    scheduler.drainPendingQueue();

    Assertions.assertEquals(2, taskManager.getPendingTaskIds().size());
  }

  @Test
  public void testPriorityFreezeKillsActiveTasks() {
    final SingularityRequest lowPriorityRequest = new SingularityRequestBuilder(
      "lowPriorityRequest",
      RequestType.WORKER
    )
      .setTaskPriorityLevel(Optional.of(.25))
      .build();
    saveRequest(lowPriorityRequest);
    final SingularityRequest mediumPriorityRequest = new SingularityRequestBuilder(
      "mediumPriorityRequest",
      RequestType.WORKER
    )
      .setTaskPriorityLevel(Optional.of(.5))
      .build();
    saveRequest(mediumPriorityRequest);
    final SingularityRequest highPriorityRequest = new SingularityRequestBuilder(
      "highPriorityRequest",
      RequestType.WORKER
    )
      .setTaskPriorityLevel(Optional.of(.75))
      .build();
    saveRequest(highPriorityRequest);

    final SingularityDeploy lowPriorityDeploy = initAndFinishDeploy(
      lowPriorityRequest,
      "lowPriorityDeploy"
    );
    final SingularityDeploy mediumPriorityDeploy = initAndFinishDeploy(
      mediumPriorityRequest,
      "mediumPriorityDeploy"
    );
    SingularityDeploy highPriorityDeploy = initAndFinishDeploy(
      highPriorityRequest,
      "highPriorityDeploy"
    );

    final SingularityTask lowPriorityTask = launchTask(
      lowPriorityRequest,
      lowPriorityDeploy,
      2,
      1,
      TaskState.TASK_RUNNING
    );
    final SingularityTask mediumPriorityTask = launchTask(
      mediumPriorityRequest,
      mediumPriorityDeploy,
      1,
      1,
      TaskState.TASK_RUNNING
    );
    final SingularityTask highPriorityTask = launchTask(
      highPriorityRequest,
      highPriorityDeploy,
      10,
      1,
      TaskState.TASK_RUNNING
    );

    // priority freeze of .5 means that lowPriorityRequest's task should have a cleanup
    priorityResource.createPriorityFreeze(
      singularityUser,
      new SingularityPriorityFreeze(.5, true, Optional.of("test"), Optional.empty())
    );

    // perform the killing
    priorityKillPoller.runActionOnPoll();

    // assert lowPriorityRequest has a PRIORITY_KILL task cleanup and that mediumPriorityRequest and highPriorityRequest should not have cleanups
    Assertions.assertEquals(
      TaskCleanupType.PRIORITY_KILL,
      taskManager
        .getTaskCleanup(lowPriorityTask.getTaskId().getId())
        .get()
        .getCleanupType()
    );

    Assertions.assertEquals(
      false,
      taskManager.getTaskCleanup(mediumPriorityTask.getTaskId().getId()).isPresent()
    );
    Assertions.assertEquals(
      false,
      taskManager.getTaskCleanup(highPriorityTask.getTaskId().getId()).isPresent()
    );

    // kill task(s) with cleanups
    cleaner.drainCleanupQueue();
    killKilledTasks();

    // assert lowPriorityTask was killed, mediumPriorityTask and highPriorityTask are still running
    Assertions.assertEquals(
      ExtendedTaskState.TASK_KILLED,
      taskManager
        .getTaskHistory(lowPriorityTask.getTaskId())
        .get()
        .getLastTaskUpdate()
        .get()
        .getTaskState()
    );
    Assertions.assertEquals(
      ExtendedTaskState.TASK_RUNNING,
      taskManager
        .getTaskHistory(mediumPriorityTask.getTaskId())
        .get()
        .getLastTaskUpdate()
        .get()
        .getTaskState()
    );
    Assertions.assertEquals(
      ExtendedTaskState.TASK_RUNNING,
      taskManager
        .getTaskHistory(highPriorityTask.getTaskId())
        .get()
        .getLastTaskUpdate()
        .get()
        .getTaskState()
    );

    // assert lowPriorityRequest has a pending task
    final SingularityPendingTaskId pendingTaskId = taskManager.getPendingTaskIds().get(0);
    Assertions.assertEquals(PendingType.TASK_DONE, pendingTaskId.getPendingType());
    Assertions.assertEquals(lowPriorityRequest.getId(), pendingTaskId.getRequestId());

    // end the priority freeze
    priorityResource.deleteActivePriorityFreeze(singularityUser);

    // launch task(s)
    scheduler.drainPendingQueue();
    resourceOffers();

    // assert lowPriorityRequest has a new task running
    Assertions.assertNotEquals(
      lowPriorityTask.getTaskId(),
      taskManager.getActiveTaskIdsForRequest(lowPriorityRequest.getId()).get(0).getId()
    );
  }

  @Test
  public void testPriorityFreezeDoesntLaunchTasks() {
    // deploy lowPriorityRequest (affected by priority freeze)
    final SingularityRequest lowPriorityRequest = new SingularityRequestBuilder(
      "lowPriorityRequest",
      RequestType.ON_DEMAND
    )
      .setTaskPriorityLevel(Optional.of(.25))
      .build();
    saveRequest(lowPriorityRequest);
    deployResource.deploy(
      new SingularityDeployRequest(
        new SingularityDeployBuilder(lowPriorityRequest.getId(), "d1")
          .setCommand(Optional.of("cmd"))
          .build(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    // deploy medium priority request (NOT affected by priority freeze)
    final SingularityRequest mediumPriorityRequest = new SingularityRequestBuilder(
      "mediumPriorityRequest",
      RequestType.ON_DEMAND
    )
      .setTaskPriorityLevel(Optional.of(.5))
      .build();
    saveRequest(mediumPriorityRequest);
    deployResource.deploy(
      new SingularityDeployRequest(
        new SingularityDeployBuilder(mediumPriorityRequest.getId(), "d2")
          .setCommand(Optional.of("cmd"))
          .build(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    // create priority freeze
    priorityManager.createPriorityFreeze(
      new SingularityPriorityFreezeParent(
        new SingularityPriorityFreeze(
          0.3,
          true,
          Optional.<String>empty(),
          Optional.<String>empty()
        ),
        System.currentTimeMillis(),
        Optional.<String>empty()
      )
    );

    // launch both tasks
    requestResource.scheduleImmediately(
      singularityUser,
      lowPriorityRequest.getId(),
      ((SingularityRunNowRequest) null)
    );
    requestResource.scheduleImmediately(
      singularityUser,
      mediumPriorityRequest.getId(),
      ((SingularityRunNowRequest) null)
    );

    // drain pending queue
    scheduler.drainPendingQueue();
    resourceOffers();

    // assert that lowPriorityRequest has a pending task
    Assertions.assertEquals(1, taskManager.getPendingTaskIds().size());
    Assertions.assertEquals(
      lowPriorityRequest.getId(),
      taskManager.getPendingTaskIds().get(0).getRequestId()
    );

    // assert that only mediumPriorityRequest has an active task
    Assertions.assertEquals(
      0,
      taskManager.getActiveTaskIdsForRequest(lowPriorityRequest.getId()).size()
    );
    Assertions.assertEquals(
      1,
      taskManager.getActiveTaskIdsForRequest(mediumPriorityRequest.getId()).size()
    );

    // delete priority freeze
    Assertions.assertEquals(
      SingularityDeleteResult.DELETED,
      priorityManager.deleteActivePriorityFreeze()
    );

    // drain pending
    scheduler.drainPendingQueue();
    resourceOffers();

    // check that both requests have active tasks
    Assertions.assertEquals(
      1,
      taskManager.getActiveTaskIdsForRequest(lowPriorityRequest.getId()).size()
    );
    Assertions.assertEquals(
      1,
      taskManager.getActiveTaskIdsForRequest(mediumPriorityRequest.getId()).size()
    );
  }

  @Test
  public void testObsoletePendingRequestsRemoved() {
    initRequest();
    initFirstDeploy();
    SingularityTask taskOne = startTask(firstDeploy);
    requestResource.pause(requestId, Optional.empty(), singularityUser);
    requestManager.addToPendingQueue(
      new SingularityPendingRequest(
        requestId,
        firstDeployId,
        System.currentTimeMillis(),
        Optional.<String>empty(),
        PendingType.NEW_DEPLOY,
        Optional.<Boolean>empty(),
        Optional.<String>empty()
      )
    );

    Assertions.assertEquals(requestManager.getPendingRequests().size(), 1);
    scheduler.drainPendingQueue();

    Assertions.assertEquals(requestManager.getPendingRequests().size(), 0);
  }

  @Test
  public void testCronScheduleChanges() throws Exception {
    final String requestId = "test-change-cron";
    final String oldSchedule = "*/5 * * * *";
    final String oldScheduleQuartz = "0 */5 * * * ?";
    final String newSchedule = "*/30 * * * *";
    final String newScheduleQuartz = "0 */30 * * * ?";

    SingularityRequest request = new SingularityRequestBuilder(
      requestId,
      RequestType.SCHEDULED
    )
      .setSchedule(Optional.of(oldSchedule))
      .build();

    request =
      validator.checkSingularityRequest(
        request,
        Optional.<SingularityRequest>empty(),
        Optional.<SingularityDeploy>empty(),
        Optional.<SingularityDeploy>empty()
      );

    saveRequest(request);

    Assertions.assertEquals(
      oldScheduleQuartz,
      requestManager.getRequest(requestId).get().getRequest().getQuartzScheduleSafe()
    );

    initAndFinishDeploy(request, "1");

    scheduler.drainPendingQueue();

    final SingularityRequest newRequest = request
      .toBuilder()
      .setSchedule(Optional.of(newSchedule))
      .setQuartzSchedule(Optional.<String>empty())
      .build();

    final SingularityDeploy newDeploy = new SingularityDeployBuilder(request.getId(), "2")
      .setCommand(Optional.of("sleep 100"))
      .build();

    deployResource.deploy(
      new SingularityDeployRequest(
        newDeploy,
        Optional.empty(),
        Optional.empty(),
        Optional.of(newRequest)
      ),
      singularityUser
    );

    deployChecker.checkDeploys();

    scheduler.drainPendingQueue();

    Assertions.assertEquals(
      newScheduleQuartz,
      requestManager.getRequest(requestId).get().getRequest().getQuartzScheduleSafe()
    );
  }

  @Test
  public void testImmediateRunReplacesScheduledTask() {
    initScheduledRequest();

    SingularityDeploy deploy = SingularityDeploy
      .newBuilder(requestId, firstDeployId)
      .setCommand(Optional.of("sleep 100"))
      .build();
    SingularityDeployRequest singularityDeployRequest = new SingularityDeployRequest(
      deploy,
      Optional.empty(),
      Optional.empty(),
      Optional.empty()
    );
    deployResource.deploy(singularityDeployRequest, singularityUser);

    scheduler.drainPendingQueue();

    SingularityPendingTask task1 = createAndSchedulePendingTask(firstDeployId);

    Assertions.assertEquals(1, taskManager.getPendingTaskIds().size());
    Assertions.assertEquals(
      PendingType.NEW_DEPLOY,
      taskManager.getPendingTaskIds().get(0).getPendingType()
    );

    requestManager.addToPendingQueue(
      new SingularityPendingRequest(
        requestId,
        deploy.getId(),
        System.currentTimeMillis(),
        Optional.empty(),
        PendingType.IMMEDIATE,
        deploy.getSkipHealthchecksOnDeploy(),
        Optional.empty()
      )
    );
    scheduler.drainPendingQueue();

    Assertions.assertEquals(1, taskManager.getPendingTaskIds().size());
    Assertions.assertEquals(
      PendingType.IMMEDIATE,
      taskManager.getPendingTaskIds().get(0).getPendingType()
    );
  }

  @Test
  public void testSchedulerDropsMultipleScheduledTaskInstances() {
    initScheduledRequest();

    SingularityDeploy deploy = SingularityDeploy
      .newBuilder(requestId, firstDeployId)
      .setCommand(Optional.of("sleep 100"))
      .build();
    SingularityDeployRequest singularityDeployRequest = new SingularityDeployRequest(
      deploy,
      Optional.empty(),
      Optional.empty(),
      Optional.empty()
    );
    deployResource.deploy(singularityDeployRequest, singularityUser);

    scheduler.drainPendingQueue();
    requestManager.addToPendingQueue(
      new SingularityPendingRequest(
        requestId,
        firstDeployId,
        Instant.now().plus(3, ChronoUnit.DAYS).toEpochMilli(),
        Optional.empty(),
        PendingType.NEW_DEPLOY,
        Optional.empty(),
        Optional.empty()
      )
    );

    SingularityRunNowRequest runNowRequest = new SingularityRunNowRequestBuilder()
    .build();
    requestResource.scheduleImmediately(singularityUser, requestId, runNowRequest);

    Assertions.assertEquals(2, requestManager.getPendingRequests().size());
    Assertions.assertEquals(
      PendingType.IMMEDIATE,
      requestManager.getPendingRequests().get(0).getPendingType()
    );
    Assertions.assertEquals(
      PendingType.NEW_DEPLOY,
      requestManager.getPendingRequests().get(1).getPendingType()
    );

    scheduler.drainPendingQueue();
    Assertions.assertEquals(1, taskManager.getPendingTaskIds().size());
    Assertions.assertEquals(
      PendingType.IMMEDIATE,
      taskManager.getPendingTaskIds().get(0).getPendingType()
    );

    Assertions.assertEquals(0, requestManager.getPendingRequests().size());
  }

  @Test
  public void testInvalidQuartzTimeZoneErrors() {
    SingularityRequest req = new SingularityRequestBuilder(
      requestId,
      RequestType.SCHEDULED
    )
      .setQuartzSchedule(Optional.of("*/1 * * * * ? 2020"))
      .setScheduleType(Optional.of(ScheduleType.QUARTZ))
      .setScheduleTimeZone(Optional.of("invalid_timezone"))
      .build();

    Assertions.assertThrows(
      WebApplicationException.class,
      () -> requestResource.postRequest(req, singularityUser)
    );
  }

  @Test
  public void testDifferentQuartzTimeZones() {
    final Optional<String> schedule = Optional.of("* 30 14 22 3 ? 2083");

    SingularityRequest requestEST = new SingularityRequestBuilder(
      "est_id",
      RequestType.SCHEDULED
    )
      .setSchedule(schedule)
      .setScheduleType(Optional.of(ScheduleType.QUARTZ))
      .setScheduleTimeZone(Optional.of("EST")) // fixed in relation to GMT
      .build();

    SingularityRequest requestGMT = new SingularityRequestBuilder(
      "gmt_id",
      RequestType.SCHEDULED
    )
      .setSchedule(schedule)
      .setScheduleType(Optional.of(ScheduleType.QUARTZ))
      .setScheduleTimeZone(Optional.of("GMT"))
      .build();

    requestResource.postRequest(requestEST, singularityUser);
    requestResource.postRequest(requestGMT, singularityUser);

    SingularityDeploy deployEST = new SingularityDeployBuilder(
      requestEST.getId(),
      "est_deploy_id"
    )
      .setCommand(Optional.of("sleep 1"))
      .build();

    SingularityDeploy deployGMT = new SingularityDeployBuilder(
      requestGMT.getId(),
      "gmt_deploy_id"
    )
      .setCommand(Optional.of("sleep 1"))
      .build();

    deployResource.deploy(
      new SingularityDeployRequest(
        deployEST,
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );
    deployResource.deploy(
      new SingularityDeployRequest(
        deployGMT,
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

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
    Assertions.assertEquals(nextRunEST - nextRunGMT, fiveHoursInMilliseconds);
  }

  @Test
  public void testDeployCleanupOverwritesTaskBounceCleanup() {
    initRequest();
    initFirstDeploy();
    final SingularityTask oldTask = startTask(firstDeploy);

    taskResource.killTask(
      oldTask.getTaskId().getId(),
      Optional.of(
        new SingularityKillTaskRequest(
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.of(true),
          Optional.empty()
        )
      ),
      singularityUser
    );

    final Optional<SingularityTaskCleanup> taskCleanup = taskManager.getTaskCleanup(
      oldTask.getTaskId().getId()
    );
    Assertions.assertTrue(taskCleanup.isPresent());
    Assertions.assertEquals(
      TaskCleanupType.USER_REQUESTED_TASK_BOUNCE,
      taskCleanup.get().getCleanupType()
    );

    initSecondDeploy();
    startTask(secondDeploy);
    deployChecker.checkDeploys();
    deployChecker.checkDeploys();

    Assertions.assertEquals(
      DeployState.SUCCEEDED,
      deployManager.getDeployResult(requestId, secondDeployId).get().getDeployState()
    );
    Assertions.assertEquals(
      TaskCleanupType.DEPLOY_STEP_FINISHED,
      taskManager.getTaskCleanup(oldTask.getTaskId().getId()).get().getCleanupType()
    );

    cleaner.drainCleanupQueue();

    Assertions.assertFalse(
      taskManager.getTaskCleanup(oldTask.getTaskId().getId()).isPresent()
    );
  }

  @Test
  public void testCleanerFindsTasksWithSkippedHealthchecks() {
    initRequest();
    resourceOffers(2); // set up agents so scale validate will pass

    SingularityRequest request = requestResource
      .getRequest(requestId, singularityUser)
      .getRequest();

    long now = System.currentTimeMillis();

    requestManager.saveHistory(
      new SingularityRequestHistory(
        now,
        Optional.<String>empty(),
        RequestHistoryType.UPDATED,
        request
          .toBuilder()
          .setSkipHealthchecks(Optional.of(true))
          .setInstances(Optional.of(2))
          .build(),
        Optional.<String>empty()
      )
    );

    firstDeploy =
      initDeploy(
        new SingularityDeployBuilder(request.getId(), firstDeployId)
          .setCommand(Optional.of("sleep 100"))
          .setHealthcheckUri(Optional.of("http://uri")),
        System.currentTimeMillis()
      );

    SingularityTask taskOne = launchTask(
      request,
      firstDeploy,
      now + 1000,
      now + 2000,
      1,
      TaskState.TASK_RUNNING
    );

    finishDeploy(
      new SingularityDeployMarker(
        requestId,
        firstDeployId,
        now + 2000,
        Optional.<String>empty(),
        Optional.<String>empty()
      ),
      firstDeploy
    );

    SingularityRequest updatedRequest = request
      .toBuilder()
      .setSkipHealthchecks(Optional.<Boolean>empty())
      .setInstances(Optional.of(2))
      .build();

    requestManager.saveHistory(
      new SingularityRequestHistory(
        now + 3000,
        Optional.<String>empty(),
        RequestHistoryType.UPDATED,
        updatedRequest,
        Optional.<String>empty()
      )
    );

    SingularityTask newTaskTwoWithCheck = prepTask(
      updatedRequest,
      firstDeploy,
      now + 4000,
      2
    );
    taskManager.createTaskAndDeletePendingTask(newTaskTwoWithCheck);
    statusUpdate(newTaskTwoWithCheck, TaskState.TASK_RUNNING, Optional.of(now + 5000));
    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(200),
        Optional.of(1000L),
        now + 6000,
        Optional.<String>empty(),
        Optional.<String>empty(),
        newTaskTwoWithCheck.getTaskId(),
        Optional.<Boolean>empty()
      )
    );

    SingularityTask unhealthyTaskThree = prepTask(
      updatedRequest,
      firstDeploy,
      now + 4000,
      3
    );
    taskManager.createTaskAndDeletePendingTask(unhealthyTaskThree);
    statusUpdate(unhealthyTaskThree, TaskState.TASK_RUNNING, Optional.of(now + 5000));

    List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIdsForRequest(
      requestId
    );
    List<SingularityTaskId> healthyTaskIds = deployHealthHelper.getHealthyTasks(
      updatedRequest,
      firstDeploy,
      activeTaskIds,
      false
    );
    Assertions.assertTrue(!healthyTaskIds.contains(unhealthyTaskThree.getTaskId()));
    Assertions.assertEquals(2, healthyTaskIds.size()); // Healthchecked and skip-healthchecked tasks should both be here
    Assertions.assertEquals(
      DeployHealth.WAITING,
      deployHealthHelper.getDeployHealth(
        updatedRequest,
        firstDeploy,
        activeTaskIds,
        false
      )
    );

    taskManager.saveHealthcheckResult(
      new SingularityTaskHealthcheckResult(
        Optional.of(200),
        Optional.of(1000L),
        now + 6000,
        Optional.<String>empty(),
        Optional.<String>empty(),
        unhealthyTaskThree.getTaskId(),
        Optional.<Boolean>empty()
      )
    );
    Assertions.assertEquals(
      DeployHealth.HEALTHY,
      deployHealthHelper.getDeployHealth(
        updatedRequest,
        firstDeploy,
        activeTaskIds,
        false
      )
    );
  }

  @Test
  public void testScaleWithBounceDoesNotLaunchExtraInstances() {
    initRequest();
    initFirstDeploy();
    launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    requestResource.scale(
      requestId,
      new SingularityScaleRequest(
        Optional.of(5),
        Optional.of(1L),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(true),
        Optional.empty(),
        Optional.empty()
      ),
      singularityUser
    );

    Assertions.assertEquals(1, requestManager.getCleanupRequests().size());
    cleaner.drainCleanupQueue();
    Assertions.assertEquals(1, taskManager.getNumCleanupTasks());

    scheduler.drainPendingQueue();
    Assertions.assertEquals(5, taskManager.getPendingTaskIds().size());
  }

  @Test
  public void testAcceptOffersWithRoleForRequestWithRole() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(
      requestId,
      RequestType.ON_DEMAND
    );
    bldr.setRequiredRole(Optional.of("test-role"));
    requestResource.postRequest(bldr.build(), singularityUser);
    deploy("d2");

    SingularityRunNowRequest runNowRequest = new SingularityRunNowRequestBuilder()
      .setResources(new Resources(2, 2, 0))
      .build();
    requestResource.scheduleImmediately(singularityUser, requestId, runNowRequest);

    scheduler.drainPendingQueue();

    SingularityPendingTask pendingTaskWithResources = taskManager
      .getPendingTasks()
      .get(0);
    Assertions.assertTrue(pendingTaskWithResources.getResources().isPresent());
    Assertions.assertEquals(
      pendingTaskWithResources.getResources().get().getCpus(),
      2,
      0.0
    );

    sms.resourceOffers(Arrays.asList(createOffer(5, 5, 5))).join();

    pendingTaskWithResources = taskManager.getPendingTasks().get(0);
    Assertions.assertTrue(pendingTaskWithResources.getResources().isPresent());
    Assertions.assertEquals(
      pendingTaskWithResources.getResources().get().getCpus(),
      2,
      0.0
    );

    sms
      .resourceOffers(Arrays.asList(createOffer(5, 5, 5, Optional.of("test-role"))))
      .join();
    SingularityTask task = taskManager.getActiveTasks().get(0);
    Assertions.assertEquals(
      MesosUtils.getNumCpus(
        mesosProtosUtils.toResourceList(task.getMesosTask().getResources()),
        Optional.of("test-role")
      ),
      2.0,
      0.0
    );
  }

  @Test
  public void testNotAcceptOfferWithRoleForRequestWithoutRole() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(
      requestId,
      RequestType.ON_DEMAND
    );
    requestResource.postRequest(bldr.build(), singularityUser);
    deploy("d2");

    SingularityRunNowRequest runNowRequest = new SingularityRunNowRequestBuilder()
      .setResources(new Resources(2, 2, 0))
      .build();
    requestResource.scheduleImmediately(singularityUser, requestId, runNowRequest);

    scheduler.drainPendingQueue();

    SingularityPendingTask pendingTaskWithResources = taskManager
      .getPendingTasks()
      .get(0);
    Assertions.assertTrue(pendingTaskWithResources.getResources().isPresent());
    Assertions.assertEquals(
      pendingTaskWithResources.getResources().get().getCpus(),
      2,
      0.0
    );

    sms
      .resourceOffers(Arrays.asList(createOffer(5, 5, 5, Optional.of("test-role"))))
      .join();

    pendingTaskWithResources = taskManager.getPendingTasks().get(0);
    Assertions.assertTrue(pendingTaskWithResources.getResources().isPresent());
    Assertions.assertEquals(
      pendingTaskWithResources.getResources().get().getCpus(),
      2,
      0.0
    );
  }

  @Test
  public void testMaxOnDemandTasks() {
    SingularityRequestBuilder bldr = new SingularityRequestBuilder(
      requestId,
      RequestType.ON_DEMAND
    );
    bldr.setInstances(Optional.of(1));
    requestResource.postRequest(bldr.build(), singularityUser);
    deploy("on_demand_deploy");
    deployChecker.checkDeploys();

    requestManager.addToPendingQueue(
      new SingularityPendingRequest(
        requestId,
        "on_demand_deploy",
        System.currentTimeMillis(),
        Optional.<String>empty(),
        PendingType.ONEOFF,
        Optional.<List<String>>empty(),
        Optional.<String>empty(),
        Optional.<Boolean>empty(),
        Optional.<String>empty(),
        Optional.<String>empty()
      )
    );
    requestManager.addToPendingQueue(
      new SingularityPendingRequest(
        requestId,
        "on_demand_deploy",
        System.currentTimeMillis(),
        Optional.<String>empty(),
        PendingType.ONEOFF,
        Optional.<List<String>>empty(),
        Optional.<String>empty(),
        Optional.<Boolean>empty(),
        Optional.<String>empty(),
        Optional.<String>empty()
      )
    );

    scheduler.drainPendingQueue();

    resourceOffers();

    Assertions.assertEquals(1, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testCleanupsCreatedOnScaleDown() {
    initRequest();
    SingularityRequestBuilder bldr = request.toBuilder();
    bldr.setInstances(Optional.of(2));
    requestResource.postRequest(bldr.build(), singularityUser);
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
    Assertions.assertEquals(0, taskManager.getNumCleanupTasks());

    bldr.setInstances(Optional.of(1));
    requestResource.postRequest(bldr.build(), singularityUser);
    Assertions.assertEquals(1, taskManager.getNumCleanupTasks());
    Assertions.assertEquals(
      taskManager.getCleanupTaskIds().get(0),
      secondTask.getTaskId()
    );
  }

  @Test
  public void testRecoveredTask() {
    // set up the agent first
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent1", "host1", Optional.of("rack1")))
      )
      .join();

    initRequest();
    initFirstDeploy();
    SingularityTask task = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    Assertions.assertEquals(1, taskManager.getNumActiveTasks());
    TaskStatus lost = TaskStatus
      .newBuilder()
      .setTaskId(MesosProtosUtils.toTaskId(task.getMesosTask().getTaskId()))
      .setAgentId(MesosProtosUtils.toAgentId(task.getAgentId()))
      .setReason(Reason.REASON_AGENT_REMOVED)
      .setMessage("health check timed out")
      .setState(TaskState.TASK_LOST)
      .build();

    sms.statusUpdate(lost).join();

    Assertions.assertEquals(0, taskManager.getNumActiveTasks());
    Assertions.assertTrue(taskManager.getTaskHistory(task.getTaskId()).isPresent());

    TaskStatus recovered = TaskStatus
      .newBuilder()
      .setTaskId(MesosProtosUtils.toTaskId(task.getMesosTask().getTaskId()))
      .setAgentId(MesosProtosUtils.toAgentId(task.getAgentId()))
      .setReason(Reason.REASON_AGENT_REREGISTERED)
      .setMessage("agent reregistered")
      .setState(TaskState.TASK_RUNNING)
      .build();

    sms.statusUpdate(recovered).join();

    Assertions.assertEquals(1, taskManager.getNumActiveTasks());
    Assertions.assertEquals(1, requestManager.getSizeOfPendingQueue());
  }

  @Test
  public void testRecoveredTaskIsRecoveredIfLoadBalancerRemoveIsStarted() {
    // set up the agent first
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent1", "host1", Optional.of("rack1")))
      )
      .join();

    initLoadBalancedRequest();
    initLoadBalancedDeploy();
    SingularityTask task = launchTask(request, firstDeploy, 1, TaskState.TASK_RUNNING);

    Assertions.assertEquals(1, taskManager.getNumActiveTasks());
    TaskStatus lost = TaskStatus
      .newBuilder()
      .setTaskId(MesosProtosUtils.toTaskId(task.getMesosTask().getTaskId()))
      .setAgentId(MesosProtosUtils.toAgentId(task.getAgentId()))
      .setReason(Reason.REASON_AGENT_REMOVED)
      .setMessage("health check timed out")
      .setState(TaskState.TASK_LOST)
      .build();

    sms.statusUpdate(lost).join();

    Assertions.assertEquals(0, taskManager.getNumActiveTasks());
    SingularityTaskId taskId = task.getTaskId();
    Assertions.assertTrue(taskManager.getTaskHistory(taskId).isPresent());

    taskManager.saveLoadBalancerState(
      taskId,
      LoadBalancerRequestType.REMOVE,
      new SingularityLoadBalancerUpdate(
        LoadBalancerRequestState.UNKNOWN,
        new LoadBalancerRequestId(
          taskId.getId(),
          LoadBalancerRequestType.REMOVE,
          Optional.<Integer>empty()
        ),
        Optional.empty(),
        System.currentTimeMillis(),
        LoadBalancerMethod.DELETE,
        Optional.empty()
      )
    );

    TaskStatus recovered = TaskStatus
      .newBuilder()
      .setTaskId(MesosProtosUtils.toTaskId(task.getMesosTask().getTaskId()))
      .setAgentId(MesosProtosUtils.toAgentId(task.getAgentId()))
      .setReason(Reason.REASON_AGENT_REREGISTERED)
      .setMessage("agent reregistered")
      .setState(TaskState.TASK_RUNNING)
      .build();

    sms.statusUpdate(recovered).join();

    newTaskChecker
      .getTaskCheckFutures()
      .forEach(
        f -> {
          try {
            f.get(5, TimeUnit.SECONDS);
          } catch (TimeoutException te) {
            // Didn't see that....
          } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
          }
        }
      );

    Assertions.assertEquals(1, taskManager.getNumActiveTasks());
    Assertions.assertEquals(0, taskManager.getNumCleanupTasks());
    Assertions.assertEquals(1, requestManager.getSizeOfPendingQueue());
    Assertions.assertTrue(
      taskManager.getLoadBalancerState(taskId, LoadBalancerRequestType.ADD).isPresent()
    );
  }

  @Test
  public void itRetriesLostShortRunningRequests() {
    runTest(RequestType.ON_DEMAND, Reason.REASON_AGENT_RESTARTED, true);
  }

  @Test
  public void itDoesNotRetryLostLongRunningRequests() {
    runTest(RequestType.SERVICE, Reason.REASON_AGENT_RESTARTED, false);
  }

  @Test
  public void itDoesNotRetryLostRequestsDueToNonAgentFailures() {
    runTest(RequestType.ON_DEMAND, Reason.REASON_CONTAINER_LIMITATION_DISK, false);
  }

  private void runTest(RequestType requestType, Reason reason, boolean shouldRetry) {
    initRequestWithType(requestType, false);
    initFirstDeploy();

    SingularityTask task = startTask(firstDeploy);
    Assertions.assertEquals(0, taskManager.getPendingTaskIds().size());
    Assertions.assertEquals(0, requestManager.getPendingRequests().size());

    try {
      updateHandler
        .processStatusUpdateAsync(
          TaskStatus
            .newBuilder()
            .setState(TaskState.TASK_LOST)
            .setReason(reason)
            .setTaskId(TaskID.newBuilder().setValue(task.getTaskId().getId()))
            .build()
        )
        .get();
    } catch (InterruptedException | ExecutionException e) {
      Assertions.assertTrue(false);
    }

    if (shouldRetry) {
      Assertions.assertEquals(requestManager.getPendingRequests().size(), 1);
      Assertions.assertEquals(
        requestManager.getPendingRequests().get(0).getPendingType(),
        PendingType.RETRY
      );
    } else {
      if (requestManager.getPendingRequests().size() > 0) {
        Assertions.assertEquals(
          requestManager.getPendingRequests().get(0).getPendingType(),
          PendingType.TASK_DONE
        );
      }
    }
    scheduler.drainPendingQueue();
  }
}
