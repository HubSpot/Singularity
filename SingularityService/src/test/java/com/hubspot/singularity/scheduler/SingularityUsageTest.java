package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.v1.Protos.TaskState;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosSlaveMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityClusterUtilization;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCurrentUsageWithId;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.data.UsageManager;

public class SingularityUsageTest extends SingularitySchedulerTestBase {

  @Inject
  protected SingularityUsageHelper usageHelper;

  @Inject
  protected SingularityUsagePoller usagePoller;

  @Inject
  protected SingularityUsageCleanerPoller cleaner;

  @Inject
  protected UsageManager usageManager;

  @Inject
  protected TestingMesosClient mesosClient;

  public SingularityUsageTest() {
    super(false);
  }

  @Test
  public void testUsagePollerSimple() {
    // works with no slaves
    usagePoller.runActionOnPoll();
    cleaner.runActionOnPoll();

    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)));
    resourceOffers(1);

    SingularityTask firstTask = taskManager.getActiveTasks().get(0);
    String hostname = firstTask.getHostname();
    MesosTaskMonitorObject usage = getTaskMonitor(firstTask.getTaskId().getId(), 2, 5, 100);
    mesosClient.setSlaveResourceUsage(hostname, Collections.singletonList(usage));
    usagePoller.runActionOnPoll();

    String slaveId = firstTask.getAgentId().getValue();

    List<String> slaves = usageManager.getSlavesWithUsage();

    Assert.assertEquals(1, slaves.size());
    Assert.assertEquals(slaves.get(0), slaveId);

    Assert.assertEquals(0, usageManager.getSlaveUsage(slaveId).get(0).getCpusUsed(), 0);
    Assert.assertEquals(100, usageManager.getSlaveUsage(slaveId).get(0).getMemoryBytesUsed(), 0);

    SingularityTaskUsage first = usageManager.getTaskUsage(firstTask.getTaskId().getId()).get(0);

    Assert.assertEquals(2, first.getCpuSeconds(), 0);
    Assert.assertEquals(100, first.getMemoryTotalBytes(), 0);
    Assert.assertEquals(5, first.getTimestamp(), 0);
  }

  @Test
  public void testUsageCleaner() {
    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)));
    resourceOffers(1);

    List<SingularityTaskId> taskIds = taskManager.getActiveTaskIds();

    String t1 = taskIds.get(0).getId();
    String t2 = taskIds.get(1).getId();

    String slaveId = slaveManager.getObjectIds().get(0);
    String host = slaveManager.getObjects().get(0).getHost();

    MesosTaskMonitorObject t1u1 = getTaskMonitor(t1, 2, 5, 100);
    MesosTaskMonitorObject t2u1 = getTaskMonitor(t2, 10, 5, 1000);

    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u1, t2u1));

    usagePoller.runActionOnPoll();
    cleaner.runActionOnPoll();

    Assert.assertEquals(2, usageManager.getTasksWithUsage().size());
    Assert.assertEquals(1, usageManager.getSlavesWithUsage().size());

    Assert.assertEquals(1100, usageManager.getAllCurrentSlaveUsage().get(0).getMemoryBytesUsed(), 0);

    // kill task one
    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_KILLED);
    killKilledTasks();

    cleaner.runActionOnPoll();

    Assert.assertEquals(1, usageManager.getTasksWithUsage().size());
    Assert.assertEquals(1, usageManager.getSlavesWithUsage().size());

    slaveManager.changeState(slaveId, MachineState.DEAD, Optional.absent(), Optional.absent());

    cleaner.runActionOnPoll();

    Assert.assertEquals(1, usageManager.getTasksWithUsage().size());
    Assert.assertEquals(0, usageManager.getSlavesWithUsage().size());
  }

  @Test
  public void testUsagePoller() throws InterruptedException {
    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)));
    resourceOffers(1);

    configuration.setNumUsageToKeep(2);
    configuration.setCheckUsageEveryMillis(1);

    List<SingularityTaskId> taskIds = taskManager.getActiveTaskIds();

    String t1 = taskIds.get(0).getId();
    String t2 = taskIds.get(1).getId();

    String slaveId = slaveManager.getObjectIds().get(0);
    String host = slaveManager.getObjects().get(0).getHost();

    MesosTaskMonitorObject t1u1 = getTaskMonitor(t1, 2, 5, 100);
    MesosTaskMonitorObject t2u1 = getTaskMonitor(t2, 10, 5, 1024);

    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u1, t2u1));

    usagePoller.runActionOnPoll();
    cleaner.runActionOnPoll();

    Thread.sleep(2);

    // 5 seconds have elapsed, t1 has used 1 CPU the whole time = 5 + 2
    // t2 has used 2.5 CPUs the whole time =
    MesosTaskMonitorObject t1u2 = getTaskMonitor(t1, 7, 10, 125);
    MesosTaskMonitorObject t2u2 = getTaskMonitor(t2, 22.5, 10, 750);

    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u2, t2u2));

    usagePoller.runActionOnPoll();
    cleaner.runActionOnPoll();

    // check usage now
    Assert.assertEquals(3.5, usageManager.getSlaveUsage(slaveId).get(1).getCpusUsed(), 0);
    Assert.assertEquals(875, usageManager.getSlaveUsage(slaveId).get(1).getMemoryBytesUsed(), 0);
    Assert.assertEquals(2, usageManager.getSlaveUsage(slaveId).get(1).getNumTasks(), 0);

    // check task usage
    Assert.assertEquals(22.5, usageManager.getTaskUsage(t2).get(1).getCpuSeconds(), 0);
    Assert.assertEquals(10, usageManager.getTaskUsage(t2).get(0).getCpuSeconds(), 0);

    Thread.sleep(2);

    MesosTaskMonitorObject t1u3 = getTaskMonitor(t1, 8, 11, 125);
    MesosTaskMonitorObject t2u3 = getTaskMonitor(t2, 23.5, 11, 1024);

    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u3, t2u3));

    usagePoller.runActionOnPoll();
    cleaner.runActionOnPoll();

    //check that there is only 2 usages

    Assert.assertEquals(2, usageManager.getSlaveUsage(slaveId).size());
    Assert.assertEquals(3, usageManager.getTaskUsage(t1).size());
    Assert.assertEquals(3, usageManager.getTaskUsage(t2).size());

    Assert.assertEquals(10.0, usageManager.getTaskUsage(t2).get(0).getCpuSeconds(), 0);
    Assert.assertEquals(22.5, usageManager.getTaskUsage(t2).get(1).getCpuSeconds(), 0);

    Assert.assertEquals(875, usageManager.getSlaveUsage(slaveId).get(0).getMemoryBytesUsed(), 0);
    Assert.assertEquals(1149, usageManager.getSlaveUsage(slaveId).get(1).getMemoryBytesUsed(), 0);

    Assert.assertEquals(slaveId, usageManager.getAllCurrentSlaveUsage().get(0).getSlaveId());
    Assert.assertEquals(1149, usageManager.getAllCurrentSlaveUsage().get(0).getMemoryBytesUsed(), 0);

    List<SingularityTaskCurrentUsageWithId> taskCurrentUsages = usageManager.getTaskCurrentUsages(taskManager.getActiveTaskIds());

    Assert.assertEquals(2, taskCurrentUsages.size());

    List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();

    for (SingularityTaskCurrentUsageWithId taskCurrentUsage : taskCurrentUsages) {
      activeTaskIds.remove(taskCurrentUsage.getTaskId());
    }

    Assert.assertTrue(activeTaskIds.isEmpty());
  }

  @Test
  public void itTracksClusterUtilizationSimple() {
    initRequest();
    double cpuReserved = 10;
    double memMbReserved = .001;
    initFirstDeployWithResources(cpuReserved, memMbReserved);
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)));
    resourceOffers(1);

    SingularityTaskId taskId = taskManager.getActiveTaskIds().get(0);
    String t1 = taskId.getId();
    String host = slaveManager.getObjects().get(0).getHost();

    // used 8 cpu
    MesosTaskMonitorObject t1u1 = getTaskMonitor(t1, 40, getTimestampSeconds(taskId, 5), 800);
    mesosClient.setSlaveResourceUsage(host, Collections.singletonList(t1u1));
    usagePoller.runActionOnPoll();

    // used 8 cpu
    MesosTaskMonitorObject t1u2 = getTaskMonitor(t1, 80, getTimestampSeconds(taskId, 10), 850);
    mesosClient.setSlaveResourceUsage(host, Collections.singletonList(t1u2));
    usagePoller.runActionOnPoll();

    Assert.assertTrue("Couldn't find cluster utilization", usageManager.getClusterUtilization().isPresent());

    SingularityClusterUtilization utilization = usageManager.getClusterUtilization().get();

    int taskUsages = usageManager.getTaskUsage(t1).size();
    testUtilization(utilization, 2, taskUsages, cpuReserved, memMbReserved,
        0, 1, 1,
        0, 2, 223,
        0, 2, 223,
        0, 2, 223);

    Assert.assertEquals(requestId, utilization.getMaxUnderUtilizedCpuRequestId());
    Assert.assertEquals(requestId, utilization.getMaxUnderUtilizedMemBytesRequestId());
  }

  @Test
  public void itDoesntIncludePerfectlyUtilizedRequestsInClusterUtilization() {
    initRequest();
    double cpuReserved = 2;
    double memMbReserved = .001;
    initFirstDeployWithResources(cpuReserved, memMbReserved);
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)));
    resourceOffers(1);

    SingularityTaskId taskId = taskManager.getActiveTaskIds().get(0);
    String t1 = taskId.getId();
    String host = slaveManager.getObjects().get(0).getHost();

    // 2 cpus used
    MesosTaskMonitorObject t1u1 = getTaskMonitor(t1, 10, getTimestampSeconds(taskId, 5), 1024);
    mesosClient.setSlaveResourceUsage(host, Collections.singletonList(t1u1));
    usagePoller.runActionOnPoll();

    // 2 cpus used
    MesosTaskMonitorObject t1u2 = getTaskMonitor(t1, 20, getTimestampSeconds(taskId, 10), 900);
    mesosClient.setSlaveResourceUsage(host, Collections.singletonList(t1u2));
    usagePoller.runActionOnPoll();

    Assert.assertTrue("Couldn't find cluster utilization", usageManager.getClusterUtilization().isPresent());

    SingularityClusterUtilization utilization = usageManager.getClusterUtilization().get();

    int taskUsages = usageManager.getTaskUsage(t1).size();
    testUtilization(utilization, 2, taskUsages, cpuReserved, memMbReserved,
        0, 0, 1,
        0, 0, 86,
        0, 0, 86,
        0, 0, 86);

    Assert.assertEquals(requestId, utilization.getMaxUnderUtilizedMemBytesRequestId());
  }

  @Test
  public void itTracksOverusedCpuInClusterUtilization() {
    initRequest();
    double cpuReserved = 2;
    double memMbReserved = .0009;
    initFirstDeployWithResources(cpuReserved, memMbReserved);
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)));
    resourceOffers(1);

    SingularityTaskId taskId = taskManager.getActiveTaskIds().get(0);
    String t1 = taskId.getId();
    String host = slaveManager.getObjects().get(0).getHost();

    // 4 cpus used
    MesosTaskMonitorObject t1u1 = getTaskMonitor(t1, 20, getTimestampSeconds(taskId, 5), 1024);
    mesosClient.setSlaveResourceUsage(host, Collections.singletonList(t1u1));
    usagePoller.runActionOnPoll();

    // 4 cpus used
    MesosTaskMonitorObject t1u2 = getTaskMonitor(t1, 40, getTimestampSeconds(taskId, 10), 1024);
    mesosClient.setSlaveResourceUsage(host, Collections.singletonList(t1u2));
    usagePoller.runActionOnPoll();

    Assert.assertTrue("Couldn't find cluster utilization", usageManager.getClusterUtilization().isPresent());

    SingularityClusterUtilization utilization = usageManager.getClusterUtilization().get();

    int taskUsages = usageManager.getTaskUsage(t1).size();
    testUtilization(utilization, 2, taskUsages, cpuReserved, memMbReserved,
        1, 0, 0,
        2, 0, 0,
        2, 0, 0,
        2, 0, 0);

    Assert.assertEquals(requestId, utilization.getMaxOverUtilizedCpuRequestId());
  }

  @Test
  public void itCorrectlyDeletesOldUsage() {
    configuration.setNumUsageToKeep(2);
    configuration.setCheckUsageEveryMillis(TimeUnit.MINUTES.toMillis(1));
    long now = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

    // no usages exist, none are deleted
    String taskId = "newTask";
    clearUsages(taskId);

    // 1 usage exists, none are deleted
    taskId = "singleUsage";
    saveTaskUsage(taskId, now);
    clearUsages(taskId);
    Assert.assertEquals(1, usageManager.getTaskUsage(taskId).size());

    // 2 usages exist 1 min apart, none are deleted
    taskId = "twoUsages";
    saveTaskUsage(taskId, now, now + TimeUnit.MINUTES.toSeconds(1));
    clearUsages(taskId);
    Assert.assertEquals(2, usageManager.getTaskUsage(taskId).size());

    // 3 usages, oldest is deleted
    taskId = "threeUsages";
    saveTaskUsage(taskId, now, now + TimeUnit.MINUTES.toSeconds(3), now + TimeUnit.MINUTES.toSeconds(4));
    clearUsages(taskId);
    Assert.assertEquals(2, usageManager.getTaskUsage(taskId).size());
    Assert.assertEquals(now, (long) usageManager.getTaskUsage(taskId).get(0).getTimestamp());
    Assert.assertEquals(now + TimeUnit.MINUTES.toSeconds(3), (long) usageManager.getTaskUsage(taskId).get(1).getTimestamp());
  }

  @Test
  public void itCorrectlyDeterminesResourcesReservedForRequestsWithMultipleTasks() {
    initRequest();
    double cpuReserved = 10;
    double memMbReserved = .0009;
    initFirstDeployWithResources(cpuReserved, memMbReserved);
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)));
    resourceOffers(1);

    List<SingularityTaskId> taskIds = taskManager.getActiveTaskIds();
    SingularityTaskId t1 = taskIds.get(0);
    SingularityTaskId t2 = taskIds.get(1);
    String host = slaveManager.getObjects().get(0).getHost();

    // used 6 cpu
    MesosTaskMonitorObject t1u1 = getTaskMonitor(t1.getId(), 30, getTimestampSeconds(t1, 5), 800);
    // used 6 cpu
    MesosTaskMonitorObject t2u1 = getTaskMonitor(t2.getId(), 30, getTimestampSeconds(t2, 5), 800);
    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u1, t2u1));
    usagePoller.runActionOnPoll();

    // used 8 cpu
    MesosTaskMonitorObject t1u2 = getTaskMonitor(t1.getId(), 70, getTimestampSeconds(t1, 10), 850);
    // used 8 cpu
    MesosTaskMonitorObject t2u2 = getTaskMonitor(t2.getId(), 70, getTimestampSeconds(t2, 10), 850);
    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u2, t2u2));
    usagePoller.runActionOnPoll();

    Assert.assertTrue("Couldn't find cluster utilization", usageManager.getClusterUtilization().isPresent());

    SingularityClusterUtilization utilization = usageManager.getClusterUtilization().get();
    List<RequestUtilization> requestUtilizations = new ArrayList<>(usageManager.getRequestUtilizations().values());

    int t1TaskUsages = usageManager.getTaskUsage(t1.getId()).size();
    int t2TaskUsages = usageManager.getTaskUsage(t2.getId()).size();
    Assert.assertEquals(2, t1TaskUsages);
    Assert.assertEquals(2, t2TaskUsages);

    Assert.assertEquals(1, requestUtilizations.size());
    Assert.assertEquals(cpuReserved * (t1TaskUsages + t2TaskUsages), requestUtilizations.get(0).getCpuReserved(), 0);
    Assert.assertEquals(Math.round(memMbReserved * SingularitySlaveUsage.BYTES_PER_MEGABYTE * (t1TaskUsages + t2TaskUsages)), requestUtilizations.get(0).getMemBytesReserved(), 1);
  }

  @Test
  public void itCorrectlyTracksMaxAndMinUtilizedPerRequest() {
    initRequest();
    double cpuReserved = 10;
    double memMbReserved = .001;
    initFirstDeployWithResources(cpuReserved, memMbReserved);
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)));
    resourceOffers(1);

    List<SingularityTaskId> taskIds = taskManager.getActiveTaskIds();
    SingularityTaskId t1 = taskIds.get(0);
    SingularityTaskId t2 = taskIds.get(1);
    String host = slaveManager.getObjects().get(0).getHost();

    // used 10 cpu
    MesosTaskMonitorObject t1u1 = getTaskMonitor(t1.getId(), 50, getTimestampSeconds(t1, 5), 800);
    // used 8 cpu
    MesosTaskMonitorObject t2u1 = getTaskMonitor(t2.getId(), 40, getTimestampSeconds(t2, 5), 700);
    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u1, t2u1));
    usagePoller.runActionOnPoll();

    Assert.assertTrue("Couldn't find cluster utilization", usageManager.getClusterUtilization().isPresent());
    SingularityClusterUtilization utilization = usageManager.getClusterUtilization().get();
    List<RequestUtilization> requestUtilizations = new ArrayList<>(usageManager.getRequestUtilizations().values());

    int t1TaskUsages = usageManager.getTaskUsage(t1.getId()).size();
    int t2TaskUsages = usageManager.getTaskUsage(t2.getId()).size();
    Assert.assertEquals(1, t1TaskUsages);
    Assert.assertEquals(1, t2TaskUsages);
    Assert.assertEquals(1, requestUtilizations.size());

    double maxCpu = requestUtilizations.get(0).getMaxCpuUsed();
    double minCpu = requestUtilizations.get(0).getMinCpuUsed();
    long maxMemBytes = requestUtilizations.get(0).getMaxMemBytesUsed();
    long minMemBytes = requestUtilizations.get(0).getMinMemBytesUsed();
    Assert.assertEquals(10, maxCpu, 0);
    Assert.assertEquals(8, minCpu, 0);
    Assert.assertEquals(800, maxMemBytes);
    Assert.assertEquals(700, minMemBytes);

    // new max and min after 2nd run

    // used 12 cpu
    MesosTaskMonitorObject t1u2 = getTaskMonitor(t1.getId(), 110, getTimestampSeconds(t1, 10), 850);
    // used 7 cpu
    MesosTaskMonitorObject t2u2 = getTaskMonitor(t2.getId(), 75, getTimestampSeconds(t2, 10), 600);
    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u2, t2u2));
    usagePoller.runActionOnPoll();

    Assert.assertTrue("Couldn't find cluster utilization", usageManager.getClusterUtilization().isPresent());
    utilization = usageManager.getClusterUtilization().get();
    requestUtilizations = new ArrayList<>(usageManager.getRequestUtilizations().values());

    t1TaskUsages = usageManager.getTaskUsage(t1.getId()).size();
    t2TaskUsages = usageManager.getTaskUsage(t2.getId()).size();
    Assert.assertEquals(2, t1TaskUsages);
    Assert.assertEquals(2, t2TaskUsages);
    Assert.assertEquals(1, requestUtilizations.size());

    maxCpu = requestUtilizations.get(0).getMaxCpuUsed();
    minCpu = requestUtilizations.get(0).getMinCpuUsed();
    maxMemBytes = requestUtilizations.get(0).getMaxMemBytesUsed();
    minMemBytes = requestUtilizations.get(0).getMinMemBytesUsed();
    Assert.assertEquals(12, maxCpu, 0);
    Assert.assertEquals(7, minCpu, 0);
    Assert.assertEquals(850, maxMemBytes);
    Assert.assertEquals(600, minMemBytes);

    // same max and min after 3rd run

    // used 8 cpu
    MesosTaskMonitorObject t1u3 = getTaskMonitor(t1.getId(), 150, getTimestampSeconds(t1, 15), 750);
    // used 8 cpu
    MesosTaskMonitorObject t2u3 = getTaskMonitor(t2.getId(), 120, getTimestampSeconds(t2, 15), 700);
    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u3, t2u3));
    usagePoller.runActionOnPoll();

    Assert.assertTrue("Couldn't find cluster utilization", usageManager.getClusterUtilization().isPresent());
    utilization = usageManager.getClusterUtilization().get();
    requestUtilizations = new ArrayList<>(usageManager.getRequestUtilizations().values());

    t1TaskUsages = usageManager.getTaskUsage(t1.getId()).size();
    t2TaskUsages = usageManager.getTaskUsage(t2.getId()).size();
    Assert.assertEquals(3, t1TaskUsages);
    Assert.assertEquals(3, t2TaskUsages);
    Assert.assertEquals(1, requestUtilizations.size());

    maxCpu = requestUtilizations.get(0).getMaxCpuUsed();
    minCpu = requestUtilizations.get(0).getMinCpuUsed();
    maxMemBytes = requestUtilizations.get(0).getMaxMemBytesUsed();
    minMemBytes = requestUtilizations.get(0).getMinMemBytesUsed();
    Assert.assertEquals(12, maxCpu, 0);
    Assert.assertEquals(7, minCpu, 0);
    Assert.assertEquals(850, maxMemBytes);
    Assert.assertEquals(600, minMemBytes);
  }

  @Test
  public void itDelaysTaskShuffles() {
    try {
      configuration.setShuffleTasksForOverloadedSlaves(true);
      configuration.setMinutesBeforeNewTaskEligibleForShuffle(15);

      initRequest();
      initFirstDeployWithResources(configuration.getMesosConfiguration().getDefaultCpus(), configuration.getMesosConfiguration().getDefaultMemory());
      saveAndSchedule(requestManager.getRequest(requestId).get().getRequest().toBuilder().setInstances(Optional.of(3)));
      resourceOffers(1);
      SingularitySlaveUsage highUsage = new SingularitySlaveUsage(15, 10, Optional.of(10.0), 1, 1, Optional.of(30L), 1, 1, Optional.of(1024L), 1, System.currentTimeMillis(), 1, 30000, 10, 15, 15, 15, 0, 107374182);
      usageManager.saveSpecificSlaveUsageAndSetCurrent("host1", highUsage);

      SingularityTaskId taskId1 = taskManager.getActiveTaskIds().get(0);
      String t1 = taskId1.getId();
      SingularityTaskId taskId2 = taskManager.getActiveTaskIds().get(1);
      String t2 = taskId2.getId();
      SingularityTaskId taskId3 = taskManager.getActiveTaskIds().get(2);
      String t3 = taskId3.getId();
      statusUpdate(taskManager.getTask(taskId1).get(), TaskState.TASK_STARTING, Optional.of(taskId1.getStartedAt()));
      statusUpdate(taskManager.getTask(taskId2).get(), TaskState.TASK_STARTING, Optional.of(taskId2.getStartedAt()));
      statusUpdate(taskManager.getTask(taskId3).get(), TaskState.TASK_STARTING, Optional.of(taskId3.getStartedAt()));

      statusUpdate(taskManager.getTask(taskId2).get(), TaskState.TASK_RUNNING, Optional.of(taskId2.getStartedAt() - TimeUnit.MINUTES.toMillis(15)));

      // task 1 using 3 cpus
      MesosTaskMonitorObject t1u1 = getTaskMonitor(t1, 15, TimeUnit.MILLISECONDS.toSeconds(taskId1.getStartedAt()) + 5, 1024);
      // task 2 using 2 cpus
      MesosTaskMonitorObject t2u1 = getTaskMonitor(t2, 10, TimeUnit.MILLISECONDS.toSeconds(taskId2.getStartedAt()) + 5, 1024);
      // task 3 using 1 cpus
      MesosTaskMonitorObject t3u1 = getTaskMonitor(t3, 5, TimeUnit.MILLISECONDS.toSeconds(taskId3.getStartedAt()) + 5, 1024);
      mesosClient.setSlaveResourceUsage("host1", Arrays.asList(t1u1, t2u1, t3u1));
      mesosClient.setSlaveMetricsSnapshot(
          "host1",
          new MesosSlaveMetricsSnapshotObject(0, 0, 0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0)
      );

      usagePoller.runActionOnPoll();

      // Tasks are not cleaned up because they haven't been running for long enough.
      Assert.assertFalse(taskManager.getTaskCleanup(taskId1.getId()).isPresent());
      Assert.assertFalse(taskManager.getTaskCleanup(taskId3.getId()).isPresent());

      // Even though it's not the worst offender, task 2 is cleaned up because it's been running long enough.
      Assert.assertEquals(taskManager.getTaskCleanup(taskId2.getId()).get().getCleanupType(), TaskCleanupType.REBALANCE_CPU_USAGE);
    } finally {
      configuration.setShuffleTasksForOverloadedSlaves(false);
    }

  }

  @Test
  public void itCreatesTaskCleanupsWhenAMachineIsOverloaded() {
    try {
      configuration.setShuffleTasksForOverloadedSlaves(true);
      configuration.setMinutesBeforeNewTaskEligibleForShuffle(0);

      initRequest();
      initFirstDeployWithResources(configuration.getMesosConfiguration().getDefaultCpus(), configuration.getMesosConfiguration().getDefaultMemory());
      saveAndSchedule(requestManager.getRequest(requestId).get().getRequest().toBuilder().setInstances(Optional.of(3)));
      resourceOffers(1);
      SingularitySlaveUsage highUsage = new SingularitySlaveUsage(15, 10, Optional.of(10.0), 1, 1, Optional.of(30L), 1, 1, Optional.of(1024L), 1, System.currentTimeMillis(), 1, 30000, 10, 15, 15, 15, 0, 107374182);
      usageManager.saveSpecificSlaveUsageAndSetCurrent("host1", highUsage);

      SingularityTaskId taskId1 = taskManager.getActiveTaskIds().get(0);
      String t1 = taskId1.getId();
      SingularityTaskId taskId2 = taskManager.getActiveTaskIds().get(1);
      String t2 = taskId2.getId();
      SingularityTaskId taskId3 = taskManager.getActiveTaskIds().get(2);
      String t3 = taskId3.getId();
      statusUpdate(taskManager.getTask(taskId1).get(), TaskState.TASK_STARTING, Optional.of(taskId1.getStartedAt()));
      statusUpdate(taskManager.getTask(taskId2).get(), TaskState.TASK_STARTING, Optional.of(taskId2.getStartedAt()));
      statusUpdate(taskManager.getTask(taskId3).get(), TaskState.TASK_STARTING, Optional.of(taskId3.getStartedAt()));
      // task 1 using 3 cpus
      MesosTaskMonitorObject t1u1 = getTaskMonitor(t1, 15, TimeUnit.MILLISECONDS.toSeconds(taskId1.getStartedAt()) + 5, 1024);
      // task 2 using 2 cpus
      MesosTaskMonitorObject t2u1 = getTaskMonitor(t2, 10, TimeUnit.MILLISECONDS.toSeconds(taskId2.getStartedAt()) + 5, 1024);
      // task 3 using 1 cpus
      MesosTaskMonitorObject t3u1 = getTaskMonitor(t3, 5, TimeUnit.MILLISECONDS.toSeconds(taskId3.getStartedAt()) + 5, 1024);
      mesosClient.setSlaveResourceUsage("host1", Arrays.asList(t1u1, t2u1, t3u1));
      mesosClient.setSlaveMetricsSnapshot(
          "host1",
          new MesosSlaveMetricsSnapshotObject(0, 0, 0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0)
      );

      usagePoller.runActionOnPoll();

      // First task is cleaned up
      Assert.assertEquals(taskManager.getTaskCleanup(taskId1.getId()).get().getCleanupType(), TaskCleanupType.REBALANCE_CPU_USAGE);
      // Second task is not cleaned up because it is from the same request as task 1
      Assert.assertFalse(taskManager.getTaskCleanup(taskId2.getId()).isPresent());
    } finally {
      configuration.setShuffleTasksForOverloadedSlaves(false);
    }
  }

  @Test
  public void itLimitsTheNumberOfTaskCleanupsToCreate() {
    try {
      configuration.setShuffleTasksForOverloadedSlaves(true);
      configuration.setMinutesBeforeNewTaskEligibleForShuffle(0);
      configuration.setMaxTasksToShuffleTotal(1);

      initRequest();
      initFirstDeployWithResources(configuration.getMesosConfiguration().getDefaultCpus(), configuration.getMesosConfiguration().getDefaultMemory());
      saveAndSchedule(requestManager.getRequest(requestId).get().getRequest().toBuilder().setInstances(Optional.of(3)));
      resourceOffers(1);
      SingularitySlaveUsage highUsage = new SingularitySlaveUsage(15, 10, Optional.of(10.0), 1, 1, Optional.of(30L), 1, 1, Optional.of(1024L), 1, System.currentTimeMillis(), 1, 30000, 10, 15, 15, 15, 0, 107374182);
      usageManager.saveSpecificSlaveUsageAndSetCurrent("host1", highUsage);

      SingularityTaskId taskId1 = taskManager.getActiveTaskIds().get(0);
      String t1 = taskId1.getId();
      SingularityTaskId taskId2 = taskManager.getActiveTaskIds().get(1);
      String t2 = taskId2.getId();
      statusUpdate(taskManager.getTask(taskId1).get(), TaskState.TASK_STARTING, Optional.of(taskId1.getStartedAt()));
      statusUpdate(taskManager.getTask(taskId2).get(), TaskState.TASK_STARTING, Optional.of(taskId2.getStartedAt()));
      // task 1 using 3 cpus
      MesosTaskMonitorObject t1u1 = getTaskMonitor(t1, 15, TimeUnit.MILLISECONDS.toSeconds(taskId1.getStartedAt()) + 5, 1024);
      // task 2 using 2 cpus
      MesosTaskMonitorObject t2u1 = getTaskMonitor(t2, 10, TimeUnit.MILLISECONDS.toSeconds(taskId2.getStartedAt()) + 5, 1024);
      mesosClient.setSlaveResourceUsage("host1", Arrays.asList(t1u1, t2u1));
      mesosClient.setSlaveMetricsSnapshot(
          "host1",
          new MesosSlaveMetricsSnapshotObject(0, 0, 0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0)
      );

      usagePoller.runActionOnPoll();

      // First task is cleaned up
      Assert.assertEquals(taskManager.getTaskCleanup(taskId1.getId()).get().getCleanupType(), TaskCleanupType.REBALANCE_CPU_USAGE);
      // Second task doesn't get cleaned up dur to cluster wide limit
      Assert.assertFalse(taskManager.getTaskCleanup(taskId2.getId()).isPresent());
    } finally {
      configuration.setShuffleTasksForOverloadedSlaves(false);
      configuration.setMaxTasksToShuffleTotal(6);
    }
  }

  private long getTimestampSeconds(SingularityTaskId taskId, long seconds) {
    return TimeUnit.MILLISECONDS.toSeconds(taskId.getStartedAt()) + seconds;
  }

  private void saveTaskUsage(String taskId, long... times) {
    for (long time : times) {
      usageManager.saveSpecificTaskUsage(taskId, new SingularityTaskUsage(0, time, 0, 0, 0, 0, 0));
    }
  }

  private void clearUsages(String taskId) {
    usageHelper.clearOldUsage(taskId);
  }

  private void testUtilization(SingularityClusterUtilization utilization,
                               int expectedTaskUsages,
                               int actualTaskUsages,
                               double cpuReserved,
                               double memMbReserved,
                               int expectedRequestsWithOverUtilizedCpu,
                               int expectedRequestsWithUnderUtilizedCpu,
                               int expectedRequestsWithUnderUtilizedMemBytes,
                               double expectedAvgOverUtilizedCpu,
                               double expectedAvgUnderUtilizedCpu,
                               double expectedAvgUnderUtilizedMemBytes,
                               double expectedMaxOverUtilizedCpu,
                               double expectedMaxUnderUtilizedCpu,
                               long expectedMaxUnderUtilizedMemBytes,
                               double expectedMinOverUtilizedCpu,
                               double expectedMinUnderUtilizedCpu,
                               long expectedMinUnderUtilizedMemBytes) {

    Assert.assertEquals(expectedTaskUsages, actualTaskUsages);

    Assert.assertEquals(expectedRequestsWithOverUtilizedCpu, utilization.getNumRequestsWithOverUtilizedCpu());
    Assert.assertEquals(expectedRequestsWithUnderUtilizedCpu, utilization.getNumRequestsWithUnderUtilizedCpu());
    Assert.assertEquals(expectedRequestsWithUnderUtilizedMemBytes, utilization.getNumRequestsWithUnderUtilizedMemBytes());

    Assert.assertEquals(expectedAvgOverUtilizedCpu, utilization.getAvgOverUtilizedCpu(), 0);
    Assert.assertEquals(expectedAvgUnderUtilizedCpu, utilization.getAvgUnderUtilizedCpu(), 0);
    Assert.assertEquals(expectedAvgUnderUtilizedMemBytes, utilization.getAvgUnderUtilizedMemBytes(), 0);

    Assert.assertEquals(expectedMaxOverUtilizedCpu, utilization.getMaxOverUtilizedCpu(), 0);
    Assert.assertEquals(expectedMaxUnderUtilizedCpu, utilization.getMaxUnderUtilizedCpu(), 0);
    Assert.assertEquals(expectedMaxUnderUtilizedMemBytes, utilization.getMaxUnderUtilizedMemBytes());

    Assert.assertEquals(expectedMinOverUtilizedCpu, utilization.getMinOverUtilizedCpu(), 0);
    Assert.assertEquals(expectedMinUnderUtilizedCpu, utilization.getMinUnderUtilizedCpu(), 0);
    Assert.assertEquals(expectedMinUnderUtilizedMemBytes, utilization.getMinUnderUtilizedMemBytes());
  }
}
