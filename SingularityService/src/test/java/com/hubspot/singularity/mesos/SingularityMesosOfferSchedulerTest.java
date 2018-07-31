package com.hubspot.singularity.mesos;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.v1.Protos.Offer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.singularity.MachineLoadMetric;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityDeployStatisticsBuilder;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.UsageManager;
import com.hubspot.singularity.mesos.SingularitySlaveUsageWithCalculatedScores.MaxProbableUsage;
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
    super(false, (configuration) -> {
      configuration.getMesosConfiguration().setCpuWeight(0.30);
      configuration.getMesosConfiguration().setMemWeight(0.50);
      configuration.getMesosConfiguration().setDiskWeight(0.20);
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
    setRequestType(RequestType.SERVICE);

    // LR - no usage tracked -> default score
    assertValueIs(0.50, scheduler.score(SLAVE_ID, Optional.absent()));

    // NLR - no deployStatistics -> default weights
    setRequestType(RequestType.ON_DEMAND);
    assertValueIs(0.5, scheduler.score(SLAVE_ID, Optional.of(getUsage(5, 10, 5,  5, 10, 5, 5, 10, 5))));
  }

  @Test
  public void itCorrectlyScoresLongRunningTasks() {
    setRequestType(RequestType.SERVICE);

    // new slave (no resources used) -> perfect score
    assertValueIs(1, scheduler.score(SLAVE_ID, Optional.of(getUsage(0,10, 0, 0,10, 0, 0, 10, 0))));

    // cpu used, no mem used, no disk used
    assertValueIs(0.85, scheduler.score(SLAVE_ID, Optional.of(getUsage(0, 10, 0, 5, 10, 5, 0, 10, 0))));
    assertValueIs(0.76, scheduler.score(SLAVE_ID, Optional.of(getUsage(0, 10, 0, 8, 10, 8, 0, 10, 0))));

    // no cpu used, mem used, no disk used
    assertValueIs(0.75, scheduler.score(SLAVE_ID, Optional.of(getUsage(5, 10, 5, 0, 10, 0, 0, 10, 0))));
    assertValueIs(0.60, scheduler.score(SLAVE_ID, Optional.of(getUsage(8, 10, 8, 0, 10, 0, 0, 10, 0))));

    // no cpu used, no mem used, disk used
    assertValueIs(0.90, scheduler.score(SLAVE_ID, Optional.of(getUsage(0, 10, 0, 0, 10, 0, 5, 10, 5))));
    assertValueIs(0.84, scheduler.score(SLAVE_ID, Optional.of(getUsage(0, 10, 0, 0, 10, 0, 8, 10, 8))));

    // cpu used, mem used, no disk used
    assertValueIs(0.60, scheduler.score(SLAVE_ID, Optional.of(getUsage(5, 10, 5, 5, 10, 5, 0, 10, 0))));
    assertValueIs(0.36, scheduler.score(SLAVE_ID, Optional.of(getUsage(8, 10, 8, 8, 10, 8, 0, 10, 0))));

    // no cpu used, mem used, disk used
    assertValueIs(0.65, scheduler.score(SLAVE_ID, Optional.of(getUsage(5,10, 5, 0, 10,0, 5, 10, 5))));
    assertValueIs(0.44, scheduler.score(SLAVE_ID, Optional.of(getUsage(8,10, 8, 0,10, 0, 8, 10, 8))));

    // cpu used, no mem used, disk used
    assertValueIs(0.75, scheduler.score(SLAVE_ID, Optional.of(getUsage(0,10, 0, 5,10, 5, 5, 10, 5))));
    assertValueIs(0.60, scheduler.score(SLAVE_ID, Optional.of(getUsage(0,10, 0, 8,10, 8, 8, 10, 8))));

    // cpu used, mem used, disk used
    assertValueIs(0.5, scheduler.score(SLAVE_ID, Optional.of(getUsage(5,10, 5, 5,10, 5, 5, 10, 5))));
    assertValueIs(0.2, scheduler.score(SLAVE_ID, Optional.of(getUsage(8,10, 8, 8,10, 8, 8, 10, 8))));
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
    SingularitySlaveUsage smallUsage = new SingularitySlaveUsage(0.1, 0.1, Optional.of(10.0), 1, 1, Optional.of(30L), 1, 1, Optional.of(1024L), 1, System.currentTimeMillis(), 1, 30000, 10, 0, 0, 0, 0, 107374182);

    usageManager.saveSpecificSlaveUsageAndSetCurrent("host1", smallUsage);
    usageManager.saveSpecificSlaveUsageAndSetCurrent("host2", smallUsage);
    usageManager.saveSpecificSlaveUsageAndSetCurrent("host3", smallUsage);

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(3), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent()), SingularityUser.DEFAULT_USER);

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

  @Test
  public void itAccountsForMaxHistoricalTaskUsage() {
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

    // 1 cpus used
    MesosTaskMonitorObject t1u2 = getTaskMonitor(t1, 11, TimeUnit.MILLISECONDS.toSeconds(taskId.getStartedAt()) + 6, 1000);
    mesosClient.setSlaveResourceUsage("host1", Collections.singletonList(t1u2));
    usagePoller.runActionOnPoll();
    SingularitySlaveUsage smallUsage = new SingularitySlaveUsage(0.1, 0.1, Optional.of(10.0), 1, 1, Optional.of(30L), 1, 1, Optional.of(1024L), 1, System.currentTimeMillis(), 1, 30000, 10, 0, 0, 0, 0, 107374182);

    usageManager.saveSpecificSlaveUsageAndSetCurrent("host1", smallUsage);
    usageManager.saveSpecificSlaveUsageAndSetCurrent("host2", smallUsage);
    usageManager.saveSpecificSlaveUsageAndSetCurrent("host3", smallUsage);

    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(3), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional
        .absent()), SingularityUser.DEFAULT_USER);

    Assert.assertEquals(3.0, usageManager.getRequestUtilizations().get(requestId).getCpuUsed(), 0.001);

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

  private SingularitySlaveUsageWithCalculatedScores getUsage(long memMbReserved,
                                                             long memMbTotal,
                                                             long memMbInUse,
                                                             double cpusReserved,
                                                             double cpusTotal,
                                                             double cpuInUse,
                                                             long diskMbReserved,
                                                             long diskMbTotal,
                                                             long diskMbInUse) {
    long totalMemBytes = memMbTotal * SingularitySlaveUsage.BYTES_PER_MEGABYTE;
    long memBytesInUse = memMbInUse * SingularitySlaveUsage.BYTES_PER_MEGABYTE;
    return new SingularitySlaveUsageWithCalculatedScores(
        new SingularitySlaveUsage(
            cpuInUse, cpusReserved, Optional.of(cpusTotal),
            memBytesInUse, memMbReserved, Optional.of(memMbTotal),
            diskMbInUse * SingularitySlaveUsage.BYTES_PER_MEGABYTE, diskMbReserved, Optional.of(diskMbTotal),
            1, 0L,
            totalMemBytes, totalMemBytes - memBytesInUse,
            cpusTotal, cpuInUse, cpuInUse, cpuInUse,
            diskMbInUse * SingularitySlaveUsage.BYTES_PER_MEGABYTE, diskMbTotal * SingularitySlaveUsage.BYTES_PER_MEGABYTE),
        MachineLoadMetric.LOAD_5, new MaxProbableUsage(0, 0, 0),
        0, 0, System.currentTimeMillis()
    );
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
