package com.hubspot.singularity.mesos;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.v1.Protos.Offer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.singularity.api.auth.SingularityUser;
import com.hubspot.singularity.api.deploy.SingularityDeploy;
import com.hubspot.singularity.api.deploy.SingularityDeployStatistics;
import com.hubspot.singularity.api.deploy.SingularityDeployStatisticsBuilder;
import com.hubspot.singularity.api.machines.MachineLoadMetric;
import com.hubspot.singularity.api.machines.SingularitySlaveUsage;
import com.hubspot.singularity.api.machines.SingularitySlaveUsage.ResourceUsageType;
import com.hubspot.singularity.api.machines.SingularityUsageScoringStrategy;
import com.hubspot.singularity.api.request.RequestType;
import com.hubspot.singularity.api.request.SingularityRequest;
import com.hubspot.singularity.api.request.SingularityScaleRequest;
import com.hubspot.singularity.api.task.SingularityPendingTask;
import com.hubspot.singularity.api.task.SingularityPendingTaskId;
import com.hubspot.singularity.api.task.SingularityTaskId;
import com.hubspot.singularity.api.task.SingularityTaskRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.UsageManager;
import com.hubspot.singularity.scheduler.SingularitySchedulerTestBase;
import com.hubspot.singularity.scheduler.SingularityUsagePoller;
import com.hubspot.singularity.scheduler.TestingMesosClient;

public class SingularityMesosOfferSchedulerTest extends SingularitySchedulerTestBase {

  @Inject
  protected SingularityMesosOfferScheduler scheduler;

  @Inject
  protected DeployManager deployManager;

  @Inject
  protected SingularityConfiguration configuration;

  @Inject
  protected SingularityMesosOfferScheduler offerScheduler;

  @Inject
  protected SingularitySlaveAndRackManager slaveAndRackManager;

  @Inject
  protected UsageManager usageManager;

  @Inject
  protected TestingMesosClient mesosClient;

  @Inject
  protected SingularityUsagePoller usagePoller;

  private static final String SLAVE_ID = "slave";

  private SingularityTaskRequest taskRequest = Mockito.mock(SingularityTaskRequest.class);
  private SingularityDeploy deploy = Mockito.mock(SingularityDeploy.class);
  private SingularityRequest request = Mockito.mock(SingularityRequest.class);
  private SingularityPendingTask task = Mockito.mock(SingularityPendingTask.class);
  private SingularityPendingTaskId taskId = Mockito.mock(SingularityPendingTaskId.class);


  public SingularityMesosOfferSchedulerTest() {
    super(false, (configuration) -> {configuration.setLongRunningUsedCpuWeightForOffer(0.30);
      configuration.setLongRunningUsedMemWeightForOffer(0.50);
      configuration.setLongRunningUsedDiskWeightForOffer(0.20);
      configuration.setFreeCpuWeightForOffer(0.30);
      configuration.setFreeMemWeightForOffer(0.50);
      configuration.setFreeDiskWeightForOffer(0.20);
      configuration.setDefaultOfferScoreForMissingUsage(0.10);
      configuration.setConsiderNonLongRunningTaskLongRunningAfterRunningForSeconds(TimeUnit.HOURS.toSeconds(6));
      configuration.setMaxNonLongRunningUsedResourceWeight(0.50);
      return null;
    });
  }

  @Before
  public void setup() {
    Mockito.when(taskRequest.getRequest()).thenReturn(request);
    Mockito.when(request.getId()).thenReturn("requestId");

    Mockito.when(taskRequest.getDeploy()).thenReturn(deploy);
    Mockito.when(deploy.getId()).thenReturn("deployId");

    Mockito.when(taskRequest.getPendingTask()).thenReturn(task);
    Mockito.when(task.getPendingTaskId()).thenReturn(taskId);
  }

