package com.hubspot.singularity.scheduler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.mesos.Protos.TaskState;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.mesos.json.MesosTaskStatisticsObject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCurrentUsageWithId;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.data.UsageManager;

public class SingularityUsageTest extends SingularitySchedulerTestBase {

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
    MesosTaskMonitorObject usage = new MesosTaskMonitorObject(null, null, null, firstTask.getTaskId().getId(), getStatistics(2, 5, 100));

    mesosClient.setSlaveResourceUsage(hostname, Collections.singletonList(usage));

    usagePoller.runActionOnPoll();

    String slaveId = firstTask.getSlaveId().getValue();

    List<String> slaves = usageManager.getSlavesWithUsage();

    Assert.assertEquals(1, slaves.size());
    Assert.assertEquals(slaves.get(0), slaveId);

    Assert.assertEquals(0, usageManager.getSlaveUsage(slaveId).get(0).getCpusUsed(), 0);
    Assert.assertEquals(100, usageManager.getSlaveUsage(slaveId).get(0).getMemoryBytesUsed());

    SingularityTaskUsage first = usageManager.getTaskUsage(firstTask.getTaskId().getId()).get(0);

    Assert.assertEquals(2, first.getCpuSeconds(), 0);
    Assert.assertEquals(100, first.getMemoryRssBytes(), 0);
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

    MesosTaskMonitorObject t1u1 = new MesosTaskMonitorObject(null, null, null, t1, getStatistics(2, 5, 100));
    MesosTaskMonitorObject t2u1 = new MesosTaskMonitorObject(null, null, null, t2, getStatistics(10, 5, 1000));

    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u1, t2u1));

    usagePoller.runActionOnPoll();
    cleaner.runActionOnPoll();

    Assert.assertEquals(2, usageManager.getTasksWithUsage().size());
    Assert.assertEquals(1, usageManager.getSlavesWithUsage().size());

    Assert.assertEquals(1100, usageManager.getAllCurrentSlaveUsage().get(0).getMemoryBytesUsed());

    // kill task one
    statusUpdate(taskManager.getActiveTasks().get(0), TaskState.TASK_KILLED);
    killKilledTasks();

    cleaner.runActionOnPoll();

    Assert.assertEquals(1, usageManager.getTasksWithUsage().size());
    Assert.assertEquals(1, usageManager.getSlavesWithUsage().size());

    slaveManager.changeState(slaveId, MachineState.DEAD, Optional.<String> absent(), Optional.<String> absent());

    cleaner.runActionOnPoll();

    Assert.assertEquals(1, usageManager.getTasksWithUsage().size());
    Assert.assertEquals(0, usageManager.getSlavesWithUsage().size());
  }

  @Test
  public void testUsagePollerComplex() throws InterruptedException {
    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)));
    resourceOffers(1);

    configuration.setNumUsageToKeep(2);

    List<SingularityTaskId> taskIds = taskManager.getActiveTaskIds();

    String t1 = taskIds.get(0).getId();
    String t2 = taskIds.get(1).getId();

    String slaveId = slaveManager.getObjectIds().get(0);
    String host = slaveManager.getObjects().get(0).getHost();

    MesosTaskMonitorObject t1u1 = new MesosTaskMonitorObject(null, null, null, t1, getStatistics(2, 5, 100));
    MesosTaskMonitorObject t2u1 = new MesosTaskMonitorObject(null, null, null, t2, getStatistics(10, 5, 1000));

    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u1, t2u1));

    usagePoller.runActionOnPoll();
    cleaner.runActionOnPoll();

    Thread.sleep(2);

    // 5 seconds have elapsed, t1 has used 1 CPU the whole time = 5 + 2
    // t2 has used 2.5 CPUs the whole time =
    MesosTaskMonitorObject t1u2 = new MesosTaskMonitorObject(null, null, null, t1, getStatistics(7, 10, 125));
    MesosTaskMonitorObject t2u2 = new MesosTaskMonitorObject(null, null, null, t2, getStatistics(22.5, 10, 750));

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

    MesosTaskMonitorObject t1u3 = new MesosTaskMonitorObject(null, null, null, t1, getStatistics(8, 11, 125));
    MesosTaskMonitorObject t2u3 = new MesosTaskMonitorObject(null, null, null, t2, getStatistics(23.5, 11, 1000));

    mesosClient.setSlaveResourceUsage(host, Arrays.asList(t1u3, t2u3));

    usagePoller.runActionOnPoll();
    cleaner.runActionOnPoll();

    //check that there is only 2 usages

    Assert.assertEquals(2, usageManager.getSlaveUsage(slaveId).size());
    Assert.assertEquals(2, usageManager.getTaskUsage(t1).size());
    Assert.assertEquals(2, usageManager.getTaskUsage(t2).size());

    Assert.assertEquals(22.5, usageManager.getTaskUsage(t2).get(0).getCpuSeconds(), 0);
    Assert.assertEquals(23.5, usageManager.getTaskUsage(t2).get(1).getCpuSeconds(), 0);

    Assert.assertEquals(875, usageManager.getSlaveUsage(slaveId).get(0).getMemoryBytesUsed(), 0);
    Assert.assertEquals(1125, usageManager.getSlaveUsage(slaveId).get(1).getMemoryBytesUsed(), 0);

    Assert.assertEquals(slaveId, usageManager.getAllCurrentSlaveUsage().get(0).getSlaveId());
    Assert.assertEquals(1125, usageManager.getAllCurrentSlaveUsage().get(0).getMemoryBytesUsed());

    List<SingularityTaskCurrentUsageWithId> taskCurrentUsages = usageManager.getTaskCurrentUsages(taskManager.getActiveTaskIds());

    Assert.assertEquals(2, taskCurrentUsages.size());

    List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();

    for (SingularityTaskCurrentUsageWithId taskCurrentUsage : taskCurrentUsages) {
      activeTaskIds.remove(taskCurrentUsage.getTaskId());
    }

    Assert.assertTrue(activeTaskIds.isEmpty());
  }

  private MesosTaskStatisticsObject getStatistics(double cpuSecs, double timestamp, long memBytes) {
    return new MesosTaskStatisticsObject(1, 0L, 0L, 0, 0, cpuSecs, 0L, 0L, 0L, 0L, memBytes, timestamp);
  }

}
