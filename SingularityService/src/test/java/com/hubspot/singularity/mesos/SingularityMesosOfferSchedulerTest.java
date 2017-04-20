package com.hubspot.singularity.mesos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.SlaveID;
import org.apache.mesos.Protos.Value.Scalar;
import org.apache.mesos.Protos.Value.Type;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.hubspot.horizon.shaded.org.jboss.netty.util.internal.ThreadLocalRandom;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityCuratorTestBase;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsage.ResourceUsageType;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityTaskRequest;

public class SingularityMesosOfferSchedulerTest extends SingularityCuratorTestBase {

  @Inject
  protected  SingularityMesosOfferScheduler scheduler;

  private SingularityTaskRequest taskRequest = Mockito.mock(SingularityTaskRequest.class);
  private SingularityRequest request = Mockito.mock(SingularityRequest.class);
  private SingularityPendingTask task = Mockito.mock(SingularityPendingTask.class);
  private SingularityPendingTaskId taskId = Mockito.mock(SingularityPendingTaskId.class);
  private final Map<String, Integer> offerMatchAttemptsPerTask = new HashMap<>();


  public SingularityMesosOfferSchedulerTest() {
    super(false);
  }

  @Before
  public void setup() {
    Mockito.when(taskRequest.getRequest()).thenReturn(request);
    Mockito.when(taskRequest.getPendingTask()).thenReturn(task);
    Mockito.when(task.getPendingTaskId()).thenReturn(taskId);
  }

  @Test
  public void itGetsTheCorrectScore() {
    String slaveId = "slave";
    Map<RequestType, Map<ResourceUsageType, Number>> usagePerRequestType = new HashMap<>();
    setRequestType(RequestType.SERVICE);

    // no usage tracked -> default score
    assertScoreIs(0.10, scheduler.score(getOffer(10, 10, slaveId), taskRequest, Optional.empty()));

    // new slave (no resources used) -> perfect score
    usagePerRequestType.put(RequestType.SERVICE, ImmutableMap.of(ResourceUsageType.CPU_USED, 0, ResourceUsageType.MEMORY_BYTES_USED, 0));
    assertScoreIs(1, scheduler.score(getOffer(10, 10, slaveId), taskRequest, Optional.of(getUsage(0, 0, 10, 10, usagePerRequestType, slaveId))));


    // cpu used, no mem used --- different request type
    usagePerRequestType.put(RequestType.SERVICE, ImmutableMap.of(ResourceUsageType.CPU_USED, 0, ResourceUsageType.MEMORY_BYTES_USED, 0));
    assertScoreIs(0.90, scheduler.score(getOffer(5, 10, slaveId), taskRequest, Optional.of(getUsage(0, 5, 10, 10, usagePerRequestType, slaveId))));

    usagePerRequestType.put(RequestType.SERVICE, ImmutableMap.of(ResourceUsageType.CPU_USED, 0, ResourceUsageType.MEMORY_BYTES_USED, 0));
    assertScoreIs(0.84, scheduler.score(getOffer(2, 10, slaveId), taskRequest, Optional.of(getUsage(0, 8, 10, 10, usagePerRequestType, slaveId))));

    // cpu used, no mem used --- same request type
    usagePerRequestType.put(RequestType.SERVICE, ImmutableMap.of(ResourceUsageType.CPU_USED, 5, ResourceUsageType.MEMORY_BYTES_USED, 0));
    assertScoreIs(0.80, scheduler.score(getOffer(5, 10, slaveId), taskRequest, Optional.of(getUsage(0, 5, 10, 10, usagePerRequestType, slaveId))));

    usagePerRequestType.put(RequestType.SERVICE, ImmutableMap.of(ResourceUsageType.CPU_USED, 8, ResourceUsageType.MEMORY_BYTES_USED, 0));
    assertScoreIs(0.68, scheduler.score(getOffer(2, 10, slaveId), taskRequest, Optional.of(getUsage(0, 8, 10, 10, usagePerRequestType, slaveId))));


    // cpu used, no mem used --- different request type
    usagePerRequestType.put(RequestType.SERVICE, ImmutableMap.of(ResourceUsageType.CPU_USED, 0, ResourceUsageType.MEMORY_BYTES_USED, 0));
    assertScoreIs(0.85, scheduler.score(getOffer(10, 5, slaveId), taskRequest, Optional.of(getUsage(mbToBytes(5), 0, 10, 10, usagePerRequestType, slaveId))));

    usagePerRequestType.put(RequestType.SERVICE, ImmutableMap.of(ResourceUsageType.CPU_USED, 0, ResourceUsageType.MEMORY_BYTES_USED, 0));
    assertScoreIs(0.76, scheduler.score(getOffer(10, 2, slaveId), taskRequest, Optional.of(getUsage(mbToBytes(8), 0, 10, 10, usagePerRequestType, slaveId))));

    // no cpu used, mem used --- same request type
    usagePerRequestType.put(RequestType.SERVICE, ImmutableMap.of(ResourceUsageType.CPU_USED, 0, ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(5)));
    assertScoreIs(0.70, scheduler.score(getOffer(10, 5, slaveId), taskRequest, Optional.of(getUsage(mbToBytes(5), 0, 10, 10, usagePerRequestType, slaveId))));

    usagePerRequestType.put(RequestType.SERVICE, ImmutableMap.of(ResourceUsageType.CPU_USED, 0, ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(8)));
    assertScoreIs(0.52, scheduler.score(getOffer(10, 2, slaveId), taskRequest, Optional.of(getUsage(mbToBytes(8), 0, 10, 10, usagePerRequestType, slaveId))));


    // cpu used, mem used --- different request type
    usagePerRequestType.put(RequestType.SERVICE, ImmutableMap.of(ResourceUsageType.CPU_USED, 0, ResourceUsageType.MEMORY_BYTES_USED, 0));
    assertScoreIs(0.75, scheduler.score(getOffer(5, 5, slaveId), taskRequest, Optional.of(getUsage(mbToBytes(5), 5, 10, 10, usagePerRequestType, slaveId))));

    usagePerRequestType.put(RequestType.SERVICE, ImmutableMap.of(ResourceUsageType.CPU_USED, 0, ResourceUsageType.MEMORY_BYTES_USED, 0));
    assertScoreIs(0.60, scheduler.score(getOffer(2, 2, slaveId), taskRequest, Optional.of(getUsage(mbToBytes(8), 8, 10, 10, usagePerRequestType, slaveId))));

    // cpu used, mem used --- same request type
    usagePerRequestType.put(RequestType.SERVICE, ImmutableMap.of(ResourceUsageType.CPU_USED, 5, ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(5)));
    assertScoreIs(0.50, scheduler.score(getOffer(5, 5, slaveId), taskRequest, Optional.of(getUsage(mbToBytes(5), 5, 10, 10, usagePerRequestType, slaveId))));

    usagePerRequestType.put(RequestType.SERVICE, ImmutableMap.of(ResourceUsageType.CPU_USED, 8, ResourceUsageType.MEMORY_BYTES_USED, mbToBytes(8)));
    assertScoreIs(0.20, scheduler.score(getOffer(2, 2, slaveId), taskRequest, Optional.of(getUsage(mbToBytes(8), 8, 10, 10, usagePerRequestType, slaveId))));
  }

