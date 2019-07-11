package com.hubspot.singularity.scheduler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.mesos.v1.Protos.TaskState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRunNowRequestBuilder;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.TaskCleanupType;

public class  SingularitySlavePlacementTest extends SingularitySchedulerTestBase {

  public SingularitySlavePlacementTest() {
    super(false);
  }

  @Test
  public void testSlavePlacementSeparate() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)).setSlavePlacement(Optional.of(SlavePlacement.SEPARATE)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1"), createOffer(20, 20000, 50000, "slave1", "host1")));

    Assertions.assertTrue(taskManager.getPendingTaskIds().size() == 1);
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1")));

    Assertions.assertTrue(taskManager.getPendingTaskIds().size() == 1);
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1")));

    Assertions.assertTrue(taskManager.getPendingTaskIds().size() == 1);
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave2", "host2")));

    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 2);
  }

  @Test
  public void testSlavePlacementSpread() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)).setSlavePlacement(Optional.of(SlavePlacement.SPREAD_ALL_SLAVES)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1", Optional.of("rack1"))));

    // assert one Request on one slave.
    Assertions.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 1);
    Assertions.assertTrue(taskManager.getPendingTaskIds().size() == 0);
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave2", "host2")));
    Assertions.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);

    spreadAllPoller.runActionOnPoll();
    scheduler.drainPendingQueue();

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave2", "host2")));

    // assert Request is spread over the two slaves
    Assertions.assertTrue(taskManager.getPendingTaskIds().size() == 0);
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 2);
    Assertions.assertEquals(1, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());
    Assertions.assertEquals(1, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size());

    // decommission slave and kill task
    slaveManager.changeState("slave2", MachineState.FROZEN, Optional.<String>absent(), Optional.<String>absent());
    slaveManager.changeState("slave2", MachineState.STARTING_DECOMMISSION, Optional.<String>absent(), Optional.<String>absent());
    cleaner.drainCleanupQueue();
    statusUpdate(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).get(0), TaskState.TASK_KILLED);


    spreadAllPoller.runActionOnPoll();
    scheduler.drainPendingQueue();

    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testSlavePlacementOptimistic() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(20)).setSlavePlacement(Optional.of(SlavePlacement.OPTIMISTIC)));

    // Default behavior if we don't have info about other hosts that can run this task: be greedy.
    sms.resourceOffers(Arrays.asList(createOffer(2, 128 * 2, 1024 * 2, "slave1", "host1")));
    Assertions.assertEquals(2, taskManager.getActiveTaskIds().size());

    // Now that at least one other host is running tasks for this request, we expect an even-ish spread,
    // but because we have many tasks pending, we allow quite a bit of unevenness.
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave2", "host2")));
    Assertions.assertEquals(13, taskManager.getActiveTaskIds().size());

    // ...but now we won't schedule more tasks on host2, because it's hosting a disproportionate number of tasks.
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave2", "host2")));
    Assertions.assertEquals(13, taskManager.getActiveTaskIds().size());

    // ...but since host1 is only hosting 2 tasks, we will schedule more tasks on it when an offer is received.
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1")));
    Assertions.assertEquals(20, taskManager.getActiveTaskIds().size());

    Map<String, List<SingularityTaskId>> tasksByHost = taskManager.getActiveTaskIdsForRequest(request.getId()).stream()
        .collect(Collectors.groupingBy(SingularityTaskId::getSanitizedHost));

    Assertions.assertNotNull(tasksByHost.get("host1"));
    Assertions.assertEquals(9, tasksByHost.get("host1").size());

    Assertions.assertNotNull(tasksByHost.get("host2"));
    Assertions.assertEquals(11, tasksByHost.get("host2").size());

  }

  @Test
  public void testSlavePlacementGreedy() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(3)).setSlavePlacement(Optional.of(SlavePlacement.GREEDY)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1")));

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 3);
  }

  @Test
  public void testReservedSlaveAttribute() {
    Map<String, List<String>> reservedAttributes = new HashMap<>();
    reservedAttributes.put("reservedKey", Arrays.asList("reservedValue1"));
    configuration.setReserveSlavesWithAttributes(reservedAttributes);

    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("reservedKey", "reservedValue1"))));

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave2", "host2", Optional.<String>absent(), ImmutableMap.of("reservedKey", "notAReservedValue"))));

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);
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

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1", Optional.<String>absent(), reservedAttributesMap)));

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)).setRequiredSlaveAttributes(Optional.of(reservedAttributesMap)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("reservedKey", "reservedValue1"))));

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);
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

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("reservedKey", "reservedValue1"))));

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)).setAllowedSlaveAttributes(Optional.of(allowedAttributes)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("reservedKey", "reservedValue1"))));

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testRequiredSlaveAttributesForRequest() {
    Map<String, String> requiredAttributes = new HashMap<>();
    requiredAttributes.put("requiredKey", "requiredValue1");

    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)).setRequiredSlaveAttributes(Optional.of(requiredAttributes)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("requiredKey", "notTheRightValue"))));
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave2", "host2", Optional.<String>absent(), ImmutableMap.of("notTheRightKey", "requiredValue1"))));

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave2", "host2", Optional.<String>absent(), requiredAttributes)));

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testRequiredSlaveAttributeOverrides() {
    Map<String, String> requiredAttributes = new HashMap<>();
    requiredAttributes.put("requiredKey", "requiredValue1");

    initOnDemandRequest();
    initFirstDeploy();

    requestResource.scheduleImmediately(
        singularityUser,
        requestId,
        new SingularityRunNowRequestBuilder()
            .setRequiredSlaveAttributeOverrides(requiredAttributes)
            .build()
    );

    scheduler.drainPendingQueue();

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("requiredKey", "notTheRightValue"))));
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave2", "host2", Optional.<String>absent(), ImmutableMap.of("notTheRightKey", "requiredValue1"))));

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave2", "host2", Optional.<String>absent(), requiredAttributes)));

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testMultipleRequiredAttributes() {
    Map<String, String> requiredAttributes = new HashMap<>();
    requiredAttributes.put("requiredKey1", "requiredValue1");
    requiredAttributes.put("requiredKey2", "requiredValue2");

    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)).setRequiredSlaveAttributes(Optional.of(requiredAttributes)));

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("requiredKey1", "requiredValue1"))));
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave2", "host2", Optional.<String>absent(), ImmutableMap.of("requiredKey1", "requiredValue1", "someotherkey", "someothervalue"))));

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave2", "host2", Optional.<String>absent(), requiredAttributes)));

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testEvenRackPlacement() {
    // Set up 3 active racks
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave1", "host1", Optional.of("rack1"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave2", "host2", Optional.of("rack2"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave3", "host3", Optional.of("rack3"))));

    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(7)).setRackSensitive(Optional.of(true)));

    // rack1 -> 1, rack2 -> 2, rack3 -> 3
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave1", "host1", Optional.of("rack1"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave2", "host2", Optional.of("rack2"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave3", "host3", Optional.of("rack3"))));

    Assertions.assertEquals(3, taskManager.getActiveTaskIds().size());

    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave1", "host1", Optional.of("rack1"))));
    Assertions.assertEquals(4, taskManager.getActiveTaskIds().size());

    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave1", "host1", Optional.of("rack1"))));
    Assertions.assertEquals(4, taskManager.getActiveTaskIds().size());

    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave2", "host2", Optional.of("rack2"))));
    Assertions.assertEquals(5, taskManager.getActiveTaskIds().size());

    // rack1 should not get a third instance until rack3 has a second
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave1", "host1", Optional.of("rack1"))));
    Assertions.assertEquals(5, taskManager.getActiveTaskIds().size());

    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave3", "host3", Optional.of("rack3"))));
    Assertions.assertEquals(6, taskManager.getActiveTaskIds().size());

    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave1", "host1", Optional.of("rack1"))));
    Assertions.assertEquals(7, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testRackPlacementOnScaleDown() {
    try {
      configuration.setRebalanceRacksOnScaleDown(true);
      // Set up 3 active racks
      sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave1", "host1", Optional.of("rack1"))));
      sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave2", "host2", Optional.of("rack2"))));
      sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave3", "host3", Optional.of("rack3"))));

      initRequest();
      initFirstDeploy();
      saveAndSchedule(request.toBuilder().setInstances(Optional.of(7)).setRackSensitive(Optional.of(true)));

      sms.resourceOffers(Arrays.asList(createOffer(2, 256, 2048, "slave1", "host1", Optional.of("rack1"))));
      sms.resourceOffers(Arrays.asList(createOffer(2, 256, 2048, "slave2", "host2", Optional.of("rack2"))));
      sms.resourceOffers(Arrays.asList(createOffer(3, 384, 3072, "slave3", "host3", Optional.of("rack3"))));

      Assertions.assertEquals(7, taskManager.getActiveTaskIds().size());

      requestResource.postRequest(request.toBuilder().setInstances(Optional.of(4)).setRackSensitive(Optional.of(true)).build(), singularityUser);

      scheduler.drainPendingQueue();

      Assertions.assertEquals(4, taskManager.getNumCleanupTasks());

      int rebalanceRackCleanups = 0;
      for (SingularityTaskCleanup cleanup : taskManager.getCleanupTasks()) {
        if (cleanup.getCleanupType() == TaskCleanupType.REBALANCE_RACKS) {
          rebalanceRackCleanups++;
        }
      }
      Assertions.assertEquals(1, rebalanceRackCleanups);
      Assertions.assertEquals(1, taskManager.getPendingTaskIds().size());
    } finally {
      configuration.setRebalanceRacksOnScaleDown(false);
    }
  }

  @Test
  public void testPlacementOfBounceTasks() {
    // Set up 1 active rack
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave1", "host1", Optional.of("rack1"))));

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

    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave1", "host1", Optional.of("rack1"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave2", "host2", Optional.of("rack1"))));
    Assertions.assertEquals(2, taskManager.getActiveTaskIds().size());

    requestResource.bounce(requestId, Optional.absent(), singularityUser);
    cleaner.drainCleanupQueue();
    scheduler.drainPendingQueue();

    Assertions.assertEquals(2, taskManager.getNumCleanupTasks());
    Assertions.assertEquals(2, taskManager.getPendingTaskIds().size());
    Assertions.assertEquals(taskManager.getCleanupTasks().get(0).getActionId().get(), taskManager.getPendingTasks().get(0).getActionId().get());

    // BOUNCE should allow a task to launch on the same host
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave1", "host1", Optional.of("rack1"))));
    Assertions.assertEquals(3, taskManager.getActiveTaskIds().size());

    // But not a second one from the same bounce
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave1", "host1", Optional.of("rack1"))));
    Assertions.assertEquals(3, taskManager.getActiveTaskIds().size());

    // Other pending type should not allow tasks on same host
    saveAndSchedule(newRequest.toBuilder().setInstances(Optional.of(2)));
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave1", "host1", Optional.of("rack1"))));
    Assertions.assertEquals(3, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testSlaveAttributeMinimumsAreNotForciblyViolated() {
    Map<String, List<String>> reservedAttributes = new HashMap<>();
    reservedAttributes.put("instance_lifecycle_type", Arrays.asList("spot"));
    configuration.setReserveSlavesWithAttributes(reservedAttributes);

    Map<String, String> allowedAttributes = new HashMap<>();
    allowedAttributes.put("instance_lifecycle_type", "spot");

    Map<String, Map<String, Integer>> attributeMinimums = new HashMap<>();
    attributeMinimums.put("instance_lifecycle_type", ImmutableMap.of("non_spot", 70));

    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder()
        .setInstances(Optional.of(10))
        .setAllowedSlaveAttributes(Optional.of(allowedAttributes))
        .setSlaveAttributeMinimums(Optional.of(attributeMinimums)));

    // The schedule should only accept as many "spot" instances so as to not force a violation of the minimum "non_spot" instances
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("instance_lifecycle_type", "spot"))));
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 3);
    Assertions.assertEquals(3, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());

    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave2", "host2", Optional.<String>absent(), ImmutableMap.of("instance_lifecycle_type", "non_spot"))));
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 10);
    Assertions.assertEquals(3, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());
    Assertions.assertEquals(7, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size());
  }

  @Test
  public void testSlaveAttributeMinimumsCanBeExceeded() {
    Map<String, List<String>> reservedAttributes = new HashMap<>();
    reservedAttributes.put("instance_lifecycle_type", Arrays.asList("spot"));
    configuration.setReserveSlavesWithAttributes(reservedAttributes);

    Map<String, String> allowedAttributes = new HashMap<>();
    allowedAttributes.put("instance_lifecycle_type", "spot");

    Map<String, Map<String, Integer>> attributeMinimums = new HashMap<>();
    attributeMinimums.put("instance_lifecycle_type", ImmutableMap.of("non_spot", 70));

    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder()
        .setInstances(Optional.of(10))
        .setAllowedSlaveAttributes(Optional.of(allowedAttributes))
        .setSlaveAttributeMinimums(Optional.of(attributeMinimums)));

    // Ensure we can go over the minimum if there are enough resources available
    sms.resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("instance_lifecycle_type", "non_spot"))));
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 10);
    Assertions.assertEquals(10, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());
  }