  @Test
  public void itCorrectlyUsesDefaults() {
    Map<ResourceUsageType, Number> longRunningTasksUsage = new HashMap<>();
    setRequestType(RequestType.SERVICE);

    // LR - no usage tracked -> default score
    assertValueIs(0.10, scheduler.score(SLAVE_ID, taskRequest, Optional.empty()));

    // NLR - no deployStatistics -> default weights
    setRequestType(RequestType.ON_DEMAND);
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 5);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(5));
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, mbToBytes(5));
    assertValueIs(0.25, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(5, 10, 5, 10, 5, 10, longRunningTasksUsage))));
  }

  @Test
  public void itCorrectlyScoresLongRunningTasks() {
    Map<ResourceUsageType, Number> longRunningTasksUsage = new HashMap<>();
    setRequestType(RequestType.SERVICE);

    // new slave (no resources used) -> perfect score
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    assertValueIs(1, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0,10, 0,10, 0, 10, longRunningTasksUsage))));

    // cpu used, no mem used, no disk used
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 5);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    assertValueIs(0.85, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 5, 10, 0, 10, longRunningTasksUsage))));

    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 8);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    assertValueIs(0.76, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 8, 10, 0, 10, longRunningTasksUsage))));

    // no cpu used, mem used, no disk used
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(5));
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    assertValueIs(0.75, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(5, 10, 0, 10, 0, 10, longRunningTasksUsage))));

    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(8));
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    assertValueIs(0.60, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(8, 10, 0, 10, 0, 10, longRunningTasksUsage))));

    // no cpu used, no mem used, disk used
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, mbToBytes(5));
    assertValueIs(0.90, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 0, 10, 5, 10, longRunningTasksUsage))));

    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, mbToBytes(8));
    assertValueIs(0.84, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 0, 10, 8, 10, longRunningTasksUsage))));

    // cpu used, mem used, no disk used
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 5);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(5));
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    assertValueIs(0.60, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(5, 10, 5, 10, 0, 10, longRunningTasksUsage))));

    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 8);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(8));
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    assertValueIs(0.36, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(8, 10, 8, 10, 0, 10, longRunningTasksUsage))));

    // no cpu used, mem used, disk used
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(5));
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, mbToBytes(5));
    assertValueIs(0.65, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(5,10, 0,10, 5, 10, longRunningTasksUsage))));

    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(8));
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, mbToBytes(8));
    assertValueIs(0.44, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(8,10, 0,10, 8, 10, longRunningTasksUsage))));

    // cpu used, no mem used, disk used
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 5);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, mbToBytes(5));
    assertValueIs(0.75, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0,10, 5,10, 5, 10, longRunningTasksUsage))));

    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 8);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, mbToBytes(8));
    assertValueIs(0.60, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0,10, 8,10, 8, 10, longRunningTasksUsage))));

    // cpu used, mem used, disk used
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 5);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(5));
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, mbToBytes(5));
    assertValueIs(0.5, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(5,10, 5,10, 5, 10, longRunningTasksUsage))));

    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 8);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(8));
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, mbToBytes(8));
    assertValueIs(0.2, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(8,10, 8,10, 8, 10, longRunningTasksUsage))));
  }

  @Test
  public void itCorrectlyScoresMediumLongNonLongRunningTasks() {
    Map<ResourceUsageType, Number> longRunningTasksUsage = new HashMap<>();
    setRequestType(RequestType.ON_DEMAND);

    // medium duration
    setDeployStatistics(TimeUnit.HOURS, 3);

    // 50% LR cpu -- 0% LR mem -- 0% LR disk
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 5);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    assertValueIs(0.6, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 5, 10, 0, 10, longRunningTasksUsage))));

    // 0% LR cpu -- 0% LR mem -- 50% LR disk
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, mbToBytes(5));
    assertValueIs(0.65, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 0, 10, 5, 10, longRunningTasksUsage))));

    // 20% NLR cpu -- 20% NLR mem -- 0% LR disk
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    double nlrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(2, 10, 2, 10, 0, 10, longRunningTasksUsage)));
    assertValueIs(0.63, nlrScore);

    // 20% LR cpu -- 20% LR mem -- 0% LR disk
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 2);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(2));
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    double lrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(2, 10,2, 10, 0, 10, longRunningTasksUsage)));
    assertValueIs(0.59, lrScore);

    Assert.assertTrue(nlrScore > lrScore);

    // 30% NLR cpu -- 30% NLR mem -- 0% LR disk
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    nlrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(3, 10, 3, 10, 0, 10, longRunningTasksUsage)));
    assertValueIs(0.57, nlrScore);

    Assert.assertTrue(lrScore > nlrScore);
  }

  @Test
  public void itCorrectlyScoresLongNonLongRunningTasks() {
    Map<ResourceUsageType, Number> longRunningTasksUsage = new HashMap<>();
    setRequestType(RequestType.ON_DEMAND);

    // long duration
    setDeployStatistics(TimeUnit.HOURS, 6);

    // 50% LR cpu -- 0% LR mem -- 0% LR disk
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 5);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    assertValueIs(0.85, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 5, 10, 0, 10, longRunningTasksUsage))));

    // 0% LR cpu -- 0% LR mem -- 50% LR disk
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, mbToBytes(5));
    assertValueIs(0.9, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 0, 10, 5, 10, longRunningTasksUsage))));

    // 20% NLR cpu -- 20% NLR mem -- 0% LR disk
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    double nlrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(2, 10, 2, 10, 0, 10, longRunningTasksUsage)));
    assertValueIs(0.92, nlrScore);

    // 20% LR cpu -- 20% LR mem -- 0% LR disk
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 2);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(2));
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    double lrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(2, 10, 2, 10, 0, 10, longRunningTasksUsage)));
    assertValueIs(0.84, lrScore);

    Assert.assertTrue(nlrScore > lrScore);

    // 30% NLR cpu -- 30% NLR mem -- 0% LR disk
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    nlrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(3, 10, 3, 10, 0, 10, longRunningTasksUsage)));
    assertValueIs(0.88, nlrScore);

    Assert.assertTrue(nlrScore > lrScore);

    // over max duration
    setDeployStatistics(TimeUnit.HOURS, 12);

    // 50% LR cpu -- 0% LR mem -- 0% LR disk
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 5);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    assertValueIs(0.85, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 5, 10, 0, 10, longRunningTasksUsage))));

    // 0% LR cpu -- 0% LR mem -- 50% LR disk
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, mbToBytes(5));
    assertValueIs(0.9, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 0, 10, 5, 10, longRunningTasksUsage))));

    // 20% NLR cpu -- 20% NLR mem -- 0% LR disk
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    nlrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(2, 10, 2, 10, 0, 10, longRunningTasksUsage)));
    assertValueIs(0.92, nlrScore);

    // 20% LR cpu -- 20% LR mem -- 0% LR disk
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 2);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(2));
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    lrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(2, 10, 2, 10, 0, 10, longRunningTasksUsage)));
    assertValueIs(0.84, lrScore);

    Assert.assertTrue(nlrScore > lrScore);

    // 30% NLR cpu -- 30% NLR mem -- 0% LR disk
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0);
    nlrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(3, 10, 3, 10, 0, 10, longRunningTasksUsage)));
    assertValueIs(0.88, nlrScore);

    Assert.assertTrue(nlrScore > lrScore);
  }

  @Test
  public void itAccountsForExpectedTaskUsage() {
    initRequest();
    double cpuReserved = 2;
    double memMbReserved = 1000;
    initFirstDeployWithResources(cpuReserved, memMbReserved);
    saveAndSchedule(requestManager.getRequest(requestId).get().getRequest().toBuilder().setInstances(Optional.of(1)));
    resourceOffers(3);

    SingularityTaskId taskId = taskManager.getActiveTaskIds().get(0);
    String t1 = taskId.getId();

    // 2 cpus used
    MesosTaskMonitorObject t1u1 = getTaskMonitor(t1, 10, TimeUnit.MILLISECONDS.toSeconds(taskId.getStartedAt()) + 5, 1000);
    mesosClient.setSlaveResourceUsage("host1", Collections.singletonList(t1u1));
    usagePoller.runActionOnPoll();

    Map<ResourceUsageType, Number> longRunningTasksUsage = new HashMap<>();
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0.1);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0.1);
    longRunningTasksUsage.put(ResourceUsageType.DISK_BYTES_USED, 0.1);
    SingularitySlaveUsage smallUsage = new SingularitySlaveUsage(0.1, 0.1, Optional.of(10.0), 1, 1, Optional.of(30L), 1, 1, Optional.of(1024L), longRunningTasksUsage, 1, System.currentTimeMillis(), 1, 30000, 10, 0, 0, 0, 0, 107374182);

    usageManager.saveSpecificSlaveUsageAndSetCurrent("host1", smallUsage);
    usageManager.saveSpecificSlaveUsageAndSetCurrent("host2", smallUsage);
    usageManager.saveSpecificSlaveUsageAndSetCurrent("host3", smallUsage);

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(3), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()), SingularityUser.DEFAULT_USER);

    Assert.assertEquals(2.0, usageManager.getRequestUtilizations().get(requestId).getCpuUsed(), 0.001);

    Offer host2Offer = createOffer(6, 30000, 107374182, "host2", "host2");
    slaveAndRackManager.checkOffer(host2Offer);
    Offer host3Offer = createOffer(6, 30000, 107374182, "host3", "host3");
    slaveAndRackManager.checkOffer(host3Offer);

    Collection<SingularityOfferHolder> offerHolders = offerScheduler.checkOffers(Arrays.asList(host2Offer, host3Offer));
    Assert.assertEquals(2, offerHolders.size());

    // A single offer should only ever get a single task even though both have room for both tasks here. Adding a task should reduce the score for the next check
    for (SingularityOfferHolder offerHolder : offerHolders) {
      Assert.assertEquals(1, offerHolder.getAcceptedTasks().size());
    }
  }

  private void assertValueIs(double expectedValue, double actualValue) {
    actualValue = Math.round(actualValue * 1000.0) / 1000.0;
    Assert.assertTrue(String.format("Expected %f but found %f", expectedValue, actualValue),  actualValue == expectedValue);
  }

  private long mbToBytes(long memMb) {
    return memMb * 1000L * 1000L;
  }

  private SingularitySlaveUsageWithCalculatedScores getUsage(long memMbReserved, long memMbTotal, double cpusReserved, double cpusTotal, long diskMbReserved, long diskMbTotal, Map<ResourceUsageType, Number> longRunningTasksUsage) {
    return new SingularitySlaveUsageWithCalculatedScores(
        new SingularitySlaveUsage(0, cpusReserved, Optional.of(cpusTotal), 0, memMbReserved, Optional.of(memMbTotal), 0, diskMbReserved, Optional.of(diskMbTotal), longRunningTasksUsage, 1, 0L,
            0, 0, 0, 0, 0, 0, 0 , 0),
        SingularityUsageScoringStrategy.SPREAD_TASK_USAGE, MachineLoadMetric.LOAD_5
    );
  }

  private void setDeployStatistics(TimeUnit unit, long time) {
    deployManager.saveDeployStatistics(getDeployStatistics(unit.toMillis(time)));
  }

  private SingularityDeployStatistics getDeployStatistics(long avgRunTimeMillis) {
    return new SingularityDeployStatisticsBuilder("requestId", "deployId")
        .setAverageRuntimeMillis(Optional.of(avgRunTimeMillis))
        .build();
  }

  private void setRequestType(RequestType type) {
    Mockito.when(request.getRequestType()).thenReturn(type);
  }
}
