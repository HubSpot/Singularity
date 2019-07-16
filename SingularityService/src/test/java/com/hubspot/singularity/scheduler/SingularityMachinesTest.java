package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.TaskState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosFrameworkObject;
import com.hubspot.mesos.json.MesosMasterSlaveObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosResourcesObject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityRunNowRequestBuilder;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.data.AbstractMachineManager.StateChangeResult;
import com.hubspot.singularity.mesos.SingularitySlaveAndRackManager;
import com.hubspot.singularity.resources.SlaveResource;

@TestMethodOrder(OrderAnnotation.class)
public class SingularityMachinesTest extends SingularitySchedulerTestBase {

  @Inject
  protected SingularitySlaveReconciliationPoller slaveReconciliationPoller;

  @Inject
  private SingularitySlaveAndRackManager singularitySlaveAndRackManager;

  @Inject
  private SlaveResource slaveResource;

  public SingularityMachinesTest() {
    super(false);
  }

  @Test
  public void testDeadSlavesArePurged() {
    SingularitySlave liveSlave = new SingularitySlave("1", "h1", "r1", ImmutableMap.of("uniqueAttribute", "1"), Optional.absent());
    SingularitySlave deadSlave = new SingularitySlave("2", "h1", "r1", ImmutableMap.of("uniqueAttribute", "2"), Optional.absent());

    final long now = System.currentTimeMillis();

    liveSlave = liveSlave.changeState(new SingularityMachineStateHistoryUpdate("1", MachineState.ACTIVE, 100, Optional.absent(), Optional.absent()));
    deadSlave = deadSlave.changeState(new SingularityMachineStateHistoryUpdate("2", MachineState.DEAD, now - TimeUnit.HOURS.toMillis(10), Optional.absent(), Optional.absent()));

    slaveManager.saveObject(liveSlave);
    slaveManager.saveObject(deadSlave);

    slaveReconciliationPoller.runActionOnPoll();

    Assertions.assertEquals(1, slaveManager.getObjectsFiltered(MachineState.ACTIVE).size());
    Assertions.assertEquals(1, slaveManager.getObjectsFiltered(MachineState.DEAD).size());

    configuration.setDeleteDeadSlavesAfterHours(1);

    slaveReconciliationPoller.runActionOnPoll();

    Assertions.assertEquals(1, slaveManager.getObjectsFiltered(MachineState.ACTIVE).size());
    Assertions.assertEquals(0, slaveManager.getObjectsFiltered(MachineState.DEAD).size());
  }

  @Test
  public void testBasicSlaveAndRackState() {
    sms.resourceOffers(Arrays.asList(createOffer(1, 1, 1, "slave1", "host1", Optional.of("rack1"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 1, 1, "slave2", "host2", Optional.of("rack2"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 1, 1, "slave1", "host1", Optional.of("rack1"))));

    Assertions.assertEquals(1, slaveManager.getHistory("slave1").size());
    Assertions.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assertions.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);

    SingularityMachineStateHistoryUpdate currentState = slaveManager.getObject("slave1").get().getCurrentState();
    SingularityMachineStateHistoryUpdate historyUpdate = slaveManager.getHistory("slave1").get(0);

    Assertions.assertTrue(currentState.equals(historyUpdate));

    sms.slaveLost(AgentID.newBuilder().setValue("slave1").build());

    Assertions.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 1);
    Assertions.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 1);

    Assertions.assertTrue(slaveManager.getNumObjectsAtState(MachineState.DEAD) == 1);
    Assertions.assertTrue(rackManager.getNumObjectsAtState(MachineState.DEAD) == 1);

