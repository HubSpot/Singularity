package com.hubspot.singularity.mesos;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityCuratorTestBase;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityDeployStatisticsBuilder;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsage.ResourceUsageType;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;

public class SingularityMesosOfferSchedulerTest extends SingularityCuratorTestBase {

  @Inject
  protected SingularityMesosOfferScheduler scheduler;

  @Inject
  protected DeployManager deployManager;

  @Inject
  protected SingularityConfiguration configuration;

  private static final String SLAVE_ID = "slave";

  private SingularityTaskRequest taskRequest = Mockito.mock(SingularityTaskRequest.class);
  private SingularityDeploy deploy = Mockito.mock(SingularityDeploy.class);
  private SingularityRequest request = Mockito.mock(SingularityRequest.class);
  private SingularityPendingTask task = Mockito.mock(SingularityPendingTask.class);
  private SingularityPendingTaskId taskId = Mockito.mock(SingularityPendingTaskId.class);


  public SingularityMesosOfferSchedulerTest() {
    super(false);
  }

  @Before
  public void setup() {
    configuration.setLongRunningUsedCpuWeightForOffer(0.30);
    configuration.setLongRunningUsedMemWeightForOffer(0.50);
    configuration.setLongRunningUsedDiskWeightForOffer(0.20);
    configuration.setFreeCpuWeightForOffer(0.30);
    configuration.setFreeMemWeightForOffer(0.50);
    configuration.setFreeDiskWeightForOffer(0.20);
    configuration.setDefaultOfferScoreForMissingUsage(0.10);
    configuration.setConsiderNonLongRunningTaskLongRunningAfterRunningForSeconds(TimeUnit.HOURS.toSeconds(6));
    configuration.setMaxNonLongRunningUsedResourceWeight(0.50);

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
    assertValueIs(0.10, scheduler.score(SLAVE_ID, taskRequest, Optional.absent()));

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

  private void assertValueIs(double expectedValue, double actualValue) {
    actualValue = Math.round(actualValue * 1000.0) / 1000.0;
    Assert.assertTrue(String.format("Expected %f but found %f", expectedValue, actualValue),  actualValue == expectedValue);
  }

  private long mbToBytes(long memMb) {
    return memMb * 1000L * 1000L;
  }

  private SingularitySlaveUsageWithId getUsage(long memMbReserved, long memMbTotal, double cpusReserved, double cpusTotal, long diskMbReserved, long diskMbTotal, Map<ResourceUsageType, Number> longRunningTasksUsage) {
    return new SingularitySlaveUsageWithId(
        new SingularitySlaveUsage(
            0, cpusReserved, Optional.of(cpusTotal), 0, memMbReserved, Optional.of(memMbTotal), 0, diskMbReserved, Optional.of(diskMbTotal), longRunningTasksUsage, 1, 0L
        ),
        SLAVE_ID
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
