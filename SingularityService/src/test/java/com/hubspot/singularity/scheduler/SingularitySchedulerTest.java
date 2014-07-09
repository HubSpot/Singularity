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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
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
  
  private SingularityTask launchTask(SingularityRequest request, SingularityDeploy deploy) {
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
    
    taskManager.persistScheduleTasks(Arrays.asList(pendingTask));
    taskManager.launchTask(task);
    
    sms.statusUpdate(driver, TaskStatus.newBuilder()
        .setTaskId(taskIdProto)
        .setState(TaskState.TASK_RUNNING)
        .build());
    
    return task;
  }
  
  @Test
  public void testScheduler() {
    
    final String testRid = "test-deploy-erasure";
    final SingularityRequest request = new SingularityRequestBuilder(testRid).build();
    final String activeDid = "active";
    final SingularityDeployMarker activeDeployMarker = new SingularityDeployMarker(testRid, activeDid, System.currentTimeMillis(), Optional.<String> absent());
    final SingularityDeploy activeDeploy = new SingularityDeployBuilder(testRid, activeDid).setCommand(Optional.of("sleep 100")).build();
    
    requestManager.saveRequest(request);
    deployManager.persistDeploy(request, activeDeployMarker, activeDeploy);
    deployManager.saveDeployResult(activeDeployMarker, new SingularityDeployResult(DeployState.SUCCEEDED));
  
//    SingularityTask existingTask = launchTask(request, activeDeploy);
  
    final String pendingDid = "pending";
//    final SingularityDeployMarker pendingDeployMarker = new SingularityDeployMarker(testRid, pendingDid, System.currentTimeMillis(), Optional.<String> absent());
//    final SingularityDeploy pendingDeploy = new SingularityDeployBuilder(testRid, pendingDid).setCommand(Optional.of("sleep 100")).build();
//    final SingularityPendingDeploy pendingDeployObj = new SingularityPendingDeploy(pendingDeployMarker, Optional.<SingularityLoadBalancerUpdate> absent(), DeployState.WAITING);
//    
//    deployManager.createPendingDeploy(pendingDeployObj);
//    deployManager.persistDeploy(request, pendingDeployMarker, pendingDeploy);
//    
  
    SingularityPendingTask p1 = new SingularityPendingTask(new SingularityPendingTaskId(testRid, activeDid, System.currentTimeMillis(), 1, PendingType.ONEOFF), Optional.<String> absent());
    SingularityPendingTask p2 = new SingularityPendingTask(new SingularityPendingTaskId(testRid, activeDid, System.currentTimeMillis(), 1, PendingType.TASK_DONE), Optional.<String> absent());
    SingularityPendingTask p3 = new SingularityPendingTask(new SingularityPendingTaskId(testRid, pendingDid, System.currentTimeMillis(), 1, PendingType.TASK_DONE), Optional.<String> absent());
    
    List<SingularityPendingTask> pendingTasks = Arrays.asList(p1, p2, p3);
    
    taskManager.persistScheduleTasks(pendingTasks);
    
    requestManager.addToPendingQueue(new SingularityPendingRequest(testRid, pendingDid, PendingType.NEW_DEPLOY));
    
    scheduler.drainPendingQueue(stateCache);
    
    // we expect there to be 3 pending tasks :
    
    List<SingularityPendingTask> returnedScheduledTasks = taskManager.getScheduledTasks();
    
    Assert.assertEquals(3, returnedScheduledTasks.size());
    Assert.assertTrue(returnedScheduledTasks.contains(p1));
    Assert.assertTrue(returnedScheduledTasks.contains(p2));
    Assert.assertTrue(!returnedScheduledTasks.contains(p3));
    
    boolean found = false;
    
    for (SingularityPendingTask pendingTask : returnedScheduledTasks) {
      if (pendingTask.getPendingTaskId().getDeployId().equals(pendingDid)) {
        found = true;
        Assert.assertEquals(PendingType.NEW_DEPLOY, pendingTask.getPendingTaskId().getPendingType());
      }
    }
    
    Assert.assertTrue(found);
  }
  
  
}
