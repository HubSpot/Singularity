package com.hubspot.singularity;

import java.util.Arrays;
import java.util.Random;

import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.SlaveID;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.TaskStatus;
import org.apache.mesos.Protos.Value.Scalar;
import org.apache.mesos.Protos.Value.Type;
import org.apache.mesos.SchedulerDriver;
import org.junit.After;
import org.junit.Before;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SchedulerDriverSupplier;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;
import com.hubspot.singularity.resources.DeployResource;
import com.hubspot.singularity.resources.RequestResource;
import com.hubspot.singularity.scheduler.SingularityCleaner;
import com.hubspot.singularity.scheduler.SingularityCooldownChecker;
import com.hubspot.singularity.scheduler.SingularityDeployChecker;
import com.hubspot.singularity.scheduler.SingularityScheduler;
import com.hubspot.singularity.scheduler.SingularitySchedulerPriority;
import com.hubspot.singularity.scheduler.SingularitySchedulerStateCache;
import com.hubspot.singularity.scheduler.SingularityTaskReconciliation;
import com.hubspot.singularity.scheduler.TestingLoadBalancerClient;
import com.ning.http.client.AsyncHttpClient;

public class SingularitySchedulerTestBase extends SingularityCuratorTestBase {

  @Inject
  protected Provider<SingularitySchedulerStateCache> stateCacheProvider;
  @Inject
  protected SingularityMesosScheduler sms;
  @Inject
  protected RequestManager requestManager;
  @Inject
  protected DeployManager deployManager;
  @Inject
  protected TaskManager taskManager;
  @Inject
  protected SchedulerDriverSupplier driverSupplier;
  protected SchedulerDriver driver;
  @Inject
  protected SingularityScheduler scheduler;
  @Inject
  protected SingularityDeployChecker deployChecker;
  @Inject
  protected RequestResource requestResource;
  @Inject
  protected DeployResource deployResource;
  @Inject
  protected SingularityCleaner cleaner;
  @Inject
  protected SingularityConfiguration configuration;
  @Inject
  protected SingularityCooldownChecker cooldownChecker;
  @Inject
  protected AsyncHttpClient httpClient;
  @Inject
  protected TestingLoadBalancerClient testingLbClient;
  @Inject
  protected SingularitySchedulerPriority schedulerPriority;
  @Inject
  protected SingularityTaskReconciliation taskReconciliation;
  @Inject
  @Named(SingularityMainModule.SERVER_ID_PROPERTY)
  protected String serverId;

  protected String requestId;
  protected SingularityRequest request;
  protected String schedule = "*/1 * * * * ?";

  protected String firstDeployId;
  protected SingularityDeploy firstDeploy;

  protected String secondDeployId;
  protected SingularityDeployMarker secondDeployMarker;
  protected SingularityDeploy secondDeploy;

  @After
  public void teardown() throws Exception {
    if (httpClient != null) {
      httpClient.close();
    }
  }

  @Before
  public final void setupDriver() throws Exception {
    driver = driverSupplier.get().get();
  }

  protected Offer createOffer(double cpus, double memory) {
    return createOffer(cpus, memory, "slave1", "host1");
  }

  protected Offer createOffer(double cpus, double memory, String slave, String host) {
    SlaveID slaveId = SlaveID.newBuilder().setValue(slave).build();
    FrameworkID frameworkId = FrameworkID.newBuilder().setValue("framework1").build();

    Random r = new Random();

    return Offer.newBuilder()
        .setId(OfferID.newBuilder().setValue("offer" + r.nextInt(1000)).build())
        .setFrameworkId(frameworkId)
        .setSlaveId(slaveId)
        .setHostname(host)
        .addResources(Resource.newBuilder().setType(Type.SCALAR).setName(MesosUtils.CPUS).setScalar(Scalar.newBuilder().setValue(cpus)))
        .addResources(Resource.newBuilder().setType(Type.SCALAR).setName(MesosUtils.MEMORY).setScalar(Scalar.newBuilder().setValue(memory)))
        .build();
  }

  protected SingularityTask launchTask(SingularityRequest request, SingularityDeploy deploy, int instanceNo, TaskState initialTaskState) {
    return launchTask(request, deploy, System.currentTimeMillis(), instanceNo, initialTaskState);
  }

  protected SingularityPendingTask buildPendingTask(SingularityRequest request, SingularityDeploy deploy, long launchTime, int instanceNo) {
    SingularityPendingTaskId pendingTaskId = new SingularityPendingTaskId(request.getId(), deploy.getId(), launchTime, instanceNo, PendingType.IMMEDIATE);
    SingularityPendingTask pendingTask = new SingularityPendingTask(pendingTaskId, Optional.<String> absent());

    return pendingTask;
  }

