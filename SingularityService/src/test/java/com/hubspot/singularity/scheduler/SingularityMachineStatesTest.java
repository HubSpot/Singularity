package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.WebApplicationException;

import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.TaskState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosFrameworkObject;
import com.hubspot.mesos.json.MesosMasterSlaveObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosResourcesObject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.data.AbstractMachineManager.StateChangeResult;
import com.hubspot.singularity.mesos.SingularitySlaveAndRackManager;
import com.hubspot.singularity.resources.SlaveResource;

public class SingularityMachineStatesTest extends SingularitySchedulerTestBase {

  @Inject
  protected SingularitySlaveReconciliationPoller slaveReconciliationPoller;

  @Inject
  private SingularitySlaveAndRackManager singularitySlaveAndRackManager;

  @Inject
  private SlaveResource slaveResource;

  public SingularityMachineStatesTest() {
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

}