//  @Test
//  public void testSlaveAttributesAreRedistributedOnScaleDown() {
//    Map<String, List<String>> reservedAttributes = new HashMap<>();
//    reservedAttributes.put("instance_lifecycle_type", Arrays.asList("spot"));
//    configuration.setReserveSlavesWithAttributes(reservedAttributes);
//
//    Map<String, String> allowedAttributes = new HashMap<>();
//    allowedAttributes.put("instance_lifecycle_type", "spot");
//
//    Map<String, Map<String, Integer>> attributeMinimums = new HashMap<>();
//    attributeMinimums.put("instance_lifecycle_type", ImmutableMap.of("non_spot", 70));
//
//    initRequest();
//    initFirstDeploy();
//
//    saveAndSchedule(request.toBuilder()
//        .setInstances(Optional.of(10))
//        .setAllowedSlaveAttributes(Optional.of(allowedAttributes))
//        .setSlaveAttributeMinimums(Optional.of(attributeMinimums)));
//
//    // The schedule should only accept as many "spot" instances so as to not force a violation of the minimum "non_spot" instances
//    sms.resourceOffers(Arrays.asList(createOffer(3, 20000, 50000, "slave1", "host1", Optional.<String>absent(), ImmutableMap.of("instance_lifecycle_type", "spot"))));
//    sms.resourceOffers(Arrays.asList(createOffer(7, 20000, 50000, "slave2", "host2", Optional.<String>absent(), ImmutableMap.of("instance_lifecycle_type", "non_spot"))));
//    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 10);
//    System.out.println(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).stream().map(SingularityTaskIdHolder::getTaskId).collect(Collectors.toList()));
//    System.out.println(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).stream().map(SingularityTaskIdHolder::getTaskId).collect(Collectors.toList()));
//
//    Assertions.assertEquals(3, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());
//    Assertions.assertEquals(7, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size());
//
//    Map<SingularityTaskId, SingularityTask> allTasks = taskManager.getActiveTasks().stream().collect(Collectors.toMap(SingularityTask::getTaskId, Function.identity()));
//
//    requestResource.scale(requestId, new SingularityScaleRequest(Optional.of(9), Optional.absent(), Optional.absent(),
//        Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent()), singularityUser);
//
//    Assertions.assertEquals(2, taskManager.getCleanupTaskIds().size());
//    Assertions.assertEquals(1, taskManager.getCleanupTasks().stream().filter(s -> s.getCleanupType() == TaskCleanupType.SCALING_DOWN).count());
//    Assertions.assertEquals(1, taskManager.getCleanupTasks().stream().filter(s -> s.getCleanupType() == TaskCleanupType.REBALANCE_SLAVE_ATTRIBUTES).count());
//
//    scheduler.drainPendingQueue();
//
//    sms.resourceOffers(Arrays.asList(createOffer(10, 20000, 50000, "slave2", "host2", Optional.<String>absent(), ImmutableMap.of("instance_lifecycle_type", "non_spot"))));
//
//    Assertions.assertEquals(11, taskManager.getActiveTaskIds().size());
//    Assertions.assertEquals(8, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size());
//    Assertions.assertEquals(3, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());
//  }
}
