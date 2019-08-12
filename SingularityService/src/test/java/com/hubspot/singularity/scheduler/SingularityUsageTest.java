package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.v1.Protos.TaskState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosSlaveMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityClusterUtilization;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.data.usage.UsageManager;

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

    Assertions.assertEquals(0, usageManager.getSlaveUsage(slaveId).get().getCpusUsed(), 0);
    Assertions.assertEquals(100, usageManager.getSlaveUsage(slaveId).get().getMemoryBytesUsed(), 0);

    SingularityTaskUsage first = usageManager.getTaskUsage(firstTask.getTaskId()).get(0);

    Assertions.assertEquals(2, first.getCpuSeconds(), 0);
    Assertions.assertEquals(100, first.getMemoryTotalBytes(), 0);
    Assertions.assertEquals(5000, first.getTimestamp(), 0);
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

    Assertions.assertEquals(2, usageManager.countTasksWithUsage());
    Assertions.assertEquals(1, usageManager.getAllCurrentSlaveUsage().size());

    Assertions.assertEquals(1100, usageManager.getSlaveUsage(slaveId).get().getMemoryBytesUsed(), 0);

    // kill task one
    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_KILLED);
    killKilledTasks();

    cleaner.runActionOnPoll();

    Assertions.assertEquals(1, usageManager.countTasksWithUsage());
    Assertions.assertEquals(1, usageManager.getAllCurrentSlaveUsage().size());

    slaveManager.changeState(slaveId, MachineState.DEAD, Optional.empty(), Optional.empty());

    cleaner.runActionOnPoll();

    Assertions.assertEquals(1, usageManager.countTasksWithUsage());
    Assertions.assertEquals(0, usageManager.getAllCurrentSlaveUsage().size());
  }

  @Test
  public void testUsagePoller() throws InterruptedException {
    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)));
    resourceOffers(1);

    configuration.setNumUsageToKeep(3);
    configuration.setCheckUsageEveryMillis(1);

    List<SingularityTaskId> taskIds = taskManager.getActiveTaskIds();

    SingularityTaskId t1 = taskIds.get(0);
    SingularityTaskId t2 = taskIds.get(1);

    String slaveId = slaveManager.getObjectIds().get(0);
    String host = slaveManager.getObjects().get(0).getHost();

    MesosTaskMonitorObject t1u1 = getTaskMonitor(t1.getId(), 2, 5, 100);
    MesosTaskMonitorObject t2u1 = getTaskMonitor(t2.getId(), 10, 5, 1024);

    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u1, t2u1));

    usagePoller.runActionOnPoll();
    cleaner.runActionOnPoll();

    Thread.sleep(2);

    // 5 seconds have elapsed, t1 has used 1 CPU the whole time = 5 + 2
    // t2 has used 2.5 CPUs the whole time =
    MesosTaskMonitorObject t1u2 = getTaskMonitor(t1.getId(), 7, 10, 125);
    MesosTaskMonitorObject t2u2 = getTaskMonitor(t2.getId(), 22.5, 10, 750);

    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u2, t2u2));

    usagePoller.runActionOnPoll();
    cleaner.runActionOnPoll();

    // check usage now
    Assertions.assertEquals(3.5, usageManager.getSlaveUsage(slaveId).get().getCpusUsed(), 0);
    Assertions.assertEquals(875, usageManager.getSlaveUsage(slaveId).get().getMemoryBytesUsed(), 0);
    Assertions.assertEquals(2, usageManager.getSlaveUsage(slaveId).get().getNumTasks(), 0);

    // check task usage
    Assertions.assertEquals(22.5, usageManager.getTaskUsage(t2).get(1).getCpuSeconds(), 0);
    Assertions.assertEquals(10, usageManager.getTaskUsage(t2).get(0).getCpuSeconds(), 0);

    Thread.sleep(2);

    MesosTaskMonitorObject t1u3 = getTaskMonitor(t1.getId(), 8, 11, 125);
    MesosTaskMonitorObject t2u3 = getTaskMonitor(t2.getId(), 23.5, 11, 1024);

    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u3, t2u3));

    usagePoller.runActionOnPoll();
    cleaner.runActionOnPoll();

    //check that there is only 2 usages

    Assertions.assertEquals(3, usageManager.getTaskUsage(t1).size());
    Assertions.assertEquals(3, usageManager.getTaskUsage(t2).size());

    Assertions.assertEquals(10.0, usageManager.getTaskUsage(t2).get(0).getCpuSeconds(), 0);
    Assertions.assertEquals(22.5, usageManager.getTaskUsage(t2).get(1).getCpuSeconds(), 0);

    Assertions.assertEquals(1149, usageManager.getSlaveUsage(slaveId).get().getMemoryBytesUsed(), 0);

    Assertions.assertEquals(2, usageManager.countTasksWithUsage());
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
    SingularityTaskId t1 = taskId;
    String host = slaveManager.getObjects().get(0).getHost();

    // used 8 cpu
    MesosTaskMonitorObject t1u1 = getTaskMonitor(t1.getId(), 40, getTimestampSeconds(taskId, 5), 800);
    mesosClient.setSlaveResourceUsage(host, Collections.singletonList(t1u1));
    usagePoller.runActionOnPoll();

    // used 8 cpu
    MesosTaskMonitorObject t1u2 = getTaskMonitor(t1.getId(), 80, getTimestampSeconds(taskId, 10), 850);
    mesosClient.setSlaveResourceUsage(host, Collections.singletonList(t1u2));
    usagePoller.runActionOnPoll();

    Assertions.assertTrue(usageManager.getClusterUtilization().isPresent(), "Couldn't find cluster utilization");

    SingularityClusterUtilization utilization = usageManager.getClusterUtilization().get();

    int taskUsages = usageManager.getTaskUsage(t1).size();
    testUtilization(utilization, 2, taskUsages, cpuReserved, memMbReserved,
        0, 1, 1,
        0, 2, 223,
        0, 2, 223,
        0, 2, 223);

    Assertions.assertEquals(requestId, utilization.getMaxUnderUtilizedCpuRequestId());
    Assertions.assertEquals(requestId, utilization.getMaxUnderUtilizedMemBytesRequestId());
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
    String host = slaveManager.getObjects().get(0).getHost();

    // 2 cpus used
    MesosTaskMonitorObject t1u1 = getTaskMonitor(taskId.getId(), 10, getTimestampSeconds(taskId, 5), 1024);
    mesosClient.setSlaveResourceUsage(host, Collections.singletonList(t1u1));
    usagePoller.runActionOnPoll();

    // 2 cpus used
    MesosTaskMonitorObject t1u2 = getTaskMonitor(taskId.getId(), 20, getTimestampSeconds(taskId, 10), 900);
    mesosClient.setSlaveResourceUsage(host, Collections.singletonList(t1u2));
    usagePoller.runActionOnPoll();

    Assertions.assertTrue(usageManager.getClusterUtilization().isPresent(), "Couldn't find cluster utilization");

    SingularityClusterUtilization utilization = usageManager.getClusterUtilization().get();

    int taskUsages = usageManager.getTaskUsage(taskId).size();
    testUtilization(utilization, 2, taskUsages, cpuReserved, memMbReserved,
        0, 0, 1,
        0, 0, 86,
        0, 0, 86,
        0, 0, 86);

    Assertions.assertEquals(requestId, utilization.getMaxUnderUtilizedMemBytesRequestId());
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
    String host = slaveManager.getObjects().get(0).getHost();

    // 4 cpus used
    MesosTaskMonitorObject t1u1 = getTaskMonitor(taskId.getId(), 20, getTimestampSeconds(taskId, 5), 1024);
    mesosClient.setSlaveResourceUsage(host, Collections.singletonList(t1u1));
    usagePoller.runActionOnPoll();

    // 4 cpus used
    MesosTaskMonitorObject t1u2 = getTaskMonitor(taskId.getId(), 40, getTimestampSeconds(taskId, 10), 1024);
    mesosClient.setSlaveResourceUsage(host, Collections.singletonList(t1u2));
    usagePoller.runActionOnPoll();

    Assertions.assertTrue(usageManager.getClusterUtilization().isPresent(), "Couldn't find cluster utilization");

    SingularityClusterUtilization utilization = usageManager.getClusterUtilization().get();

    int taskUsages = usageManager.getTaskUsage(taskId).size();
    testUtilization(utilization, 2, taskUsages, cpuReserved, memMbReserved,
        1, 0, 0,
        2, 0, 0,
        2, 0, 0,
        2, 0, 0);

    Assertions.assertEquals(requestId, utilization.getMaxOverUtilizedCpuRequestId());
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

    Assertions.assertTrue(usageManager.getClusterUtilization().isPresent(), "Couldn't find cluster utilization");

    SingularityClusterUtilization utilization = usageManager.getClusterUtilization().get();
    List<RequestUtilization> requestUtilizations = new ArrayList<>(usageManager.getRequestUtilizations().values());

    int t1TaskUsages = usageManager.getTaskUsage(t1).size();
    int t2TaskUsages = usageManager.getTaskUsage(t2).size();
    Assertions.assertEquals(2, t1TaskUsages);
    Assertions.assertEquals(2, t2TaskUsages);

    Assertions.assertEquals(1, requestUtilizations.size());
    Assertions.assertEquals(cpuReserved * (t1TaskUsages + t2TaskUsages), requestUtilizations.get(0).getCpuReserved(), 0);
    Assertions.assertEquals(Math.round(memMbReserved * SingularitySlaveUsage.BYTES_PER_MEGABYTE * (t1TaskUsages + t2TaskUsages)), requestUtilizations.get(0).getMemBytesReserved(), 1);
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

    Assertions.assertTrue(usageManager.getClusterUtilization().isPresent(), "Couldn't find cluster utilization");
    SingularityClusterUtilization utilization = usageManager.getClusterUtilization().get();
    List<RequestUtilization> requestUtilizations = new ArrayList<>(usageManager.getRequestUtilizations().values());

    int t1TaskUsages = usageManager.getTaskUsage(t1).size();
    int t2TaskUsages = usageManager.getTaskUsage(t2).size();
    Assertions.assertEquals(1, t1TaskUsages);
    Assertions.assertEquals(1, t2TaskUsages);
    Assertions.assertEquals(1, requestUtilizations.size());

    double maxCpu = requestUtilizations.get(0).getMaxCpuUsed();
    double minCpu = requestUtilizations.get(0).getMinCpuUsed();
    long maxMemBytes = requestUtilizations.get(0).getMaxMemBytesUsed();
    long minMemBytes = requestUtilizations.get(0).getMinMemBytesUsed();
    Assertions.assertEquals(10, maxCpu, 0);
    Assertions.assertEquals(8, minCpu, 0);
    Assertions.assertEquals(800, maxMemBytes);
    Assertions.assertEquals(700, minMemBytes);

    // new max and min after 2nd run

    // used 12 cpu
    MesosTaskMonitorObject t1u2 = getTaskMonitor(t1.getId(), 110, getTimestampSeconds(t1, 10), 850);
    // used 7 cpu
    MesosTaskMonitorObject t2u2 = getTaskMonitor(t2.getId(), 75, getTimestampSeconds(t2, 10), 600);
    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u2, t2u2));
    usagePoller.runActionOnPoll();

    Assertions.assertTrue(usageManager.getClusterUtilization().isPresent(), "Couldn't find cluster utilization");
    utilization = usageManager.getClusterUtilization().get();
    requestUtilizations = new ArrayList<>(usageManager.getRequestUtilizations().values());

    t1TaskUsages = usageManager.getTaskUsage(t1).size();
    t2TaskUsages = usageManager.getTaskUsage(t2).size();
    Assertions.assertEquals(2, t1TaskUsages);
    Assertions.assertEquals(2, t2TaskUsages);
    Assertions.assertEquals(1, requestUtilizations.size());

    maxCpu = requestUtilizations.get(0).getMaxCpuUsed();
    minCpu = requestUtilizations.get(0).getMinCpuUsed();
    maxMemBytes = requestUtilizations.get(0).getMaxMemBytesUsed();
    minMemBytes = requestUtilizations.get(0).getMinMemBytesUsed();
    Assertions.assertEquals(12, maxCpu, 0);
    Assertions.assertEquals(7, minCpu, 0);
    Assertions.assertEquals(850, maxMemBytes);
    Assertions.assertEquals(600, minMemBytes);

    // same max and min after 3rd run

    // used 8 cpu
    MesosTaskMonitorObject t1u3 = getTaskMonitor(t1.getId(), 150, getTimestampSeconds(t1, 15), 750);
    // used 8 cpu
    MesosTaskMonitorObject t2u3 = getTaskMonitor(t2.getId(), 120, getTimestampSeconds(t2, 15), 700);
    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u3, t2u3));
    usagePoller.runActionOnPoll();

    Assertions.assertTrue(usageManager.getClusterUtilization().isPresent(), "Couldn't find cluster utilization");
    utilization = usageManager.getClusterUtilization().get();
    requestUtilizations = new ArrayList<>(usageManager.getRequestUtilizations().values());

    t1TaskUsages = usageManager.getTaskUsage(t1).size();
    t2TaskUsages = usageManager.getTaskUsage(t2).size();
    Assertions.assertEquals(3, t1TaskUsages);
    Assertions.assertEquals(3, t2TaskUsages);
    Assertions.assertEquals(1, requestUtilizations.size());

    maxCpu = requestUtilizations.get(0).getMaxCpuUsed();
    minCpu = requestUtilizations.get(0).getMinCpuUsed();
    maxMemBytes = requestUtilizations.get(0).getMaxMemBytesUsed();
    minMemBytes = requestUtilizations.get(0).getMinMemBytesUsed();
    Assertions.assertEquals(12, maxCpu, 0);
    Assertions.assertEquals(7, minCpu, 0);
    Assertions.assertEquals(850, maxMemBytes);
    Assertions.assertEquals(600, minMemBytes);
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
      SingularitySlaveUsage highUsage = new SingularitySlaveUsage(15, 10, Optional.of(10.0), 1, 1, Optional.of(30L), 1, 1, Optional.of(1024L), 1, System.currentTimeMillis(), 200000, 30000, 10, 15, 15, 15, 0, 107374182);
      usageManager.saveCurrentSlaveUsage(new SingularitySlaveUsageWithId(highUsage, "host1"));

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
          new MesosSlaveMetricsSnapshotObject(0, 0, 0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 200000, 0, 30000, 0, 0, 0, 15, 0, 0, 0, 0)
      );

      usagePoller.runActionOnPoll();

      // Tasks are not cleaned up because they haven't been running for long enough.
      Assertions.assertFalse(taskManager.getTaskCleanup(taskId1.getId()).isPresent());
      Assertions.assertFalse(taskManager.getTaskCleanup(taskId3.getId()).isPresent());

      // Even though it's not the worst offender, task 2 is cleaned up because it's been running long enough.
      Assertions.assertEquals(TaskCleanupType.REBALANCE_CPU_USAGE, taskManager.getTaskCleanup(taskId2.getId()).get().getCleanupType());
    } finally {
      configuration.setShuffleTasksForOverloadedSlaves(false);
    }

  }

  @Test
  public void itCreatesTaskCleanupsWhenAMachineIsOverloadedOnMemory() {
    try {
      configuration.setShuffleTasksForOverloadedSlaves(true);
      configuration.setMinutesBeforeNewTaskEligibleForShuffle(0);
      configuration.setShuffleTasksWhenSlaveMemoryUtilizationPercentageExceeds(0.90);

      initRequest();
      initFirstDeployWithResources(configuration.getMesosConfiguration().getDefaultCpus(), configuration.getMesosConfiguration().getDefaultMemory());
      saveAndSchedule(requestManager.getRequest(requestId).get().getRequest().toBuilder().setInstances(Optional.of(3)));
      resourceOffers(1);
      SingularitySlaveUsage highUsage = new SingularitySlaveUsage(10, 10, Optional.of(10.0), 1, 1, Optional.of(30L), 1, 1, Optional.of(1024L), 1, System.currentTimeMillis(), 200000, 10000, 10, 10, 10, 10, 0, 107374182);
      usageManager.saveCurrentSlaveUsage(new SingularitySlaveUsageWithId(highUsage, "host1"));

      SingularityTaskId taskId1 = taskManager.getActiveTaskIds().get(0);
      String t1 = taskId1.getId();
      SingularityTaskId taskId2 = taskManager.getActiveTaskIds().get(1);
      String t2 = taskId2.getId();
      SingularityTaskId taskId3 = taskManager.getActiveTaskIds().get(2);
      String t3 = taskId3.getId();
      statusUpdate(taskManager.getTask(taskId1).get(), TaskState.TASK_STARTING, Optional.of(taskId1.getStartedAt()));
      statusUpdate(taskManager.getTask(taskId2).get(), TaskState.TASK_STARTING, Optional.of(taskId2.getStartedAt()));
      statusUpdate(taskManager.getTask(taskId3).get(), TaskState.TASK_STARTING, Optional.of(taskId3.getStartedAt()));
      // task 1 using 3G mem
      MesosTaskMonitorObject t1u1 = getTaskMonitor(t1, 2, TimeUnit.MILLISECONDS.toSeconds(taskId1.getStartedAt()) + 5, 95000);
      // task 2 using 2G mem
      MesosTaskMonitorObject t2u1 = getTaskMonitor(t2, 5, TimeUnit.MILLISECONDS.toSeconds(taskId2.getStartedAt()) + 5, 63333);
      // task 3 using 1G mem
      MesosTaskMonitorObject t3u1 = getTaskMonitor(t3, 5, TimeUnit.MILLISECONDS.toSeconds(taskId3.getStartedAt()) + 5, 31667);
      mesosClient.setSlaveResourceUsage("host1", Arrays.asList(t1u1, t2u1, t3u1));
      mesosClient.setSlaveMetricsSnapshot(
          "host1",
          new MesosSlaveMetricsSnapshotObject(0, 0, 0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 200000, 0, 10000, 0, 0, 0, 10, 0, 0, 0, 0)
      );

      usagePoller.runActionOnPoll();

      // First task is cleaned up
      Assertions.assertEquals(TaskCleanupType.REBALANCE_MEMORY_USAGE, taskManager.getTaskCleanup(taskId1.getId()).get().getCleanupType());
      // Second task is not cleaned up because it is from the same request as task 1
      Assertions.assertFalse(taskManager.getTaskCleanup(taskId2.getId()).isPresent());
    } finally {
      configuration.setShuffleTasksForOverloadedSlaves(false);
    }
  }

  @Test
  public void itCreatesTaskCleanupsWhenAMachineIsOverloadedOnCpu() {
    try {
      configuration.setShuffleTasksForOverloadedSlaves(true);
      configuration.setMinutesBeforeNewTaskEligibleForShuffle(0);

      initRequest();
      initFirstDeployWithResources(configuration.getMesosConfiguration().getDefaultCpus(), configuration.getMesosConfiguration().getDefaultMemory());
      saveAndSchedule(requestManager.getRequest(requestId).get().getRequest().toBuilder().setInstances(Optional.of(3)));
      resourceOffers(1);
      SingularitySlaveUsage highUsage = new SingularitySlaveUsage(15, 10, Optional.of(10.0), 1, 1, Optional.of(30L), 1, 1, Optional.of(1024L), 1, System.currentTimeMillis(), 200000, 30000, 10, 15, 15, 15, 0, 107374182);
      usageManager.saveCurrentSlaveUsage(new SingularitySlaveUsageWithId(highUsage, "host1"));

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
          new MesosSlaveMetricsSnapshotObject(0, 0, 0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 200000, 0, 30000, 0, 0, 0, 15, 0, 0, 0, 0)
      );

      usagePoller.runActionOnPoll();

      // First task is cleaned up
      Assertions.assertEquals(TaskCleanupType.REBALANCE_CPU_USAGE, taskManager.getTaskCleanup(taskId1.getId()).get().getCleanupType());
      // Second task is not cleaned up because it is from the same request as task 1
      Assertions.assertFalse(taskManager.getTaskCleanup(taskId2.getId()).isPresent());
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
      SingularitySlaveUsage highUsage = new SingularitySlaveUsage(15, 10, Optional.of(10.0), 1, 1, Optional.of(30L), 1, 1, Optional.of(1024L), 1, System.currentTimeMillis(), 200000, 30000, 10, 15, 15, 15, 0, 107374182);
      usageManager.saveCurrentSlaveUsage(new SingularitySlaveUsageWithId(highUsage, "host1"));

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
          new MesosSlaveMetricsSnapshotObject(0, 0, 0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0, 10.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15, 200000, 0, 30000, 0, 0, 0, 15, 0, 0, 0, 0)
      );

      usagePoller.runActionOnPoll();

      // First task is cleaned up
      Assertions.assertEquals(TaskCleanupType.REBALANCE_CPU_USAGE, taskManager.getTaskCleanup(taskId1.getId()).get().getCleanupType());
      // Second task doesn't get cleaned up dur to cluster wide limit
      Assertions.assertFalse(taskManager.getTaskCleanup(taskId2.getId()).isPresent());
    } finally {
      configuration.setShuffleTasksForOverloadedSlaves(false);
      configuration.setMaxTasksToShuffleTotal(6);
    }
  }

  private double getTimestampSeconds(SingularityTaskId taskId, long seconds) {
    return ((double) taskId.getStartedAt() + seconds * 1000) / 1000;
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

    Assertions.assertEquals(expectedTaskUsages, actualTaskUsages);

    Assertions.assertEquals(expectedRequestsWithOverUtilizedCpu, utilization.getNumRequestsWithOverUtilizedCpu());
    Assertions.assertEquals(expectedRequestsWithUnderUtilizedCpu, utilization.getNumRequestsWithUnderUtilizedCpu());
    Assertions.assertEquals(expectedRequestsWithUnderUtilizedMemBytes, utilization.getNumRequestsWithUnderUtilizedMemBytes());

    Assertions.assertEquals(expectedAvgOverUtilizedCpu, utilization.getAvgOverUtilizedCpu(), 0);
    Assertions.assertEquals(expectedAvgUnderUtilizedCpu, utilization.getAvgUnderUtilizedCpu(), 0);
    Assertions.assertEquals(expectedAvgUnderUtilizedMemBytes, utilization.getAvgUnderUtilizedMemBytes(), 0);

    Assertions.assertEquals(expectedMaxOverUtilizedCpu, utilization.getMaxOverUtilizedCpu(), 0);
    Assertions.assertEquals(expectedMaxUnderUtilizedCpu, utilization.getMaxUnderUtilizedCpu(), 0);
    Assertions.assertEquals(expectedMaxUnderUtilizedMemBytes, utilization.getMaxUnderUtilizedMemBytes());

    Assertions.assertEquals(expectedMinOverUtilizedCpu, utilization.getMinOverUtilizedCpu(), 0);
    Assertions.assertEquals(expectedMinUnderUtilizedCpu, utilization.getMinUnderUtilizedCpu(), 0);
    Assertions.assertEquals(expectedMinUnderUtilizedMemBytes, utilization.getMinUnderUtilizedMemBytes());
  }
}
