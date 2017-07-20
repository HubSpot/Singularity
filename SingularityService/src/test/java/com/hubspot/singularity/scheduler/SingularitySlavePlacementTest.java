package com.hubspot.singularity.scheduler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.mesos.v1.Protos.TaskState;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.TaskCleanupType;

public class SingularitySlavePlacementTest extends SingularitySchedulerTestBase {

  public SingularitySlavePlacementTest() {
    super(false);
  }

  @Test
  public void testSlavePlacementSeparate() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)).setSlavePlacement(Optional.of(SlavePlacement.SEPARATE)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1"), createOffer(20, 20000, "slave1", "host1")));

    Assert.assertTrue(taskManager.getPendingTaskIds().size() == 1);
    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1")));

    Assert.assertTrue(taskManager.getPendingTaskIds().size() == 1);
    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    eventListener.taskHistoryUpdateEvent(new SingularityTaskHistoryUpdate(taskManager.getActiveTaskIds().get(0), System.currentTimeMillis(), ExtendedTaskState.TASK_CLEANING, Optional.<String>absent(), Optional.<String>absent()));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1")));

    Assert.assertTrue(taskManager.getPendingTaskIds().size() == 1);
    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave2", "host2")));

    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 2);
  }

  @Test
  public void testSlavePlacementSpread() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)).setSlavePlacement(Optional.of(SlavePlacement.SPREAD_ALL_SLAVES)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.of("rack1"))));

    // assert one Request on one slave.
    Assert.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 1);
    Assert.assertTrue(taskManager.getPendingTaskIds().size() == 0);
    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave2", "host2")));
    Assert.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);

    spreadAllPoller.runActionOnPoll();
    scheduler.drainPendingQueue();

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave2", "host2")));

    // assert Request is spread over the two slaves
    Assert.assertTrue(taskManager.getPendingTaskIds().size() == 0);
    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 2);
    Assert.assertEquals(1, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());
    Assert.assertEquals(1, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size());

    // decommission slave and kill task
    slaveManager.changeState("slave2", MachineState.FROZEN, Optional.<String>absent(), Optional.<String>absent());
    slaveManager.changeState("slave2", MachineState.STARTING_DECOMMISSION, Optional.<String>absent(), Optional.<String>absent());
    cleaner.drainCleanupQueue();
    statusUpdate(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).get(0), TaskState.TASK_KILLED);


    spreadAllPoller.runActionOnPoll();
    scheduler.drainPendingQueue();

    Assert.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testSlavePlacementOptimistic() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(7)).setSlavePlacement(Optional.of(SlavePlacement.OPTIMISTIC)));

    // Default behavior if we don't have info about other hosts that can run this task: be greedy.
    sms.resourceOffers(Arrays.asList(createOffer(2, 128 * 2, "slave1", "host1")));
    Assert.assertEquals(2, taskManager.getActiveTaskIds().size());

    // Now that at least one other host is running tasks for this request, we expect an even spread.
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave2", "host2")));
    Assert.assertEquals(5, taskManager.getActiveTaskIds().size());

    // ...and we don't allow a violation of this even spread by refusing to schedule more tasks on host2 (because it's hosting 3/5 tasks).
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave2", "host2")));
    Assert.assertEquals(5, taskManager.getActiveTaskIds().size());

    // ...but since host1 is only hosting 2/5 tasks, we will schedule more tasks on it when an offer is received.
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1")));
    Assert.assertEquals(7, taskManager.getActiveTaskIds().size());

    Map<String, List<SingularityTaskId>> tasksByHost = taskManager.getActiveTaskIdsForRequest(request.getId()).stream()
        .collect(Collectors.groupingBy(SingularityTaskId::getSanitizedHost));

    Assert.assertNotNull(tasksByHost.get("host1"));
    Assert.assertEquals(4, tasksByHost.get("host1").size());

    Assert.assertNotNull(tasksByHost.get("host2"));
    Assert.assertEquals(3, tasksByHost.get("host2").size());

  }

  @Test
  public void testSlavePlacementGreedy() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(3)).setSlavePlacement(Optional.of(SlavePlacement.GREEDY)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1")));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 3);
  }

  @Test
  public void testReservedSlaveAttribute() {
    Map<String, List<String>> reservedAttributes = new HashMap<>();
    reservedAttributes.put("reservedKey", Arrays.asList("reservedValue1"));
    configuration.setReserveSlavesWithAttributes(reservedAttributes);

    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("reservedKey", "reservedValue1"))));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave2", "host2", Optional.<String>absent(), ImmutableMap.of("reservedKey", "notAReservedValue"))));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testReservedSlaveWithMatchinRequestAttribute() {
    Map<String, List<String>> reservedAttributes = new HashMap<>();
    reservedAttributes.put("reservedKey", Arrays.asList("reservedValue1"));
    configuration.setReserveSlavesWithAttributes(reservedAttributes);

    Map<String, String> reservedAttributesMap = ImmutableMap.of("reservedKey", "reservedValue1");
    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String>absent(), reservedAttributesMap)));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)).setRequiredSlaveAttributes(Optional.of(reservedAttributesMap)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("reservedKey", "reservedValue1"))));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testAllowedSlaveAttributes() {
    Map<String, List<String>> reservedAttributes = new HashMap<>();
    reservedAttributes.put("reservedKey", Arrays.asList("reservedValue1"));
    configuration.setReserveSlavesWithAttributes(reservedAttributes);

    Map<String, String> allowedAttributes = new HashMap<>();
    allowedAttributes.put("reservedKey", "reservedValue1");

    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("reservedKey", "reservedValue1"))));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)).setAllowedSlaveAttributes(Optional.of(allowedAttributes)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("reservedKey", "reservedValue1"))));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testRequiredSlaveAttributesForRequest() {
    Map<String, String> requiredAttributes = new HashMap<>();
    requiredAttributes.put("requiredKey", "requiredValue1");

    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)).setRequiredSlaveAttributes(Optional.of(requiredAttributes)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("requiredKey", "notTheRightValue"))));
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave2", "host2", Optional.<String>absent(), ImmutableMap.of("notTheRightKey", "requiredValue1"))));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave2", "host2", Optional.<String>absent(), requiredAttributes)));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testMultipleRequiredAttributes() {
    Map<String, String> requiredAttributes = new HashMap<>();
    requiredAttributes.put("requiredKey1", "requiredValue1");
    requiredAttributes.put("requiredKey2", "requiredValue2");

    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)).setRequiredSlaveAttributes(Optional.of(requiredAttributes)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("requiredKey1", "requiredValue1"))));
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave2", "host2", Optional.<String>absent(), ImmutableMap.of("requiredKey1", "requiredValue1", "someotherkey", "someothervalue"))));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, "slave2", "host2", Optional.<String>absent(), requiredAttributes)));

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testEvenRackPlacement() {
    // Set up 3 active racks
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave1", "host1", Optional.of("rack1"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave2", "host2", Optional.of("rack2"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave3", "host3", Optional.of("rack3"))));

    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(7)).setRackSensitive(Optional.of(true)));

    // rack1 -> 1, rack2 -> 2, rack3 -> 3
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave1", "host1", Optional.of("rack1"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave2", "host2", Optional.of("rack2"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave3", "host3", Optional.of("rack3"))));

    Assert.assertEquals(3, taskManager.getActiveTaskIds().size());

    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave1", "host1", Optional.of("rack1"))));
    Assert.assertEquals(4, taskManager.getActiveTaskIds().size());

    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave1", "host1", Optional.of("rack1"))));
    Assert.assertEquals(4, taskManager.getActiveTaskIds().size());

    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave2", "host2", Optional.of("rack2"))));
    Assert.assertEquals(5, taskManager.getActiveTaskIds().size());

    // rack1 should not get a third instance until rack3 has a second
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave1", "host1", Optional.of("rack1"))));
    Assert.assertEquals(5, taskManager.getActiveTaskIds().size());

    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave3", "host3", Optional.of("rack3"))));
    Assert.assertEquals(6, taskManager.getActiveTaskIds().size());

    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave1", "host1", Optional.of("rack1"))));
    Assert.assertEquals(7, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testRackPlacementOnScaleDown() {
    try {
      configuration.setRebalanceRacksOnScaleDown(true);
      // Set up 3 active racks
      sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave1", "host1", Optional.of("rack1"))));
      sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave2", "host2", Optional.of("rack2"))));
      sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave3", "host3", Optional.of("rack3"))));

      initRequest();
      initFirstDeploy();
      saveAndSchedule(request.toBuilder().setInstances(Optional.of(7)).setRackSensitive(Optional.of(true)));

      sms.resourceOffers(Arrays.asList(createOffer(2, 256, "slave1", "host1", Optional.of("rack1"))));
      sms.resourceOffers(Arrays.asList(createOffer(2, 256, "slave2", "host2", Optional.of("rack2"))));
      sms.resourceOffers(Arrays.asList(createOffer(3, 384, "slave3", "host3", Optional.of("rack3"))));

      Assert.assertEquals(7, taskManager.getActiveTaskIds().size());

      requestResource.postRequest(request.toBuilder().setInstances(Optional.of(4)).setRackSensitive(Optional.of(true)).build());

      scheduler.drainPendingQueue();

      Assert.assertEquals(4, taskManager.getNumCleanupTasks());

      int rebalanceRackCleanups = 0;
      for (SingularityTaskCleanup cleanup : taskManager.getCleanupTasks()) {
        if (cleanup.getCleanupType() == TaskCleanupType.REBALANCE_RACKS) {
          rebalanceRackCleanups++;
        }
      }
      Assert.assertEquals(1, rebalanceRackCleanups);
      Assert.assertEquals(1, taskManager.getPendingTaskIds().size());
    } finally {
      configuration.setRebalanceRacksOnScaleDown(false);
    }
  }

  @Test
  public void testPlacementOfBounceTasks() {
    // Set up 1 active rack
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave1", "host1", Optional.of("rack1"))));

    initRequest();
    initFirstDeploy();
    SingularityRequest newRequest = request.toBuilder()
      .setInstances(Optional.of(2))
      .setRackSensitive(Optional.of(true))
      .setSlavePlacement(Optional.of(SlavePlacement.SEPARATE))
      .setAllowBounceToSameHost(Optional.of(true))
      .build();
    saveAndSchedule(newRequest.toBuilder());
    scheduler.drainPendingQueue();

    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave1", "host1", Optional.of("rack1"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave2", "host2", Optional.of("rack1"))));
    Assert.assertEquals(2, taskManager.getActiveTaskIds().size());

    requestResource.bounce(requestId, Optional.absent());
    cleaner.drainCleanupQueue();
    scheduler.drainPendingQueue();

    Assert.assertEquals(2, taskManager.getNumCleanupTasks());
    Assert.assertEquals(2, taskManager.getPendingTaskIds().size());
    Assert.assertEquals(taskManager.getCleanupTasks().get(0).getActionId().get(), taskManager.getPendingTasks().get(0).getActionId().get());

    // BOUNCE should allow a task to launch on the same host
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave1", "host1", Optional.of("rack1"))));
    Assert.assertEquals(3, taskManager.getActiveTaskIds().size());

    // But not a second one from the same bounce
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave1", "host1", Optional.of("rack1"))));
    Assert.assertEquals(3, taskManager.getActiveTaskIds().size());

    // Other pending type should not allow tasks on same host
    saveAndSchedule(newRequest.toBuilder().setInstances(Optional.of(2)));
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, "slave1", "host1", Optional.of("rack1"))));
    Assert.assertEquals(3, taskManager.getActiveTaskIds().size());
  }
}