  @Test
  public void itGetsTheCorrectMinScore() {
    long now = System.currentTimeMillis();
    String taskId = "taskId";
    setNextRunAt(now);
    setTaskId(taskId);

    // no attempts, no delay
    addOrUpdateOfferMatchAttempt(taskId, 0);
    assertScoreIs(0.80, scheduler.minScore(taskRequest, offerMatchAttemptsPerTask, now));

    // no attempts, delay
    assertScoreIs(0.30, scheduler.minScore(taskRequest, offerMatchAttemptsPerTask, now + TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES)));

    // attempts, no delay
    addOrUpdateOfferMatchAttempt(taskId, 10);
    assertScoreIs(0.30, scheduler.minScore(taskRequest, offerMatchAttemptsPerTask, now));

    // attempts, delay
    assertScoreIs(0, scheduler.minScore(taskRequest, offerMatchAttemptsPerTask, now + TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES)));

    addOrUpdateOfferMatchAttempt(taskId, 1);
    assertScoreIs(0.25, scheduler.minScore(taskRequest, offerMatchAttemptsPerTask, now + TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES)));

    addOrUpdateOfferMatchAttempt(taskId, 4);
    assertScoreIs(0.10, scheduler.minScore(taskRequest, offerMatchAttemptsPerTask, now + TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES)));
  }

  private void assertScoreIs(double expectedScore, double actualScore) {
    Assert.assertTrue(Math.round(actualScore * 100.0) / 100.0 == expectedScore);
  }

  private Offer getOffer(double cpus, long memMb, String slaveId) {
    return Offer.newBuilder()
        .setId(OfferID.newBuilder().setValue("offer" + ThreadLocalRandom.current().nextInt(1000)).build())
        .setFrameworkId(FrameworkID.newBuilder().setValue("framework1").build())
        .setSlaveId(SlaveID.newBuilder().setValue(slaveId).build())
        .setHostname("host")
        .addResources(getCpuResource(cpus))
        .addResources(getMemResource(memMb))
        .build();
  }

  private long mbToBytes(long memMb) {
    return memMb * 1024L * 1024L;
  }

  private SingularitySlaveUsageWithId getUsage(long memBytes, double cpus, long memMbTotal, double cpusTotal, Map<RequestType, Map<ResourceUsageType, Number>> usagePerRequestType, String slaveId) {
    return new SingularitySlaveUsageWithId(new SingularitySlaveUsage(memBytes, 0L, cpus, 1, Optional.of(memMbTotal), Optional.of(cpusTotal), usagePerRequestType), slaveId);
  }

  private Resource.Builder getCpuResource(double cpus) {
    return Resource.newBuilder().setType(Type.SCALAR).setName(MesosUtils.CPUS).setScalar(Scalar.newBuilder().setValue(cpus));
  }

  private Resource.Builder getMemResource(double memMb) {
    return Resource.newBuilder().setType(Type.SCALAR).setName(MesosUtils.MEMORY).setScalar(Scalar.newBuilder().setValue(memMb));
  }

  private void setNextRunAt(long time) {
    Mockito.when(taskId.getNextRunAt()).thenReturn(time);
  }

  private void setTaskId(String id) {
    Mockito.when(taskId.getId()).thenReturn(id);
  }

  private void setRequestType(RequestType type) {
    Mockito.when(request.getRequestType()).thenReturn(type);
  }

  private void addOrUpdateOfferMatchAttempt(String id, int attempts) {
    offerMatchAttemptsPerTask.put(id, attempts);
  }
}
