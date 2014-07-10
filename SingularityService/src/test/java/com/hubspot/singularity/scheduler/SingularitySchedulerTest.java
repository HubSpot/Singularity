package com.hubspot.singularity.scheduler;

import java.util.Arrays;
import java.util.List;

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
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;

public class SingularitySchedulerTest {

  
  @Inject
  private SingularitySchedulerStateCache stateCache;
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
  
  public void initRequest() {
    requestId = "test-request";
    
    request = new SingularityRequestBuilder(requestId)
      .setLoadBalanced(Optional.of(Boolean.TRUE))
      .build();
  
    requestManager.saveRequest(request);
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
    
    scheduler.drainPendingQueue(stateCache);
    
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
    initRequest();
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
  
  
}
