package com.hubspot.singularity;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.Protos.Attribute;
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
import org.apache.mesos.Protos.Value.Text;
import org.apache.mesos.Protos.Value.Type;
import org.apache.mesos.SchedulerDriver;
import org.junit.After;
import org.junit.Before;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.SingularityLoadBalancerUpdate.LoadBalancerMethod;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.api.SingularityDeployRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.zkmigrations.ZkDataMigrationRunner;
import com.hubspot.singularity.mesos.SchedulerDriverSupplier;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;
import com.hubspot.singularity.resources.DeployResource;
import com.hubspot.singularity.resources.RackResource;
import com.hubspot.singularity.resources.RequestResource;
import com.hubspot.singularity.resources.SlaveResource;
import com.hubspot.singularity.scheduler.SingularityCleaner;
import com.hubspot.singularity.scheduler.SingularityCooldownChecker;
import com.hubspot.singularity.scheduler.SingularityDeployChecker;
import com.hubspot.singularity.scheduler.SingularityScheduledJobPoller;
import com.hubspot.singularity.scheduler.SingularityScheduler;
import com.hubspot.singularity.scheduler.SingularitySchedulerPriority;
import com.hubspot.singularity.scheduler.SingularitySchedulerStateCache;
import com.hubspot.singularity.scheduler.SingularityTaskReconciliation;
import com.hubspot.singularity.scheduler.TestingLoadBalancerClient;
import com.hubspot.singularity.smtp.SingularityMailer;
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
  protected SlaveManager slaveManager;
  @Inject
  protected RackManager rackManager;
  @Inject
  protected SchedulerDriverSupplier driverSupplier;
  protected SchedulerDriver driver;
  @Inject
  protected SingularityScheduler scheduler;
  @Inject
  protected SingularityDeployChecker deployChecker;
  @Inject
  protected RackResource rackResource;
  @Inject
  protected SlaveResource slaveResource;
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
  protected SingularityMailer mailer;
  @Inject
  protected SingularityScheduledJobPoller scheduledJobPoller;
  @Inject
  protected ZkDataMigrationRunner migrationRunner;

  @Inject
  @Named(SingularityMainModule.SERVER_ID_PROPERTY)
  protected String serverId;

  protected String requestId = "test-request";
  protected SingularityRequest request;
  protected String schedule = "*/1 * * * * ?";

  protected String firstDeployId = "firstDeployId";
  protected SingularityDeploy firstDeploy;

  protected String secondDeployId = "secondDeployId";
  protected SingularityDeployMarker secondDeployMarker;
  protected SingularityDeploy secondDeploy;

  protected Optional<String> user = Optional.absent();

  @After
  public void teardown() throws Exception {
    if (httpClient != null) {
      httpClient.close();
    }
  }

  @Before
  public final void setupDriver() throws Exception {
    driver = driverSupplier.get().get();

    migrationRunner.checkMigrations();
  }

  protected Offer createOffer(double cpus, double memory) {
    return createOffer(cpus, memory, "slave1", "host1", Optional.<String> absent());
  }

  protected Offer createOffer(double cpus, double memory, String slave, String host) {
    return createOffer(cpus, memory, slave, host, Optional.<String> absent());
  }

  protected Offer createOffer(double cpus, double memory, String slave, String host, Optional<String> rack) {
    SlaveID slaveId = SlaveID.newBuilder().setValue(slave).build();
    FrameworkID frameworkId = FrameworkID.newBuilder().setValue("framework1").build();

    Random r = new Random();

    return Offer.newBuilder()
        .setId(OfferID.newBuilder().setValue("offer" + r.nextInt(1000)).build())
        .setFrameworkId(frameworkId)
        .setSlaveId(slaveId)
        .setHostname(host)
        .addAttributes(Attribute.newBuilder().setType(Type.TEXT).setText(Text.newBuilder().setValue(rack.or(configuration.getMesosConfiguration().getDefaultRackId()))).setName(configuration.getMesosConfiguration().getRackIdAttributeKey()))
        .addResources(Resource.newBuilder().setType(Type.SCALAR).setName(MesosUtils.CPUS).setScalar(Scalar.newBuilder().setValue(cpus)))
        .addResources(Resource.newBuilder().setType(Type.SCALAR).setName(MesosUtils.MEMORY).setScalar(Scalar.newBuilder().setValue(memory)))
        .build();
  }

  protected SingularityTask launchTask(SingularityRequest request, SingularityDeploy deploy, int instanceNo, TaskState initialTaskState) {
    return launchTask(request, deploy, System.currentTimeMillis() - 1, System.currentTimeMillis(), instanceNo, initialTaskState);
  }

  protected SingularityTask launchTask(SingularityRequest request, SingularityDeploy deploy, long taskLaunch, int instanceNo, TaskState initialTaskState) {
    return launchTask(request, deploy, taskLaunch, System.currentTimeMillis(), instanceNo, initialTaskState);
  }

  protected SingularityPendingTask buildPendingTask(SingularityRequest request, SingularityDeploy deploy, long launchTime, int instanceNo) {
    SingularityPendingTaskId pendingTaskId = new SingularityPendingTaskId(request.getId(), deploy.getId(), launchTime, instanceNo, PendingType.IMMEDIATE, launchTime);
    SingularityPendingTask pendingTask = new SingularityPendingTask(pendingTaskId, Collections.<String> emptyList(), Optional.<String> absent());

    return pendingTask;
  }

  protected SingularityTask prepTask(SingularityRequest request, SingularityDeploy deploy, long launchTime, int instanceNo) {
    SingularityPendingTask pendingTask = buildPendingTask(request, deploy, launchTime, instanceNo);
    SingularityTaskRequest taskRequest = new SingularityTaskRequest(request, deploy, pendingTask);

    Offer offer = createOffer(125, 1024);

    SingularityTaskId taskId = new SingularityTaskId(request.getId(), deploy.getId(), launchTime, instanceNo, offer.getHostname(), "rack1");
    TaskID taskIdProto = TaskID.newBuilder().setValue(taskId.toString()).build();

    TaskInfo taskInfo = TaskInfo.newBuilder()
        .setSlaveId(offer.getSlaveId())
        .setTaskId(taskIdProto)
        .setName("name")
        .build();

    SingularityTask task = new SingularityTask(taskRequest, taskId, offer, taskInfo);

    taskManager.savePendingTask(pendingTask);

    return task;
  }

  protected SingularityTask prepTask() {
    return prepTask(request, firstDeploy, System.currentTimeMillis(), 1);
  }

  protected SingularityTask launchTask(SingularityRequest request, SingularityDeploy deploy, long launchTime, long updateTime, int instanceNo, TaskState initialTaskState) {
    SingularityTask task = prepTask(request, deploy, launchTime, instanceNo);

    taskManager.createTaskAndDeletePendingTask(task);

    statusUpdate(task, initialTaskState, Optional.of(updateTime));

    return task;
  }

  protected void statusUpdate(SingularityTask task, TaskState state, Optional<Long> timestamp) {
    TaskStatus.Builder bldr = TaskStatus.newBuilder()
        .setTaskId(task.getMesosTask().getTaskId())
        .setSlaveId(task.getOffer().getSlaveId())
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
    protectedInitRequest(true, false);
  }

  protected void initScheduledRequest() {
    protectedInitRequest(false, true);
  }

  protected void saveRequest(SingularityRequest request) {
    requestManager.activate(request, RequestHistoryType.CREATED, System.currentTimeMillis(), Optional.<String> absent());
  }

  protected void protectedInitRequest(boolean isLoadBalanced, boolean isScheduled) {
    RequestType requestType = RequestType.WORKER;

    if (isScheduled) {
      requestType = RequestType.SCHEDULED;
    }

    SingularityRequestBuilder bldr = new SingularityRequestBuilder(requestId, requestType);

    bldr.setLoadBalanced(Optional.of(isLoadBalanced));

    if (isScheduled) {
      bldr.setQuartzSchedule(Optional.of(schedule));
    }

    request = bldr.build();

    saveRequest(request);
  }

  protected void initRequest() {
    protectedInitRequest(false, false);
  }

  protected void initFirstDeploy() {
    firstDeploy = initDeploy(request, firstDeployId);
  }

  protected SingularityDeploy initDeploy(SingularityRequest request, String deployId) {
    SingularityDeployMarker marker =  new SingularityDeployMarker(request.getId(), deployId, System.currentTimeMillis(), Optional.<String> absent());
    SingularityDeploy deploy = new SingularityDeployBuilder(request.getId(), deployId).setCommand(Optional.of("sleep 100")).build();

    deployManager.saveDeploy(request, marker, deploy);

    finishDeploy(marker, deploy);

    return deploy;
  }

  protected SingularityDeploy initDeploy(SingularityDeployBuilder builder, long timestamp) {
    SingularityDeployMarker marker = new SingularityDeployMarker(requestId, builder.getId(), timestamp, Optional.<String> absent());
    builder.setCommand(Optional.of("sleep 100"));

    SingularityDeploy deploy = builder.build();

    deployManager.saveDeploy(request, marker, deploy);

    startDeploy(marker);

    return deploy;
  }

  protected void initSecondDeploy() {
    secondDeployMarker = new SingularityDeployMarker(requestId, secondDeployId, System.currentTimeMillis(), Optional.<String> absent());
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

  protected void deploy(String deployId) {
    deploy(deployId, Optional.<Boolean> absent());
  }

  protected void deploy(String deployId, Optional<Boolean> unpauseOnDeploy) {
    deployResource.deploy(new SingularityDeployRequest(new SingularityDeployBuilder(requestId, deployId).setCommand(Optional.of("sleep 1")).build(), Optional.<String> absent(), unpauseOnDeploy));
  }

  protected SingularityPendingTask createAndSchedulePendingTask(String deployId) {
    Random random = new Random();

    SingularityPendingTaskId pendingTaskId = new SingularityPendingTaskId(requestId, deployId,
        System.currentTimeMillis() + TimeUnit.DAYS.toMillis(random.nextInt(3)), random.nextInt(10), PendingType.IMMEDIATE, System.currentTimeMillis());

    SingularityPendingTask pendingTask = new SingularityPendingTask(pendingTaskId, Collections.<String> emptyList(), Optional.<String> absent());

    taskManager.savePendingTask(pendingTask);

    return pendingTask;
  }

  protected void saveAndSchedule(SingularityRequestBuilder bldr) {
    requestManager.activate(bldr.build(), RequestHistoryType.UPDATED, System.currentTimeMillis(), Optional.<String> absent());
    requestManager.addToPendingQueue(new SingularityPendingRequest(bldr.getId(), firstDeployId, System.currentTimeMillis(), PendingType.UPDATED_REQUEST));
    scheduler.drainPendingQueue(stateCacheProvider.get());
  }

  protected void saveLoadBalancerState(BaragonRequestState brs, SingularityTaskId taskId, LoadBalancerRequestType lbrt) {
    final LoadBalancerRequestId lbri = new LoadBalancerRequestId(taskId.getId(), lbrt, Optional.<Integer> absent());
    SingularityLoadBalancerUpdate update = new SingularityLoadBalancerUpdate(brs, lbri, Optional.<String> absent(), System.currentTimeMillis(), LoadBalancerMethod.CHECK_STATE, null);

    taskManager.saveLoadBalancerState(taskId, lbrt, update);
  }

  protected void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected void saveLastActiveTaskStatus(SingularityTask task, Optional<TaskStatus> taskStatus, long millisAdjustment) {
    taskManager.saveLastActiveTaskStatus(new SingularityTaskStatusHolder(task.getTaskId(), taskStatus, System.currentTimeMillis() + millisAdjustment, serverId, Optional.of("slaveId")));
  }

  protected TaskStatus buildTaskStatus(SingularityTask task) {
    return TaskStatus.newBuilder().setTaskId(TaskID.newBuilder().setValue(task.getTaskId().getId())).setState(TaskState.TASK_RUNNING).build();
  }

  protected SingularityRequest buildRequest(String requestId) {
    SingularityRequest request = new SingularityRequestBuilder(requestId, RequestType.WORKER).build();

    saveRequest(request);

    return request;
  }

  protected SingularityTaskRequest buildTaskRequest(SingularityRequest request, SingularityDeploy deploy, long launchTime) {
    return new SingularityTaskRequest(request, deploy, buildPendingTask(request, deploy, launchTime, 100));
  }

}
