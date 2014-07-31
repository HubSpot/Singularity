package com.hubspot.singularity.scheduler;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.Protos.SlaveID;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.TaskStatus;
import org.apache.mesos.SchedulerDriver;
import org.apache.zookeeper.data.Stat;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;
import com.hubspot.singularity.resources.RequestResource;

public class SingularitySchedulerTest {

  
  @Inject
  private Provider<SingularitySchedulerStateCache> stateCacheProvider;
  @Inject
  private SingularityMesosScheduler sms;
  @Inject
  private RequestManager requestManager;
  @Inject
  private DeployManager deployManager;
  @Inject
  private TaskManager taskManager;
  @Inject
  private CuratorFramework cf;
  @Inject
  private TestingServer ts;
  @Inject
  private SchedulerDriver driver;
  @Inject
  private SingularityScheduler scheduler;
  @Inject
  private TestingLoadBalancerClient testingLoadBalancerClient;
  @Inject
  private SingularityDeployChecker deployChecker;
  @Inject
  private RequestResource requestResource;
  @Inject
  private SingularityCleaner cleaner;
  
  @Before
  public void setup() {
    Injector i = Guice.createInjector(new SingularityTestModule());
    
    i.injectMembers(this);
  }
  
  @After
  public void teardown() throws Exception {
    cf.close();
    ts.close();
  }
  
  public SingularityTask launchTask(SingularityRequest request, SingularityDeploy deploy, TaskState initialTaskState) {
    SingularityTaskId taskId = new SingularityTaskId(request.getId(), deploy.getId(), System.currentTimeMillis(), 1, "host", "rack");
    SingularityPendingTaskId pendingTaskId = new SingularityPendingTaskId(request.getId(), deploy.getId(), System.currentTimeMillis(), 1, PendingType.IMMEDIATE);
    SingularityPendingTask pendingTask = new SingularityPendingTask(pendingTaskId, Optional.<String> absent());
    SingularityTaskRequest taskRequest = new SingularityTaskRequest(request, deploy, pendingTask);
    
    SlaveID slaveId = SlaveID.newBuilder().setValue("slave1").build();
    FrameworkID frameworkId = FrameworkID.newBuilder().setValue("framework1").build();
    TaskID taskIdProto = TaskID.newBuilder().setValue(taskId.toString()).build();
    
    Offer offer = Offer.newBuilder()
        .setId(OfferID.newBuilder().setValue("offer1").build())
        .setFrameworkId(frameworkId)
        .setSlaveId(slaveId)
        .setHostname("host1")
        .build();
    TaskInfo taskInfo = TaskInfo.newBuilder()
        .setSlaveId(slaveId)
        .setTaskId(taskIdProto)
        .setName("name")
        .build();
    
    SingularityTask task = new SingularityTask(taskRequest, taskId, offer, taskInfo);
    
    taskManager.createPendingTasks(Arrays.asList(pendingTask));
    taskManager.createTaskAndDeletePendingTask(task);
    
    statusUpdate(task, initialTaskState);
    
    return task;
  }
  
  public void statusUpdate(SingularityTask task, TaskState state) {
    sms.statusUpdate(driver, TaskStatus.newBuilder()
        .setTaskId(task.getMesosTask().getTaskId())
        .setState(state)
        .build());
  }
  
  private String requestId;
  private SingularityRequest request;
  
  public void initLoadBalancedRequest() {
    privateInitRequest(true, false);
  }
  
  public void initScheduledRequest() {
    privateInitRequest(false, true);
  }

  private void privateInitRequest(boolean isLoadBalanced, boolean isScheduled) {
    requestId = "test-request";

    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId)
      .setLoadBalanced(Optional.of(isLoadBalanced));

    if (isScheduled) {
      bldr.setSchedule(Optional.of("*/1 * * * * ?"));
    }

    request = bldr.build();

