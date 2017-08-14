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
    configuration.setLongRunningUsedCpuWeightForOffer(0.40);
    configuration.setLongRunningUsedMemWeightForOffer(0.60);
    configuration.setFreeCpuWeightForOffer(0.40);
    configuration.setFreeMemWeightForOffer(0.60);
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
    assertValueIs(0.25, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(5, 10, 5, 10, longRunningTasksUsage))));
  }

  @Test
  public void itCorrectlyScoresLongRunningTasks() {
    Map<ResourceUsageType, Number> longRunningTasksUsage = new HashMap<>();
    setRequestType(RequestType.SERVICE);

    // new slave (no resources used) -> perfect score
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    assertValueIs(1, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0,10, 0,10, longRunningTasksUsage))));

    // cpu used, no mem used
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 5);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    assertValueIs(0.80, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 5, 10, longRunningTasksUsage))));

    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 8);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    assertValueIs(0.68, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 8, 10, longRunningTasksUsage))));

    // no cpu used, mem used
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(5));
    assertValueIs(0.70, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(5, 10, 0, 10, longRunningTasksUsage))));

    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(8));
    assertValueIs(0.52, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(8, 10, 0, 10, longRunningTasksUsage))));

    // cpu used, mem used
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 5);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(5));
    assertValueIs(0.50, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(5, 10, 5, 10, longRunningTasksUsage))));

    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 8);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(8));
    assertValueIs(0.20, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(8, 10, 8, 10, longRunningTasksUsage))));
  }

  @Test
  public void itCorrectlyScoresEmptySlaveNonLongRunningTasks() {
    Map<ResourceUsageType, Number> longRunningTasksUsage = new HashMap<>();
    setRequestType(RequestType.ON_DEMAND);

    // new slave (no resources used) -> near perfect score
    setDeployStatistics(TimeUnit.MINUTES, 5);
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    assertValueIs(.993, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 0, 10, longRunningTasksUsage))));

    setDeployStatistics(TimeUnit.HOURS, 6);
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    assertValueIs(1, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 0, 10, longRunningTasksUsage))));

    setDeployStatistics(TimeUnit.HOURS, 12);
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    assertValueIs(1, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 0, 10, longRunningTasksUsage))));
  }

  @Test
  public void itCorrectlyScoresShortNonLongRunningTasks() {
    Map<ResourceUsageType, Number> longRunningTasksUsage = new HashMap<>();
    setRequestType(RequestType.ON_DEMAND);

    // short duration
    setDeployStatistics(TimeUnit.HOURS, 1);

    // 50% LR cpu -- 0% LR mem
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 5);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    assertValueIs(0.717, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 5, 10, longRunningTasksUsage))));

    // 20% NLR cpu -- 20% NLR mem
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    double nlrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(2, 10, 2, 10, longRunningTasksUsage)));
    assertValueIs(0.733, nlrScore);

    // 20% LR cpu -- 20% LR mem
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 2);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(2));
    double lrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(2, 10, 2, 10, longRunningTasksUsage)));
    assertValueIs(0.717, lrScore);

    Assert.assertTrue(nlrScore > lrScore);

    // 30% NLR cpu -- 30% NLR mem
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    nlrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(3, 10, 3, 10, longRunningTasksUsage)));
    assertValueIs(0.642, nlrScore);

    Assert.assertTrue(nlrScore < lrScore);
  }

  @Test
  public void itCorrectlyScoresMediumLongNonLongRunningTasks() {
    Map<ResourceUsageType, Number> longRunningTasksUsage = new HashMap<>();
    setRequestType(RequestType.ON_DEMAND);

    // medium duration
    setDeployStatistics(TimeUnit.HOURS, 3);

    // 50% LR cpu -- 0% LR mem
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 5);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    assertValueIs(0.55, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 5, 10, longRunningTasksUsage))));

    // 20% NLR cpu -- 20% NLR mem
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    double nlrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(2, 10, 2, 10, longRunningTasksUsage)));
    assertValueIs(0.6, nlrScore);

    // 20% LR cpu -- 20% LR mem
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 2);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(2));
    double lrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(2, 10, 2, 10, longRunningTasksUsage)));
    assertValueIs(0.55, lrScore);

    Assert.assertTrue(nlrScore > lrScore);

    // 30% NLR cpu -- 30% NLR mem
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    nlrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(3, 10, 3, 10, longRunningTasksUsage)));
    assertValueIs(0.525, nlrScore);

    Assert.assertTrue(lrScore > nlrScore);
  }

  @Test
  public void itCorrectlyScoresLongNonLongRunningTasks() {
    Map<ResourceUsageType, Number> longRunningTasksUsage = new HashMap<>();
    setRequestType(RequestType.ON_DEMAND);

    // long duration
    setDeployStatistics(TimeUnit.HOURS, 6);

    // 50% LR cpu -- 0% LR mem
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 5);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    assertValueIs(0.8, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 5, 10, longRunningTasksUsage))));

    // 20% NLR cpu -- 20% NLR mem
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    double nlrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(2, 10, 2, 10, longRunningTasksUsage)));
    assertValueIs(0.9, nlrScore);

    // 20% LR cpu -- 20% LR mem
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 2);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(2));
    double lrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(2, 10, 2, 10, longRunningTasksUsage)));
    assertValueIs(0.8, lrScore);

    Assert.assertTrue(nlrScore > lrScore);

    // 30% NLR cpu -- 30% NLR mem
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    nlrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(3, 10, 3, 10, longRunningTasksUsage)));
    assertValueIs(0.85, nlrScore);

    Assert.assertTrue(nlrScore > lrScore);

    // over max duration
    setDeployStatistics(TimeUnit.HOURS, 12);

    // 50% LR cpu -- 0% LR mem
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 5);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    assertValueIs(0.8, scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(0, 10, 5, 10, longRunningTasksUsage))));

    // 20% NLR cpu -- 20% NLR mem
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    nlrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(2, 10, 2, 10, longRunningTasksUsage)));
    assertValueIs(0.9, nlrScore);

    // 20% LR cpu -- 20% LR mem
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 2);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(2));
    lrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(2, 10, 2, 10, longRunningTasksUsage)));
    assertValueIs(0.8, lrScore);

    Assert.assertTrue(nlrScore > lrScore);

    // 30% NLR cpu -- 30% NLR mem
    longRunningTasksUsage.put(ResourceUsageType.CPU_USED, 0);
    longRunningTasksUsage.put(ResourceUsageType.MEMORY_BYTES_USED, 0);
    nlrScore = scheduler.score(SLAVE_ID, taskRequest, Optional.of(getUsage(3, 10, 3, 10, longRunningTasksUsage)));
    assertValueIs(0.85, nlrScore);

    Assert.assertTrue(nlrScore > lrScore);
  }

  private void assertValueIs(double expectedValue, double actualValue) {
    actualValue = Math.round(actualValue * 1000.0) / 1000.0;
    Assert.assertTrue(String.format("Expected %f but found %f", expectedValue, actualValue),  actualValue == expectedValue);
  }

  private long mbToBytes(long memMb) {
    return memMb * 1000L * 1000L;
  }

  private SingularitySlaveUsageWithId getUsage(long memMbReserved, long memMbTotal, double cpusReserved, double cpusTotal, Map<ResourceUsageType, Number> longRunningTasksUsage) {
    return new SingularitySlaveUsageWithId(new SingularitySlaveUsage(0, memMbReserved,0L, 0, cpusReserved,1, Optional.of(memMbTotal), Optional.of(cpusTotal), longRunningTasksUsage), SLAVE_ID);
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