  protected SingularityTask prepTask(SingularityRequest request, SingularityDeploy deploy, long launchTime, int instanceNo) {
    SingularityTaskId taskId = new SingularityTaskId(request.getId(), deploy.getId(), launchTime, instanceNo, "host", "rack");
    SingularityPendingTask pendingTask = buildPendingTask(request, deploy, launchTime, instanceNo);
    SingularityTaskRequest taskRequest = new SingularityTaskRequest(request, deploy, pendingTask);

    TaskID taskIdProto = TaskID.newBuilder().setValue(taskId.toString()).build();

    Offer offer = createOffer(125, 1024);
    TaskInfo taskInfo = TaskInfo.newBuilder()
        .setSlaveId(offer.getSlaveId())
        .setTaskId(taskIdProto)
        .setName("name")
        .build();

    SingularityTask task = new SingularityTask(taskRequest, taskId, offer, taskInfo);

    taskManager.createPendingTasks(Arrays.asList(pendingTask));

    return task;
  }

  protected SingularityTask prepTask() {
    return prepTask(request, firstDeploy, System.currentTimeMillis(), 1);
  }

  protected SingularityTask launchTask(SingularityRequest request, SingularityDeploy deploy, long launchTime, int instanceNo, TaskState initialTaskState) {
    SingularityTask task = prepTask(request, deploy, launchTime, instanceNo);

    taskManager.createTaskAndDeletePendingTask(task);

    statusUpdate(task, initialTaskState);

    return task;
  }

  protected void statusUpdate(SingularityTask task, TaskState state, Optional<Long> timestamp) {
    TaskStatus.Builder bldr = TaskStatus.newBuilder()
        .setTaskId(task.getMesosTask().getTaskId())
        .setState(state);

    if (timestamp.isPresent()) {
      bldr.setTimestamp(timestamp.get() / 1000);
    }

    sms.statusUpdate(driver, bldr.build());
  }

  protected void statusUpdate(SingularityTask task, TaskState state) {
    statusUpdate(task, state, Optional.<Long> absent());
  }

  protected void initLoadBalancedRequest() {
    privateInitRequest(true, false);
  }

  protected void initScheduledRequest() {
    privateInitRequest(false, true);
  }

  protected void saveRequest(SingularityRequest request) {
    requestManager.activate(request, RequestHistoryType.CREATED, Optional.<String> absent());
  }

  protected void privateInitRequest(boolean isLoadBalanced, boolean isScheduled) {
    requestId = "test-request";

    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId)
    .setLoadBalanced(Optional.of(isLoadBalanced));

    if (isScheduled) {
      bldr.setQuartzSchedule(Optional.of(schedule));
    }

    request = bldr.build();

    saveRequest(request);
  }

  protected void initRequest() {
    privateInitRequest(false, false);
  }

  protected void initFirstDeploy() {
    firstDeployId = "firstDeployId";

    firstDeploy = initDeploy(request, firstDeployId);
  }

  protected SingularityDeploy initDeploy(SingularityRequest request, String deployId) {
    SingularityDeployMarker marker =  new SingularityDeployMarker(request.getId(), deployId, System.currentTimeMillis(), Optional.<String> absent());
    SingularityDeploy deploy = new SingularityDeployBuilder(request.getId(), deployId).setCommand(Optional.of("sleep 100")).build();

    deployManager.saveDeploy(request, marker, deploy);

    finishDeploy(marker, deploy);

    return deploy;
  }

  protected void initSecondDeploy() {
    secondDeployId = "secondDeployId";
    secondDeployMarker =  new SingularityDeployMarker(requestId, secondDeployId, System.currentTimeMillis(), Optional.<String> absent());
    secondDeploy = new SingularityDeployBuilder(requestId, secondDeployId).setCommand(Optional.of("sleep 100")).build();

    deployManager.saveDeploy(request, secondDeployMarker, secondDeploy);

    startDeploy(secondDeployMarker);
  }

  protected void startDeploy(SingularityDeployMarker deployMarker) {
    deployManager.savePendingDeploy(new SingularityPendingDeploy(deployMarker, Optional.<SingularityLoadBalancerUpdate> absent(), DeployState.WAITING));
  }

  protected void finishDeploy(SingularityDeployMarker marker, SingularityDeploy deploy) {
    deployManager.saveDeployResult(marker, Optional.of(deploy), new SingularityDeployResult(DeployState.SUCCEEDED));

    deployManager.saveNewRequestDeployState(new SingularityRequestDeployState(requestId, Optional.of(marker), Optional.<SingularityDeployMarker> absent()));
  }

  protected SingularityTask startTask(SingularityDeploy deploy) {
    return startTask(deploy, 1);
  }

  protected SingularityTask startTask(SingularityDeploy deploy, int instanceNo) {
    return launchTask(request, deploy, instanceNo, TaskState.TASK_RUNNING);
  }

  protected void resourceOffers() {
    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1"), createOffer(20, 20000, "slave2", "host2")));
  }

}
