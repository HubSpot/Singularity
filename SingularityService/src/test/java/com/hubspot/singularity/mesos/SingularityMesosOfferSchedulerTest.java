package com.hubspot.singularity.mesos;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.singularity.AgentMatchState;
import com.hubspot.singularity.MachineLoadMetric;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityAgentUsage;
import com.hubspot.singularity.SingularityAgentUsageWithId;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityDeployStatisticsBuilder;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.usage.UsageManager;
import com.hubspot.singularity.mesos.SingularityAgentUsageWithCalculatedScores.MaxProbableUsage;
import com.hubspot.singularity.scheduler.SingularityScheduler;
import com.hubspot.singularity.scheduler.SingularitySchedulerTestBase;
import com.hubspot.singularity.scheduler.SingularityUsagePoller;
import com.hubspot.singularity.scheduler.TestingMesosClient;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.mesos.v1.Protos.Offer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SingularityMesosOfferSchedulerTest extends SingularitySchedulerTestBase {
  @Inject
  protected SingularityPendingTaskQueueProcessor queueProcessor;

  @Inject
  protected SingularityScheduler singularityScheduler;

  @Inject
  protected DeployManager deployManager;

  @Inject
  protected SingularityConfiguration configuration;

  @Inject
  protected SingularityOfferScoring offerScoring;

  @Inject
  protected SingularityAgentAndRackManager agentAndRackManager;

  @Inject
  protected UsageManager usageManager;

  @Inject
  protected TestingMesosClient mesosClient;

  @Inject
  protected SingularityUsagePoller usagePoller;

  private static final String AGENT_ID = "agent";

  private SingularityTaskRequest taskRequest = Mockito.mock(SingularityTaskRequest.class);
  private SingularityDeploy deploy = Mockito.mock(SingularityDeploy.class);
  private SingularityRequest request = Mockito.mock(SingularityRequest.class);
  private SingularityPendingTask task = Mockito.mock(SingularityPendingTask.class);
  private SingularityPendingTaskId taskId = Mockito.mock(SingularityPendingTaskId.class);

  public SingularityMesosOfferSchedulerTest() {
    super(
      false,
      configuration -> {
        configuration.getMesosConfiguration().setCpuWeight(0.30);
        configuration.getMesosConfiguration().setMemWeight(0.50);
        configuration.getMesosConfiguration().setDiskWeight(0.20);
        return null;
      }
    );
  }

  @BeforeEach
  public void setupMocks() {
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
    assertValueIs(0.50, offerScoring.score(AGENT_ID, null, AgentMatchState.OK));

    // NLR - no deployStatistics -> default weights
    setRequestType(RequestType.ON_DEMAND);
    assertValueIs(
      0.5,
      offerScoring.score(
        AGENT_ID,
        getUsage(5, 10, 5, 5, 10, 5, 5, 10, 5),
        AgentMatchState.OK
      )
    );
  }

  @Test
  public void itCorrectlyScoresLongRunningTasks() {
    setRequestType(RequestType.SERVICE);

    // new agent (no resources used) -> perfect score
    assertValueIs(
      1,
      offerScoring.score(
        AGENT_ID,
        getUsage(0, 10, 0, 0, 10, 0, 0, 10, 0),
        AgentMatchState.OK
      )
    );

    // cpu used, no mem used, no disk used
    assertValueIs(
      0.85,
      offerScoring.score(
        AGENT_ID,
        getUsage(0, 10, 0, 5, 10, 5, 0, 10, 0),
        AgentMatchState.OK
      )
    );
    assertValueIs(
      0.76,
      offerScoring.score(
        AGENT_ID,
        getUsage(0, 10, 0, 8, 10, 8, 0, 10, 0),
        AgentMatchState.OK
      )
    );

    // no cpu used, mem used, no disk used
    assertValueIs(
      0.75,
      offerScoring.score(
        AGENT_ID,
        getUsage(5, 10, 5, 0, 10, 0, 0, 10, 0),
        AgentMatchState.OK
      )
    );
    assertValueIs(
      0.60,
      offerScoring.score(
        AGENT_ID,
        getUsage(8, 10, 8, 0, 10, 0, 0, 10, 0),
        AgentMatchState.OK
      )
    );

    // no cpu used, no mem used, disk used
    assertValueIs(
      0.90,
      offerScoring.score(
        AGENT_ID,
        getUsage(0, 10, 0, 0, 10, 0, 5, 10, 5),
        AgentMatchState.OK
      )
    );
    assertValueIs(
      0.84,
      offerScoring.score(
        AGENT_ID,
        getUsage(0, 10, 0, 0, 10, 0, 8, 10, 8),
        AgentMatchState.OK
      )
    );

    // cpu used, mem used, no disk used
    assertValueIs(
      0.60,
      offerScoring.score(
        AGENT_ID,
        getUsage(5, 10, 5, 5, 10, 5, 0, 10, 0),
        AgentMatchState.OK
      )
    );
    assertValueIs(
      0.36,
      offerScoring.score(
        AGENT_ID,
        getUsage(8, 10, 8, 8, 10, 8, 0, 10, 0),
        AgentMatchState.OK
      )
    );

    // no cpu used, mem used, disk used
    assertValueIs(
      0.65,
      offerScoring.score(
        AGENT_ID,
        getUsage(5, 10, 5, 0, 10, 0, 5, 10, 5),
        AgentMatchState.OK
      )
    );
    assertValueIs(
      0.44,
      offerScoring.score(
        AGENT_ID,
        getUsage(8, 10, 8, 0, 10, 0, 8, 10, 8),
        AgentMatchState.OK
      )
    );

    // cpu used, no mem used, disk used
    assertValueIs(
      0.75,
      offerScoring.score(
        AGENT_ID,
        getUsage(0, 10, 0, 5, 10, 5, 5, 10, 5),
        AgentMatchState.OK
      )
    );
    assertValueIs(
      0.60,
      offerScoring.score(
        AGENT_ID,
        getUsage(0, 10, 0, 8, 10, 8, 8, 10, 8),
        AgentMatchState.OK
      )
    );

    // cpu used, mem used, disk used
    assertValueIs(
      0.5,
      offerScoring.score(
        AGENT_ID,
        getUsage(5, 10, 5, 5, 10, 5, 5, 10, 5),
        AgentMatchState.OK
      )
    );
    assertValueIs(
      0.2,
      offerScoring.score(
        AGENT_ID,
        getUsage(8, 10, 8, 8, 10, 8, 8, 10, 8),
        AgentMatchState.OK
      )
    );
  }

  @Test
  public void itCorrectlyScalesScoresForPreferredHosts() {
    assertValueIs(
      0.50,
      offerScoring.score(
        AGENT_ID,
        getUsage(5, 10, 5, 5, 10, 5, 5, 10, 5),
        AgentMatchState.OK
      )
    );
    assertValueIs(
      0.75,
      offerScoring.score(
        AGENT_ID,
        getUsage(5, 10, 5, 5, 10, 5, 5, 10, 5),
        AgentMatchState.PREFERRED_AGENT
      )
    );

    assertValueIs(
      0.20,
      offerScoring.score(
        AGENT_ID,
        getUsage(8, 10, 8, 8, 10, 8, 8, 10, 8),
        AgentMatchState.OK
      )
    );
    assertValueIs(
      0.30,
      offerScoring.score(
        AGENT_ID,
        getUsage(8, 10, 8, 8, 10, 8, 8, 10, 8),
        AgentMatchState.PREFERRED_AGENT
      )
    );
  }

  @Test
  public void itAccountsForExpectedTaskUsage() {
    initRequest();
    double cpuReserved = 2;
    double memMbReserved = 1000;
    initFirstDeployWithResources(cpuReserved, memMbReserved);
    saveAndSchedule(
      requestManager
        .getRequest(requestId)
        .get()
        .getRequest()
        .toBuilder()
        .setInstances(Optional.of(1))
    );
    resourceOffers(3);

    SingularityTaskId taskId = taskManager.getActiveTaskIds().get(0);
    String t1 = taskId.getId();

    // 2 cpus used
    MesosTaskMonitorObject t1u1 = getTaskMonitor(
      t1,
      10,
      (double) (taskId.getStartedAt() + 5000) / 1000,
      1000
    );
    mesosClient.setAgentResourceUsage("host1", Collections.singletonList(t1u1));
    usagePoller.runActionOnPoll();
    SingularityAgentUsage smallUsage = new SingularityAgentUsage(
      0.1,
      0.1,
      Optional.of(10.0),
      1,
      1,
      Optional.of(30L),
      1,
      1,
      Optional.of(1024L),
      1,
      System.currentTimeMillis(),
      1,
      30000,
      10,
      0,
      0,
      0,
      0,
      107374182
    );

    usageManager.saveCurrentAgentUsage(
      new SingularityAgentUsageWithId(smallUsage, "host1")
    );
    usageManager.saveCurrentAgentUsage(
      new SingularityAgentUsageWithId(smallUsage, "host2")
    );
    usageManager.saveCurrentAgentUsage(
      new SingularityAgentUsageWithId(smallUsage, "host3")
    );

    requestResource.scale(
      requestId,
      new SingularityScaleRequest(
        Optional.of(3),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      ),
      SingularityUser.DEFAULT_USER
    );

    Assertions.assertEquals(
      2.0,
      usageManager.getRequestUtilizations().get(requestId).getCpuUsed(),
      0.001
    );

    Offer host2Offer = createOffer(6, 30000, 107374182, "host2", "host2");
    agentAndRackManager.checkOffer(host2Offer);
    Offer host3Offer = createOffer(6, 30000, 107374182, "host3", "host3");
    agentAndRackManager.checkOffer(host3Offer);

    singularityScheduler.drainPendingQueue();
    sms.resourceOffers(ImmutableList.of(host2Offer, host3Offer));
    queueProcessor.drainHandledTasks(5000);
    List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
    Assertions.assertEquals(2, activeTaskIds.size());
    Assertions.assertNotEquals(
      activeTaskIds.get(0).getSanitizedHost(),
      activeTaskIds.get(1).getSanitizedHost()
    );
  }

  @Test
  public void itAccountsForMaxHistoricalTaskUsage() {
    initRequest();
    double cpuReserved = 2;
    double memMbReserved = 1000;
    initFirstDeployWithResources(cpuReserved, memMbReserved);
    saveAndSchedule(
      requestManager
        .getRequest(requestId)
        .get()
        .getRequest()
        .toBuilder()
        .setInstances(Optional.of(1))
    );
    resourceOffers(3);

    SingularityTaskId taskId = taskManager.getActiveTaskIds().get(0);
    String t1 = taskId.getId();

    // 2 cpus used
    MesosTaskMonitorObject t1u1 = getTaskMonitor(
      t1,
      10,
      getTimestampSeconds(taskId, 5),
      1000
    );
    mesosClient.setAgentResourceUsage("host1", Collections.singletonList(t1u1));
    usagePoller.runActionOnPoll();

    // 1 cpus used
    MesosTaskMonitorObject t1u2 = getTaskMonitor(
      t1,
      11,
      getTimestampSeconds(taskId, 6),
      1000
    );
    mesosClient.setAgentResourceUsage("host1", Collections.singletonList(t1u2));
    usagePoller.runActionOnPoll();
    SingularityAgentUsage smallUsage = new SingularityAgentUsage(
      0.1,
      0.1,
      Optional.of(10.0),
      1,
      1,
      Optional.of(30L),
      1,
      1,
      Optional.of(1024L),
      1,
      System.currentTimeMillis(),
      1,
      30000,
      10,
      0,
      0,
      0,
      0,
      107374182
    );

    usageManager.saveCurrentAgentUsage(
      new SingularityAgentUsageWithId(smallUsage, "host1")
    );
    usageManager.saveCurrentAgentUsage(
      new SingularityAgentUsageWithId(smallUsage, "host2")
    );
    usageManager.saveCurrentAgentUsage(
      new SingularityAgentUsageWithId(smallUsage, "host3")
    );

    requestResource.scale(
      requestId,
      new SingularityScaleRequest(
        Optional.of(3),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty()
      ),
      SingularityUser.DEFAULT_USER
    );

    Assertions.assertEquals(
      3.0,
      usageManager.getRequestUtilizations().get(requestId).getCpuUsed(),
      0.001
    );

    Offer host2Offer = createOffer(6, 30000, 107374182, "host2", "host2");
    agentAndRackManager.checkOffer(host2Offer);
    Offer host3Offer = createOffer(6, 30000, 107374182, "host3", "host3");
    agentAndRackManager.checkOffer(host3Offer);

    singularityScheduler.drainPendingQueue();
    queueProcessor.drainHandledTasks(5000);
    List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
    Assertions.assertEquals(2, activeTaskIds.size());
    Assertions.assertNotEquals(
      activeTaskIds.get(0).getSanitizedHost(),
      activeTaskIds.get(1).getSanitizedHost()
    );
  }

  private void assertValueIs(double expectedValue, double actualValue) {
    actualValue = Math.round(actualValue * 1000.0) / 1000.0;
    Assertions.assertEquals(
      actualValue,
      expectedValue,
      String.format("Expected %f but found %f", expectedValue, actualValue)
    );
  }

  private SingularityAgentUsageWithCalculatedScores getUsage(
    long memMbReserved,
    long memMbTotal,
    long memMbInUse,
    double cpusReserved,
    double cpusTotal,
    double cpuInUse,
    long diskMbReserved,
    long diskMbTotal,
    long diskMbInUse
  ) {
    long totalMemBytes = memMbTotal * SingularityAgentUsage.BYTES_PER_MEGABYTE;
    long memBytesInUse = memMbInUse * SingularityAgentUsage.BYTES_PER_MEGABYTE;
    return new SingularityAgentUsageWithCalculatedScores(
      new SingularityAgentUsage(
        cpuInUse,
        cpusReserved,
        Optional.of(cpusTotal),
        memBytesInUse,
        memMbReserved,
        Optional.of(memMbTotal),
        diskMbInUse * SingularityAgentUsage.BYTES_PER_MEGABYTE,
        diskMbReserved,
        Optional.of(diskMbTotal),
        1,
        0L,
        totalMemBytes,
        totalMemBytes - memBytesInUse,
        cpusTotal,
        cpuInUse,
        cpuInUse,
        cpuInUse,
        diskMbInUse * SingularityAgentUsage.BYTES_PER_MEGABYTE,
        diskMbTotal * SingularityAgentUsage.BYTES_PER_MEGABYTE
      ),
      MachineLoadMetric.LOAD_5,
      new MaxProbableUsage(0, 0, 0),
      0,
      0,
      System.currentTimeMillis()
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

  private double getTimestampSeconds(SingularityTaskId taskId, long seconds) {
    return ((double) taskId.getStartedAt() + seconds * 1000) / 1000;
  }
}