    requestManager.saveRequest(request);
  }

  public void initRequest() {
    privateInitRequest(false, false);
  }
  
  private String firstDeployId;
  private SingularityDeployMarker firstDeployMarker;
  private SingularityDeploy firstDeploy;
  
  public void initFirstDeploy() {
    firstDeployId = "firstDeployId";
    firstDeployMarker =  new SingularityDeployMarker(requestId, firstDeployId, System.currentTimeMillis(), Optional.<String> absent());
    firstDeploy = new SingularityDeployBuilder(requestId, firstDeployId).setCommand(Optional.of("sleep 100")).build();
  
    deployManager.saveDeploy(request, firstDeployMarker, firstDeploy);
    
    finishDeploy(firstDeployMarker, firstDeploy);
  }
  
  private String secondDeployId;
  private SingularityDeployMarker secondDeployMarker;
  private SingularityDeploy secondDeploy;
  
  public void initSecondDeploy() {
    secondDeployId = "secondDeployId";
    secondDeployMarker =  new SingularityDeployMarker(requestId, secondDeployId, System.currentTimeMillis(), Optional.<String> absent());
    secondDeploy = new SingularityDeployBuilder(requestId, secondDeployId).setCommand(Optional.of("sleep 100")).build();
    
    deployManager.saveDeploy(request, secondDeployMarker, secondDeploy);
    
    startDeploy(secondDeployMarker);
  }
  
  public void startDeploy(SingularityDeployMarker deployMarker) {
    deployManager.savePendingDeploy(new SingularityPendingDeploy(deployMarker, Optional.<SingularityLoadBalancerUpdate> absent(), DeployState.WAITING));
  
  }
  
  public void finishDeploy(SingularityDeployMarker marker, SingularityDeploy deploy) {
    deployManager.saveDeployResult(marker, new SingularityDeployResult(DeployState.SUCCEEDED));
    
    deployManager.saveNewRequestDeployState(new SingularityRequestDeployState(requestId, Optional.of(marker), Optional.<SingularityDeployMarker> absent()), Optional.<Stat> absent(), false);
  }
  
  public SingularityTask startTask(SingularityDeploy deploy) {
    return launchTask(request, deploy, TaskState.TASK_RUNNING);
  }
  
  @Test
  public void testSchedulerIsolatesPendingTasksBasedOnDeploy() {
    initRequest();
    initFirstDeploy();
    initSecondDeploy();
    
    SingularityPendingTask p1 = new SingularityPendingTask(new SingularityPendingTaskId(requestId, firstDeployId, System.currentTimeMillis(), 1, PendingType.ONEOFF), Optional.<String> absent());
    SingularityPendingTask p2 = new SingularityPendingTask(new SingularityPendingTaskId(requestId, firstDeployId, System.currentTimeMillis(), 1, PendingType.TASK_DONE), Optional.<String> absent());
    SingularityPendingTask p3 = new SingularityPendingTask(new SingularityPendingTaskId(requestId, secondDeployId, System.currentTimeMillis(), 1, PendingType.TASK_DONE), Optional.<String> absent());
    
    List<SingularityPendingTask> pendingTasks = Arrays.asList(p1, p2, p3);
    
    taskManager.createPendingTasks(pendingTasks);
    
    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, secondDeployId, PendingType.NEW_DEPLOY));
    
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
    
    SingularityPendingTaskId taskIdOne = new SingularityPendingTaskId(requestId, firstDeployId, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3), 1, PendingType.IMMEDIATE);
    SingularityPendingTask taskOne = new SingularityPendingTask(taskIdOne, Optional.<String> absent());

    SingularityPendingTaskId taskIdTwo = new SingularityPendingTaskId(requestId, firstDeployId, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1), 2, PendingType.IMMEDIATE);
    SingularityPendingTask taskTwo = new SingularityPendingTask(taskIdTwo, Optional.<String> absent());
    
    SingularityPendingTaskId taskIdThree = new SingularityPendingTaskId(requestId, secondDeployId, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3), 1, PendingType.IMMEDIATE);
    SingularityPendingTask taskThree = new SingularityPendingTask(taskIdThree, Optional.<String> absent());

    SingularityPendingTaskId taskIdFour = new SingularityPendingTaskId(requestId + "hi", firstDeployId, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3), 5, PendingType.IMMEDIATE);
    SingularityPendingTask taskFour = new SingularityPendingTask(taskIdFour, Optional.<String> absent());

    taskManager.createPendingTasks(Arrays.asList(taskOne, taskTwo, taskThree, taskFour));
    
    launchTask(request, secondDeploy, TaskState.TASK_RUNNING);
   
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
    
    SingularityTask firstTask = launchTask(request, firstDeploy, TaskState.TASK_RUNNING);
    
    SingularityDeploy newDeploy = new SingularityDeployBuilder(requestId, "nextDeployId").setCommand(Optional.of("sleep 100")).build();
    
    Assert.assertTrue(taskManager.getPendingTasks().isEmpty());
    Assert.assertTrue(taskManager.getActiveTaskIds().contains(firstTask.getTaskId()));
    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assert.assertTrue(taskManager.getCleanupTaskIds().isEmpty());
    
    requestResource.deploy(requestId, newDeploy, Optional.<String> absent());
    deployChecker.checkDeploys();
    scheduler.drainPendingQueue(stateCacheProvider.get());
    
    // no second task should be scheduled
    
    Assert.assertTrue(taskManager.getPendingTasks().isEmpty());
    Assert.assertTrue(taskManager.getActiveTaskIds().contains(firstTask.getTaskId()));
    Assert.assertEquals(1, taskManager.getActiveTaskIds().size());
    Assert.assertTrue(taskManager.getCleanupTaskIds().isEmpty());

    statusUpdate(firstTask, TaskState.TASK_FINISHED);
    scheduler.drainPendingQueue(stateCacheProvider.get());
    
    Assert.assertTrue(!taskManager.getPendingTasks().isEmpty());
    Assert.assertTrue(taskManager.getActiveTaskIds().isEmpty());
    Assert.assertTrue(taskManager.getCleanupTaskIds().isEmpty());

    SingularityPendingTaskId pendingTaskId = taskManager.getPendingTaskIds().get(0);
    
    Assert.assertEquals("nextDeployId", pendingTaskId.getDeployId());
    Assert.assertEquals(requestId, pendingTaskId.getRequestId());
  }
  
  private SingularityPendingTask createAndSchedulePendingTask(String deployId) {
    Random random = new Random();
    
    SingularityPendingTaskId pendingTaskId = new SingularityPendingTaskId(requestId, deployId, 
        System.currentTimeMillis() + TimeUnit.DAYS.toMillis(random.nextInt(3)), random.nextInt(10), PendingType.IMMEDIATE);
    
    SingularityPendingTask pendingTask = new SingularityPendingTask(pendingTaskId, Optional.<String> absent());

    taskManager.createPendingTasks(Arrays.asList(pendingTask));
    
    return pendingTask;
  }
  
  @Test
  public void testCleanerLeavesPausedRequestTasksByDemand() {
    initScheduledRequest();
    initFirstDeploy();
    
    SingularityTask firstTask = launchTask(request, firstDeploy, TaskState.TASK_RUNNING);
    SingularityPendingTask pendingTask = createAndSchedulePendingTask(firstDeployId);
    
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
    
    SingularityTask firstTask = launchTask(request, firstDeploy, TaskState.TASK_RUNNING);
    
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
  
}