    Assertions.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getState() == MachineState.DEAD);
    Assertions.assertTrue(rackManager.getObject("rack1").get().getCurrentState().getState() == MachineState.DEAD);

    sms.resourceOffers(Arrays.asList(createOffer(1, 1, 1, "slave3", "host3", Optional.of("rack1"))));

    Assertions.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assertions.assertEquals(2, rackManager.getNumObjectsAtState(MachineState.ACTIVE));
    Assertions.assertTrue(slaveManager.getNumObjectsAtState(MachineState.DEAD) == 1);

    Assertions.assertTrue(rackManager.getHistory("rack1").size() == 3);

    sms.resourceOffers(Arrays.asList(createOffer(1, 1, 1, "slave1", "host1", Optional.of("rack1"))));

    Assertions.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 3);
    Assertions.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);

    sms.slaveLost(AgentID.newBuilder().setValue("slave1").build());

    Assertions.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assertions.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assertions.assertTrue(slaveManager.getNumObjectsAtState(MachineState.DEAD) == 1);
    Assertions.assertTrue(slaveManager.getHistory("slave1").size() == 4);

    sms.slaveLost(AgentID.newBuilder().setValue("slave1").build());

    Assertions.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assertions.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assertions.assertTrue(slaveManager.getNumObjectsAtState(MachineState.DEAD) == 1);
    Assertions.assertTrue(slaveManager.getHistory("slave1").size() == 4);

    slaveManager.deleteObject("slave1");

    Assertions.assertTrue(slaveManager.getNumObjectsAtState(MachineState.DEAD) == 0);
    Assertions.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assertions.assertTrue(slaveManager.getHistory("slave1").isEmpty());
  }

  @Test
  public void testDecommissioning() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)));

    scheduler.drainPendingQueue();

    sms.resourceOffers(Arrays.asList(createOffer(1, 129, 1025, "slave1", "host1", Optional.of("rack1"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 129, 1025, "slave2", "host2", Optional.of("rack1"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 129, 1025, "slave3", "host3", Optional.of("rack2"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 129, 1025, "slave4", "host4", Optional.of("rack2"))));


    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }
    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }

    Assertions.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assertions.assertTrue(slaveManager.getNumObjectsAtState(MachineState.ACTIVE) == 4);

    Assertions.assertTrue(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size() == 1);
    Assertions.assertTrue(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size() == 1);
    Assertions.assertTrue(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave3").get()).isEmpty());
    Assertions.assertTrue(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave4").get()).isEmpty());

    Assertions.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.STARTING_DECOMMISSION, Optional.absent(), Optional.of("user1")));
    Assertions.assertEquals(StateChangeResult.FAILURE_ALREADY_AT_STATE, slaveManager.changeState("slave1", MachineState.STARTING_DECOMMISSION, Optional.absent(), Optional.of("user1")));
    Assertions.assertEquals(StateChangeResult.FAILURE_NOT_FOUND, slaveManager.changeState("slave9231", MachineState.STARTING_DECOMMISSION, Optional.absent(), Optional.of("user1")));

    Assertions.assertEquals(MachineState.STARTING_DECOMMISSION, slaveManager.getObject("slave1").get().getCurrentState().getState());
    Assertions.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getUser().get().equals("user1"));

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(3)));

    scheduler.drainPendingQueue();
    scheduler.checkForDecomissions();
    sms.resourceOffers(Arrays.asList(createOffer(1, 129, 1025, "slave1", "host1", Optional.of("rack1"))));

    Assertions.assertTrue(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size() == 1);

    Assertions.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getState() == MachineState.DECOMMISSIONING);
    Assertions.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getUser().get().equals("user1"));

    cleaner.drainCleanupQueue();

    Assertions.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getState() == MachineState.DECOMMISSIONING);
    Assertions.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getUser().get().equals("user1"));

    scheduler.drainPendingQueue();
    sms.resourceOffers(Arrays.asList(createOffer(1, 129, 1025, "slave4", "host4", Optional.of("rack2"))));
    sms.resourceOffers(Arrays.asList(createOffer(1, 129, 1025, "slave3", "host3", Optional.of("rack2"))));

    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave4").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }
    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave3").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }

    // all task should have moved.
    cleaner.drainCleanupQueue();
    scheduler.drainPendingQueue();

    Assertions.assertTrue(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave4").get()).size() == 1);
    Assertions.assertTrue(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave3").get()).size() == 1);
    Assertions.assertEquals(1, taskManager.getKilledTaskIdRecords().size());

    // kill the task
    statusUpdate(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).get(0), TaskState.TASK_KILLED);

    Assertions.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getState() == MachineState.DECOMMISSIONED);
    Assertions.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getUser().get().equals("user1"));

    // let's DECOMMission rack2
    Assertions.assertEquals(StateChangeResult.SUCCESS, rackManager.changeState("rack2", MachineState.STARTING_DECOMMISSION, Optional.absent(), Optional.of("user2")));

    // it shouldn't place any on here, since it's DECOMMissioned
    scheduler.drainPendingQueue();
    scheduler.checkForDecomissions();
    sms.resourceOffers(Arrays.asList(createOffer(1, 129, 1025, "slave1", "host1", Optional.of("rack1"))));

    Assertions.assertEquals(0, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());

    scheduler.drainPendingQueue();
    sms.resourceOffers(Arrays.asList(createOffer(1, 129, 1025, "slave1", "host1", Optional.of("rack1"))));

    Assertions.assertEquals(0, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());

    slaveResource.activateSlave(singularityUser, "slave1", null);

    scheduler.drainPendingQueue();
    sms.resourceOffers(Arrays.asList(createOffer(1, 129, 1025, "slave1", "host1", Optional.of("rack1"))));

    Assertions.assertEquals(1, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());

    Assertions.assertTrue(rackManager.getObject("rack2").get().getCurrentState().getState() == MachineState.DECOMMISSIONING);

    scheduler.drainPendingQueue();
    sms.resourceOffers(Arrays.asList(createOffer(1, 129, 1025, "slave2", "host2", Optional.of("rack1"))));

    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }
    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }

    cleaner.drainCleanupQueue();

    // kill the tasks
    statusUpdate(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave3").get()).get(0), TaskState.TASK_KILLED);

    Assertions.assertTrue(rackManager.getObject("rack2").get().getCurrentState().getState() == MachineState.DECOMMISSIONING);

    statusUpdate(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave4").get()).get(0), TaskState.TASK_KILLED);

    Assertions.assertTrue(rackManager.getObject("rack2").get().getCurrentState().getState() == MachineState.DECOMMISSIONED);

  }

  @Test
  public void testEmptyDecommissioning() {
    sms.resourceOffers(Arrays.asList(createOffer(1, 129, 1025, "slave1", "host1", Optional.of("rack1"))));

    Assertions.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.STARTING_DECOMMISSION, Optional.absent(), Optional.of("user1")));

    scheduler.drainPendingQueue();
    scheduler.checkForDecomissions();
    sms.resourceOffers(Arrays.asList(createOffer(1, 129, 1025, "slave1", "host1", Optional.of("rack1"))));

    Assertions.assertEquals(MachineState.DECOMMISSIONED, slaveManager.getObject("slave1").get().getCurrentState().getState());
  }

  @Test
  @Order(-1) // TODO - check why this fails when run after other tests
  public void testFrozenSlaveTransitions() {
    initRequest();
    initFirstDeploy();

    resourceOffers();

    // test transitions out of frozen
    Assertions.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.FROZEN, Optional.absent(), Optional.of("user1")));
    Assertions.assertEquals(StateChangeResult.FAILURE_ALREADY_AT_STATE, slaveManager.changeState("slave1", MachineState.FROZEN, Optional.absent(), Optional.of("user1")));
    Assertions.assertEquals(StateChangeResult.FAILURE_ILLEGAL_TRANSITION, slaveManager.changeState("slave1", MachineState.DECOMMISSIONING, Optional.absent(), Optional.of("user1")));
    Assertions.assertEquals(StateChangeResult.FAILURE_ILLEGAL_TRANSITION, slaveManager.changeState("slave1", MachineState.DECOMMISSIONED, Optional.absent(), Optional.of("user1")));
    Assertions.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.ACTIVE, Optional.absent(), Optional.of("user1")));

    // test transitions into frozen
    Assertions.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave2", MachineState.STARTING_DECOMMISSION, Optional.absent(), Optional.of("user2")));
    Assertions.assertEquals(StateChangeResult.FAILURE_ILLEGAL_TRANSITION, slaveManager.changeState("slave2", MachineState.FROZEN, Optional.absent(), Optional.of("user2")));
    Assertions.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave2", MachineState.DECOMMISSIONING, Optional.absent(), Optional.of("user2")));
    Assertions.assertEquals(StateChangeResult.FAILURE_ILLEGAL_TRANSITION, slaveManager.changeState("slave2", MachineState.FROZEN, Optional.absent(), Optional.of("user2")));
    Assertions.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave2", MachineState.DECOMMISSIONED, Optional.absent(), Optional.of("user2")));
    Assertions.assertEquals(StateChangeResult.FAILURE_ILLEGAL_TRANSITION, slaveManager.changeState("slave2", MachineState.FROZEN, Optional.absent(), Optional.of("user2")));
    Assertions.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave2", MachineState.ACTIVE, Optional.absent(), Optional.of("user2")));
    Assertions.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave2", MachineState.FROZEN, Optional.absent(), Optional.of("user2")));
  }

  @Test
  public void testFrozenSlaveDoesntLaunchTasks() {
    initRequest();
    initFirstDeploy();

    resourceOffers();

    Assertions.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.FROZEN, Optional.absent(), Optional.of("user1")));

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)));

    resourceOffers();

    Assertions.assertEquals(0, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());
    Assertions.assertEquals(2, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size());
  }

  @Test
  public void testUnfrozenSlaveLaunchesTasks() {
    initRequest();
    initFirstDeploy();

    resourceOffers();

    Assertions.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.FROZEN, Optional.absent(), Optional.of("user1")));

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)).setSlavePlacement(Optional.of(SlavePlacement.SEPARATE)));

    resourceOffers();

    Assertions.assertEquals(0, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());
    Assertions.assertEquals(1, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size());

    Assertions.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.ACTIVE, Optional.absent(), Optional.of("user1")));

    resourceOffers();

    Assertions.assertEquals(1, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());
    Assertions.assertEquals(1, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size());
  }

  @Test
  public void testFrozenSlaveCanBeDecommissioned() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setSlavePlacement(Optional.of(SlavePlacement.GREEDY)).setInstances(Optional.of(2)));

    scheduler.drainPendingQueue();
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave1", "host1")));
    sms.resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "slave2", "host2")));

    // freeze slave1
    Assertions.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.FROZEN, Optional.absent(), Optional.of("user1")));

    // mark tasks as running
    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }
    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }

    // assert Request is spread over the two slaves
    Assertions.assertEquals(1, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());
    Assertions.assertEquals(1, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size());

    // decommission frozen slave1
    Assertions.assertEquals(StateChangeResult.SUCCESS, slaveManager.changeState("slave1", MachineState.STARTING_DECOMMISSION, Optional.absent(), Optional.of("user1")));

    scheduler.checkForDecomissions();
    scheduler.drainPendingQueue();
    resourceOffers();
    cleaner.drainCleanupQueue();

    // assert slave1 is decommissioning
    Assertions.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getState() == MachineState.DECOMMISSIONING);

    // mark tasks as running
    for (SingularityTask task : taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get())) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }

    // all tasks should have moved
    cleaner.drainCleanupQueue();

    // kill decommissioned task
    statusUpdate(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).get(0), TaskState.TASK_KILLED);

    // assert all tasks on slave2 + slave1 is decommissioned
    Assertions.assertEquals(0, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave1").get()).size());
    Assertions.assertEquals(2, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slaveManager.getObject("slave2").get()).size());
    Assertions.assertTrue(slaveManager.getObject("slave1").get().getCurrentState().getState() == MachineState.DECOMMISSIONED);
  }

  @Test
  public void testLoadSlavesFromMasterDataOnStartup() {
    MesosMasterStateObject state = getMasterState(3);
    singularitySlaveAndRackManager.loadSlavesAndRacksFromMaster(state, true);

    List<SingularitySlave> slaves = slaveManager.getObjects();

    Assertions.assertEquals(3, slaves.size());
    for (SingularitySlave slave : slaves) {
      Assertions.assertEquals(MachineState.ACTIVE, slave.getCurrentState().getState());
    }
  }

  @Test
  public void testReconcileSlaves() {
    // Load 3 slaves on startup
    MesosMasterStateObject state = getMasterState(3);
    singularitySlaveAndRackManager.loadSlavesAndRacksFromMaster(state, true);

    MesosMasterStateObject newState = getMasterState(2); // 2 slaves, third has died
    singularitySlaveAndRackManager.loadSlavesAndRacksFromMaster(newState, false);
    List<SingularitySlave> slaves = slaveManager.getObjects();

    Assertions.assertEquals(3, slaves.size());

    for (SingularitySlave slave : slaves) {
      if (slave.getId().equals("2")) {
        Assertions.assertEquals(MachineState.DEAD, slave.getCurrentState().getState());
      } else {
        Assertions.assertEquals(MachineState.ACTIVE, slave.getCurrentState().getState());
      }
    }
  }

  private MesosMasterStateObject getMasterState(int numSlaves) {
    long now = System.currentTimeMillis();
    Map<String, Object> resources = new HashMap<>();
    resources.put("cpus", 10);
    resources.put("mem", 2000);

    Map<String, String> attributes = new HashMap<>();
    attributes.put("testKey", "testValue");

    List<MesosMasterSlaveObject> slaves = new ArrayList<>();
    for (Integer i = 0; i < numSlaves; i++) {
      slaves.add(new MesosMasterSlaveObject(i.toString(), i.toString(), String.format("localhost:505%s", i), now, new MesosResourcesObject(resources), attributes, new MesosResourcesObject(resources), new MesosResourcesObject(resources), new MesosResourcesObject(resources), new MesosResourcesObject(resources), "", true));
    }

    MesosFrameworkObject framework = new MesosFrameworkObject("", "", "", "", "", "", "", now, now, now, true, true, new MesosResourcesObject(resources), new MesosResourcesObject(resources), new MesosResourcesObject(resources), Collections.emptyList());

    return new MesosMasterStateObject("", "", "", "", now, "", now, now, "", "", "", 0, 0, "", "", "", Collections.emptyMap(), slaves, Collections.singletonList(framework));
  }

  @Test
  public void testExpiringMachineState() {
    MesosMasterStateObject state = getMasterState(1);
    singularitySlaveAndRackManager.loadSlavesAndRacksFromMaster(state, true);

    SingularitySlave slave = slaveManager.getObjects().get(0);

    slaveResource.freezeSlave(singularityUser, slave.getId(), new SingularityMachineChangeRequest(Optional.of(1L), Optional.absent(), Optional.absent(), Optional.of(MachineState.ACTIVE), Optional.absent()));

    Assertions.assertEquals(MachineState.FROZEN, slaveManager.getObjects().get(0).getCurrentState().getState());

    expiringUserActionPoller.runActionOnPoll();

    Assertions.assertEquals(MachineState.ACTIVE, slaveManager.getObjects().get(0).getCurrentState().getState());
  }

  private SingularitySlave getSingleSlave() {
    MesosMasterStateObject state = getMasterState(1);
    singularitySlaveAndRackManager.loadSlavesAndRacksFromMaster(state, true);
    return slaveManager.getObjects().get(0);
  }
  @Test
  public void testCannotUseStateReservedForSystem() {
    SingularitySlave slave = getSingleSlave();
    Assertions.assertThrows(WebApplicationException.class, () ->
        slaveResource.freezeSlave(singularityUser, slave.getId(), new SingularityMachineChangeRequest(Optional.of(1L), Optional.absent(), Optional.absent(), Optional.of(MachineState.DEAD), Optional.absent())));
  }

  @Test
  public void testBadExpiringStateTransition() {
    SingularitySlave slave = getSingleSlave();
    Assertions.assertThrows(WebApplicationException.class, () ->
        slaveResource.decommissionSlave(singularityUser, slave.getId(), new SingularityMachineChangeRequest(Optional.of(1L), Optional.absent(), Optional.absent(), Optional.of(MachineState.FROZEN), Optional.absent())));
  }

  @Test
  public void testInvalidTransitionToDecommissioned() {
    SingularitySlave slave = getSingleSlave();
    Assertions.assertThrows(WebApplicationException.class, () ->
        slaveResource.decommissionSlave(singularityUser, slave.getId(), new SingularityMachineChangeRequest(Optional.of(1L), Optional.absent(), Optional.absent(), Optional.of(MachineState.DECOMMISSIONED), Optional.absent())));
  }

  @Test
  public void testValidTransitionToDecommissioned() {
    initRequest();
    initFirstDeploy();
    requestResource.postRequest(request.toBuilder().setInstances(Optional.of(2)).build(), singularityUser);
    scheduler.drainPendingQueue();
    resourceOffers(1);
    SingularitySlave slave = slaveManager.getObjects().get(0);

    slaveResource.decommissionSlave(singularityUser, slave.getId(), new SingularityMachineChangeRequest(Optional.of(1L), Optional.absent(), Optional.absent(), Optional.of(MachineState.DECOMMISSIONED), Optional.of(true)));
    Assertions.assertEquals(MachineState.STARTING_DECOMMISSION, slaveManager.getObjects().get(0).getCurrentState().getState());
    scheduler.checkForDecomissions();
    scheduler.drainPendingQueue();
    Assertions.assertEquals(TaskCleanupType.DECOMISSIONING, taskManager.getCleanupTasks().get(0).getCleanupType());

    expiringUserActionPoller.runActionOnPoll();
    Assertions.assertEquals(MachineState.DECOMMISSIONED, slaveManager.getObjects().get(0).getCurrentState().getState());
    Assertions.assertEquals(TaskCleanupType.DECOMMISSION_TIMEOUT, taskManager.getCleanupTasks().get(0).getCleanupType());
  }

  @Test
  public void testSystemChangeClearsExpiringChangeIfInvalid() {
    SingularitySlave slave = getSingleSlave();
    slaveResource.freezeSlave(singularityUser, slave.getId(), null);
    slaveResource.activateSlave(singularityUser, slave.getId(), new SingularityMachineChangeRequest(Optional.of(1L), Optional.absent(), Optional.absent(), Optional.of(MachineState.FROZEN), Optional.absent()));
    Assertions.assertTrue(slaveManager.getExpiringObject(slave.getId()).isPresent());
    slaveResource.decommissionSlave(singularityUser, slave.getId(), null);
    Assertions.assertFalse(slaveManager.getExpiringObject(slave.getId()).isPresent());
  }

  @Test
  public void itShouldContainAnInactiveHostWhenHostDeactivated() {
    inactiveSlaveManager.deactivateSlave("localhost");

    Assertions.assertTrue(inactiveSlaveManager.getInactiveSlaves().contains("localhost"));
  }

  @Test
  public void itShouldNotContainHostAfterActivatingHost() {
    inactiveSlaveManager.deactivateSlave("localhost");
    inactiveSlaveManager.activateSlave("localhost");

    Assertions.assertFalse(inactiveSlaveManager.getInactiveSlaves().contains("localhost"));
  }

  @Test
  public void itShouldMarkSlavesFromInactiveHostAsDecommissioned() {
    inactiveSlaveManager.deactivateSlave("host1");

    resourceOffers();
    SingularitySlave slave = slaveManager.getObject("slave1").get();
    Assertions.assertTrue(slave.getCurrentState().getState().isDecommissioning());
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
      sms.resourceOffers(Arrays.asList(createOffer(0.1, 1, 1, "slave1", "host1", Optional.of("slave1"))));
      sms.resourceOffers(Arrays.asList(createOffer(0.1, 1, 1, "slave2", "host2", Optional.of("slave2"))));
      sms.resourceOffers(Arrays.asList(createOffer(0.1, 1, 1, "slave3", "host3", Optional.of("slave3"))));

      request = new SingularityRequestBuilder(requestId, RequestType.WORKER).setInstances(Optional.of(7)).setRackSensitive(Optional.of(true)).build();
      saveRequest(request);
      initFirstDeploy();
      launchTask(request, firstDeploy, System.currentTimeMillis(), System.currentTimeMillis(), 1, TaskState.TASK_RUNNING, true, Optional.absent(), Optional.of("slave1"));
      launchTask(request, firstDeploy, System.currentTimeMillis(), System.currentTimeMillis(), 2, TaskState.TASK_RUNNING, true, Optional.absent(), Optional.of("slave1"));
      launchTask(request, firstDeploy, System.currentTimeMillis(), System.currentTimeMillis(), 3, TaskState.TASK_RUNNING, true, Optional.absent(), Optional.of("slave2"));
      launchTask(request, firstDeploy, System.currentTimeMillis(), System.currentTimeMillis(), 4, TaskState.TASK_RUNNING, true, Optional.absent(), Optional.of("slave2"));
      launchTask(request, firstDeploy, System.currentTimeMillis(), System.currentTimeMillis(), 5, TaskState.TASK_RUNNING, true, Optional.absent(), Optional.of("slave3"));
      launchTask(request, firstDeploy, System.currentTimeMillis(), System.currentTimeMillis(), 6, TaskState.TASK_RUNNING, true, Optional.absent(), Optional.of("slave3"));
      launchTask(request, firstDeploy, System.currentTimeMillis(), System.currentTimeMillis(), 7, TaskState.TASK_RUNNING, true, Optional.absent(), Optional.of("slave3"));

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

}
