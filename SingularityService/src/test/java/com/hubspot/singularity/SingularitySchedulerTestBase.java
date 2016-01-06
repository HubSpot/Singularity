package com.hubspot.singularity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.api.SingularityDeployRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.zkmigrations.ZkDataMigrationRunner;
import com.hubspot.singularity.event.SingularityEventListener;
import com.hubspot.singularity.mesos.SchedulerDriverSupplier;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;
import com.hubspot.singularity.resources.DeployResource;
import com.hubspot.singularity.resources.RackResource;
import com.hubspot.singularity.resources.RequestResource;
import com.hubspot.singularity.resources.SlaveResource;
import com.hubspot.singularity.resources.TaskResource;
import com.hubspot.singularity.scheduler.SingularityCleaner;
import com.hubspot.singularity.scheduler.SingularityCooldownChecker;
import com.hubspot.singularity.scheduler.SingularityDeployChecker;
import com.hubspot.singularity.scheduler.SingularityExpiringUserActionPoller;
import com.hubspot.singularity.scheduler.SingularityHealthchecker;
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
  protected TaskResource taskResource;
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
  protected SingularityEventListener eventListener;
  @Inject
  protected SingularityExpiringUserActionPoller expiringUserActionPoller;
  @Inject
  protected SingularityHealthchecker healthchecker;

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

  public SingularitySchedulerTestBase(boolean useDBTests) {
    super(useDBTests);
  }

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
    return createOffer(cpus, memory, slave, host, rack, Collections.<String, String>emptyMap());
  }

  protected Offer createOffer(double cpus, double memory, String slave, String host, Optional<String> rack, Map<String, String> attributes) {
    SlaveID slaveId = SlaveID.newBuilder().setValue(slave).build();
    FrameworkID frameworkId = FrameworkID.newBuilder().setValue("framework1").build();

    Random r = new Random();

    List<Attribute> attributesList = new ArrayList<>();
    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      attributesList.add(Attribute.newBuilder()
          .setType(Type.TEXT)
          .setName(entry.getKey())
          .setText(Text.newBuilder().setValue(entry.getValue()).build())
          .build());
    }

    return Offer.newBuilder()
        .setId(OfferID.newBuilder().setValue("offer" + r.nextInt(1000)).build())
        .setFrameworkId(frameworkId)
        .setSlaveId(slaveId)
        .setHostname(host)
        .addAttributes(Attribute.newBuilder().setType(Type.TEXT).setText(Text.newBuilder().setValue(rack.or(configuration.getMesosConfiguration().getDefaultRackId()))).setName(configuration.getMesosConfiguration().getRackIdAttributeKey()))
        .addResources(Resource.newBuilder().setType(Type.SCALAR).setName(MesosUtils.CPUS).setScalar(Scalar.newBuilder().setValue(cpus)))
        .addResources(Resource.newBuilder().setType(Type.SCALAR).setName(MesosUtils.MEMORY).setScalar(Scalar.newBuilder().setValue(memory)))
        .addAllAttributes(attributesList)
        .build();
  }

  protected SingularityTask launchTask(SingularityRequest request, SingularityDeploy deploy, int instanceNo, TaskState initialTaskState) {
    return launchTask(request, deploy, System.currentTimeMillis() - 1, System.currentTimeMillis(), instanceNo, initialTaskState, false);
  }

  protected SingularityTask launchTask(SingularityRequest request, SingularityDeploy deploy, int instanceNo, TaskState initialTaskState, boolean separateHost) {
    return launchTask(request, deploy, System.currentTimeMillis() - 1, System.currentTimeMillis(), instanceNo, initialTaskState, separateHost);
  }

  protected SingularityTask launchTask(SingularityRequest request, SingularityDeploy deploy, long taskLaunch, int instanceNo, TaskState initialTaskState) {
    return launchTask(request, deploy, taskLaunch, System.currentTimeMillis(), instanceNo, initialTaskState, false);
  }

  protected SingularityTask launchTask(SingularityRequest request, SingularityDeploy deploy, long launchTime, long updateTime, int instanceNo, TaskState initialTaskState) {
    return launchTask(request, deploy, launchTime, updateTime, instanceNo, initialTaskState, false);
  }

  protected SingularityPendingTask buildPendingTask(SingularityRequest request, SingularityDeploy deploy, long launchTime, int instanceNo) {
    SingularityPendingTaskId pendingTaskId = new SingularityPendingTaskId(request.getId(), deploy.getId(), launchTime, instanceNo, PendingType.IMMEDIATE, launchTime);
    SingularityPendingTask pendingTask = new SingularityPendingTask(pendingTaskId, Optional.<List<String>> absent(), Optional.<String> absent(), Optional.<String> absent(), Optional.<Boolean> absent(), Optional.<String> absent());

    return pendingTask;
  }

  protected SingularityTask prepTask(SingularityRequest request, SingularityDeploy deploy, long launchTime, int instanceNo) {
    return prepTask(request, deploy, launchTime, instanceNo, false);
  }


  protected SingularityTask prepTask(SingularityRequest request, SingularityDeploy deploy, long launchTime, int instanceNo, boolean separateHosts) {
    SingularityPendingTask pendingTask = buildPendingTask(request, deploy, launchTime, instanceNo);
    SingularityTaskRequest taskRequest = new SingularityTaskRequest(request, deploy, pendingTask);

    Offer offer;
    if (separateHosts) {
      offer = createOffer(125, 1024, String.format("slave%s", instanceNo), String.format("host%s", instanceNo));
    } else {
      offer = createOffer(125, 1024);
    }

    SingularityTaskId taskId = new SingularityTaskId(request.getId(), deploy.getId(), launchTime, instanceNo, offer.getHostname(), "rack1");
    TaskID taskIdProto = TaskID.newBuilder().setValue(taskId.toString()).build();

    TaskInfo taskInfo = TaskInfo.newBuilder()
        .setSlaveId(offer.getSlaveId())
        .setTaskId(taskIdProto)
        .setName("name")
        .build();

    SingularityTask task = new SingularityTask(taskRequest, taskId, offer, taskInfo, Optional.of("rack1"));

    taskManager.savePendingTask(pendingTask);

    return task;
  }

  protected SingularityTask prepTask() {
    return prepTask(request, firstDeploy, System.currentTimeMillis(), 1, false);
  }

  protected SingularityTask launchTask(SingularityRequest request, SingularityDeploy deploy, long launchTime, long updateTime, int instanceNo, TaskState initialTaskState, boolean separateHost) {
    SingularityTask task = prepTask(request, deploy, launchTime, instanceNo, separateHost);

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
    statusUpdate(task, state, Optional.<Long>absent());
  }

  protected void runLaunchedTasks() {
    for (SingularityTaskId taskId : taskManager.getActiveTaskIds()) {
      Collection<SingularityTaskHistoryUpdate> updates = taskManager.getTaskHistoryUpdates(taskId);

      SimplifiedTaskState currentState = SingularityTaskHistoryUpdate.getCurrentState(updates);

      switch (currentState) {
        case UNKNOWN:
        case WAITING:
          statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_RUNNING);
          break;
        case DONE:
        case RUNNING:
          break;
      }
    }
  }

  protected void killKilledTasks() {
    for (SingularityKilledTaskIdRecord killed : taskManager.getKilledTaskIdRecords()) {
      statusUpdate(taskManager.getTask(killed.getTaskId()).get(), TaskState.TASK_KILLED);
    }
  }

  protected void initLoadBalancedRequest() {
    protectedInitRequest(true, false);
  }

  protected void initScheduledRequest() {
    protectedInitRequest(false, true);
  }

  protected void saveRequest(SingularityRequest request) {
    requestManager.activate(request, RequestHistoryType.CREATED, System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent());
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

  protected void initWithTasks(int num) {
    initRequest();

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(num), Optional.<Long> absent(), Optional.<Boolean> absent(), Optional.<String> absent(), Optional.<String>absent()));

    initFirstDeploy();

    startTasks(num);
  }

  protected void initFirstDeploy() {
    firstDeploy = initAndFinishDeploy(request, firstDeployId);
  }

  protected void initHCDeploy() {
    firstDeploy = initAndFinishDeploy(request, new SingularityDeployBuilder(request.getId(), firstDeployId).setCommand(Optional.of("sleep 100")).setHealthcheckUri(Optional.of("http://uri")));
  }

  protected SingularityDeploy initAndFinishDeploy(SingularityRequest request, String deployId) {
    return initAndFinishDeploy(request, new SingularityDeployBuilder(request.getId(), deployId).setCommand(Optional.of("sleep 100")));
  }

  protected SingularityDeploy initAndFinishDeploy(SingularityRequest request, SingularityDeployBuilder builder) {
    SingularityDeploy deploy = builder.build();

    SingularityDeployMarker marker = new SingularityDeployMarker(deploy.getRequestId(), deploy.getId(), System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent());

    deployManager.saveDeploy(request, marker, deploy);

    finishDeploy(marker, deploy);

    return deploy;
  }

  protected SingularityDeploy initDeploy(SingularityDeployBuilder builder, long timestamp) {
    SingularityDeployMarker marker = new SingularityDeployMarker(requestId, builder.getId(), timestamp, Optional.<String> absent(), Optional.<String> absent());
    builder.setCommand(Optional.of("sleep 100"));

    SingularityDeploy deploy = builder.build();

    deployManager.saveDeploy(request, marker, deploy);

    startDeploy(marker);

    return deploy;
  }

  protected void initSecondDeploy() {
    secondDeployMarker = new SingularityDeployMarker(requestId, secondDeployId, System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent());
    secondDeploy = new SingularityDeployBuilder(requestId, secondDeployId).setCommand(Optional.of("sleep 100")).build();

    deployManager.saveDeploy(request, secondDeployMarker, secondDeploy);

    startDeploy(secondDeployMarker);
  }

  protected void startDeploy(SingularityDeployMarker deployMarker) {
    deployManager.savePendingDeploy(new SingularityPendingDeploy(deployMarker, Optional.<SingularityLoadBalancerUpdate>absent(), DeployState.WAITING));
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

  protected void startTasks(int num) {
    for (int i = 1; i < num + 1; i++) {
      startTask(firstDeploy, i);
    }
  }

  protected SingularityTask startSeparatePlacementTask(SingularityDeploy deploy, int instanceNo) {
    return launchTask(request, deploy, instanceNo, TaskState.TASK_RUNNING, true);
  }

  protected void resourceOffers() {
    sms.resourceOffers(driver, Arrays.asList(createOffer(20, 20000, "slave1", "host1"), createOffer(20, 20000, "slave2", "host2")));
  }

  protected void resourceOffersByNumTasks(int numTasks) {
    List<Offer> offers = new ArrayList<>();
    for (int i = 1; i <= numTasks; i++) {
      offers.add(createOffer(1, 128, String.format("slave%s", i), String.format("host%s", i)));
    }
    sms.resourceOffers(driver, offers);
  }

  protected void resourceOffers(int numSlaves) {
    List<Offer> offers = new ArrayList<>();
    for (int i = 1; i <= numSlaves; i++) {
      offers.add(createOffer(20, 20000, String.format("slave%s", i), String.format("host%s", i)));
    }
    sms.resourceOffers(driver, offers);
  }

  protected void deploy(String deployId) {
    deploy(deployId, Optional.<Boolean> absent());
  }

  protected void deploy(String deployId, Optional<Boolean> unpauseOnDeploy) {
    deployResource.deploy(new SingularityDeployRequest(new SingularityDeployBuilder(requestId, deployId).setCommand(Optional.of("sleep 1")).build(), unpauseOnDeploy, Optional.<String> absent()));
  }

  protected SingularityPendingTask createAndSchedulePendingTask(String deployId) {
    Random random = new Random();

    SingularityPendingTaskId pendingTaskId = new SingularityPendingTaskId(requestId, deployId,
        System.currentTimeMillis() + TimeUnit.DAYS.toMillis(random.nextInt(3)), random.nextInt(10), PendingType.IMMEDIATE, System.currentTimeMillis());

    SingularityPendingTask pendingTask = new SingularityPendingTask(pendingTaskId, Optional.<List<String>> absent(), Optional.<String> absent(), Optional.<String> absent(), Optional.<Boolean> absent(), Optional.<String> absent());

    taskManager.savePendingTask(pendingTask);

    return pendingTask;
  }

  protected void saveAndSchedule(SingularityRequestBuilder bldr) {
    requestManager.activate(bldr.build(), RequestHistoryType.UPDATED, System.currentTimeMillis(), Optional.<String> absent(), Optional.<String> absent());
    requestManager.addToPendingQueue(new SingularityPendingRequest(bldr.getId(), firstDeployId, System.currentTimeMillis(), Optional.<String> absent(), PendingType.UPDATED_REQUEST, Optional.<Boolean> absent(), Optional.<String> absent()));
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
