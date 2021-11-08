package com.hubspot.singularity.scheduler;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosFrameworkObject;
import com.hubspot.mesos.json.MesosMasterAgentObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosResourcesObject;
import com.hubspot.singularity.AgentPlacement;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityAgent;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityRunNowRequestBuilder;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.data.AbstractMachineManager.StateChangeResult;
import com.hubspot.singularity.mesos.SingularityAgentAndRackManager;
import com.hubspot.singularity.resources.AgentResource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

@TestMethodOrder(OrderAnnotation.class)
public class SingularityMachinesTest extends SingularitySchedulerTestBase {
  @Inject
  protected SingularityAgentReconciliationPoller agentReconciliationPoller;

  @Inject
  private SingularityAgentAndRackManager singularityAgentAndRackManager;

  @Inject
  private AgentResource agentResource;

  public SingularityMachinesTest() {
    super(false);
  }

  @Test
  public void testDeadSlavesArePurged() {
    SingularityAgent liveSlave = new SingularityAgent(
      "1",
      "h1",
      "r1",
      ImmutableMap.of("uniqueAttribute", "1"),
      Optional.empty()
    );
    SingularityAgent deadSlave = new SingularityAgent(
      "2",
      "h1",
      "r1",
      ImmutableMap.of("uniqueAttribute", "2"),
      Optional.empty()
    );

    final long now = System.currentTimeMillis();

    liveSlave =
      liveSlave.changeState(
        new SingularityMachineStateHistoryUpdate(
          "1",
          MachineState.ACTIVE,
          100,
          Optional.empty(),
          Optional.empty()
        )
      );
    deadSlave =
      deadSlave.changeState(
        new SingularityMachineStateHistoryUpdate(
          "2",
          MachineState.DEAD,
          now - TimeUnit.HOURS.toMillis(10),
          Optional.empty(),
          Optional.empty()
        )
      );

    agentManager.saveObject(liveSlave);
    agentManager.saveObject(deadSlave);
    agentReconciliationPoller.runActionOnPoll();

    Assertions.assertEquals(
      1,
      agentManager.getObjectsFiltered(MachineState.ACTIVE).size()
    );
    Assertions.assertEquals(1, agentManager.getObjectsFiltered(MachineState.DEAD).size());

    configuration.setDeleteDeadAgentsAfterHours(1);

    agentReconciliationPoller.runActionOnPoll();

    Assertions.assertEquals(
      1,
      agentManager.getObjectsFiltered(MachineState.ACTIVE).size()
    );
    Assertions.assertEquals(0, agentManager.getObjectsFiltered(MachineState.DEAD).size());
  }

  @Test
  public void testMissingSlavesArePurged() {
    SingularityAgent liveSlave = new SingularityAgent(
      "1",
      "h1",
      "r1",
      ImmutableMap.of("uniqueAttribute", "1"),
      Optional.empty()
    );
    SingularityAgent missingSlave = new SingularityAgent(
      "2",
      "h1",
      "r1",
      ImmutableMap.of("uniqueAttribute", "2"),
      Optional.empty()
    );

    final long now = System.currentTimeMillis();

    liveSlave =
      liveSlave.changeState(
        new SingularityMachineStateHistoryUpdate(
          "1",
          MachineState.ACTIVE,
          100,
          Optional.empty(),
          Optional.empty()
        )
      );
    missingSlave =
      missingSlave.changeState(
        new SingularityMachineStateHistoryUpdate(
          "2",
          MachineState.MISSING_ON_STARTUP,
          now - TimeUnit.HOURS.toMillis(10),
          Optional.empty(),
          Optional.empty()
        )
      );

    agentManager.saveObject(liveSlave);
    agentManager.saveObject(missingSlave);

    agentReconciliationPoller.runActionOnPoll();

    Assertions.assertEquals(
      1,
      agentManager.getObjectsFiltered(MachineState.ACTIVE).size()
    );
    Assertions.assertEquals(
      1,
      agentManager.getObjectsFiltered(MachineState.MISSING_ON_STARTUP).size()
    );

    configuration.setDeleteDeadAgentsAfterHours(1);

    agentReconciliationPoller.runActionOnPoll();

    Assertions.assertEquals(
      1,
      agentManager.getObjectsFiltered(MachineState.ACTIVE).size()
    );
    Assertions.assertEquals(
      0,
      agentManager.getObjectsFiltered(MachineState.MISSING_ON_STARTUP).size()
    );
  }

  @Test
  public void testBasicSlaveAndRackState() {
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 1, 1, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 1, 1, "agent2", "host2", Optional.of("rack2")))
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 1, 1, "agent1", "host1", Optional.of("rack1")))
      )
      .join();

    Assertions.assertEquals(1, agentManager.getHistory("agent1").size());
    Assertions.assertTrue(agentManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assertions.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);

    SingularityMachineStateHistoryUpdate currentState = agentManager
      .getObject("agent1")
      .get()
      .getCurrentState();
    SingularityMachineStateHistoryUpdate historyUpdate = agentManager
      .getHistory("agent1")
      .get(0);

    Assertions.assertTrue(currentState.equals(historyUpdate));

    sms.agentLost(AgentID.newBuilder().setValue("agent1").build());

    Assertions.assertTrue(agentManager.getNumObjectsAtState(MachineState.ACTIVE) == 1);
    Assertions.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 1);

    Assertions.assertTrue(agentManager.getNumObjectsAtState(MachineState.DEAD) == 1);
    Assertions.assertTrue(rackManager.getNumObjectsAtState(MachineState.DEAD) == 1);

    Assertions.assertTrue(
      agentManager.getObject("agent1").get().getCurrentState().getState() ==
      MachineState.DEAD
    );
    Assertions.assertTrue(
      rackManager.getObject("rack1").get().getCurrentState().getState() ==
      MachineState.DEAD
    );

    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 1, 1, "agent3", "host3", Optional.of("rack1")))
      )
      .join();

    Assertions.assertTrue(agentManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assertions.assertEquals(2, rackManager.getNumObjectsAtState(MachineState.ACTIVE));
    Assertions.assertTrue(agentManager.getNumObjectsAtState(MachineState.DEAD) == 1);

    Assertions.assertTrue(rackManager.getHistory("rack1").size() == 3);

    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 1, 1, "agent1", "host1", Optional.of("rack1")))
      )
      .join();

    Assertions.assertTrue(agentManager.getNumObjectsAtState(MachineState.ACTIVE) == 3);
    Assertions.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);

    sms.agentLost(AgentID.newBuilder().setValue("agent1").build());

    Assertions.assertTrue(agentManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assertions.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assertions.assertTrue(agentManager.getNumObjectsAtState(MachineState.DEAD) == 1);
    Assertions.assertTrue(agentManager.getHistory("agent1").size() == 4);

    sms.agentLost(AgentID.newBuilder().setValue("agent1").build());

    Assertions.assertTrue(agentManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assertions.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assertions.assertTrue(agentManager.getNumObjectsAtState(MachineState.DEAD) == 1);
    Assertions.assertTrue(agentManager.getHistory("agent1").size() == 4);

    agentManager.deleteObject("agent1");

    Assertions.assertTrue(agentManager.getNumObjectsAtState(MachineState.DEAD) == 0);
    Assertions.assertTrue(agentManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assertions.assertTrue(agentManager.getHistory("agent1").isEmpty());
  }

  @Test
  public void testDecommissioning() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)));

    scheduler.drainPendingQueue();

    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent2", "host2", Optional.of("rack1")))
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent3", "host3", Optional.of("rack2")))
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent4", "host4", Optional.of("rack2")))
      )
      .join();

    for (SingularityTask task : taskManager.getTasksOnAgent(
      taskManager.getActiveTaskIds(),
      agentManager.getObject("agent1").get()
    )) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }
    for (SingularityTask task : taskManager.getTasksOnAgent(
      taskManager.getActiveTaskIds(),
      agentManager.getObject("agent2").get()
    )) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }

    Assertions.assertTrue(rackManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);
    Assertions.assertTrue(agentManager.getNumObjectsAtState(MachineState.ACTIVE) == 4);

    Assertions.assertTrue(
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent1").get()
        )
        .size() ==
      1
    );
    Assertions.assertTrue(
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent2").get()
        )
        .size() ==
      1
    );
    Assertions.assertTrue(
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent3").get()
        )
        .isEmpty()
    );
    Assertions.assertTrue(
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent4").get()
        )
        .isEmpty()
    );

    Assertions.assertEquals(
      StateChangeResult.SUCCESS,
      agentManager.changeState(
        "agent1",
        MachineState.STARTING_DECOMMISSION,
        Optional.empty(),
        Optional.of("user1")
      )
    );
    Assertions.assertEquals(
      StateChangeResult.FAILURE_ALREADY_AT_STATE,
      agentManager.changeState(
        "agent1",
        MachineState.STARTING_DECOMMISSION,
        Optional.empty(),
        Optional.of("user1")
      )
    );
    Assertions.assertEquals(
      StateChangeResult.FAILURE_NOT_FOUND,
      agentManager.changeState(
        "agent9231",
        MachineState.STARTING_DECOMMISSION,
        Optional.empty(),
        Optional.of("user1")
      )
    );

    Assertions.assertEquals(
      MachineState.STARTING_DECOMMISSION,
      agentManager.getObject("agent1").get().getCurrentState().getState()
    );
    Assertions.assertTrue(
      agentManager
        .getObject("agent1")
        .get()
        .getCurrentState()
        .getUser()
        .get()
        .equals("user1")
    );

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(3)));

    scheduler.drainPendingQueue();
    scheduler.checkForDecomissions();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent1", "host1", Optional.of("rack1")))
      )
      .join();

    Assertions.assertTrue(
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent1").get()
        )
        .size() ==
      1
    );

    Assertions.assertTrue(
      agentManager.getObject("agent1").get().getCurrentState().getState() ==
      MachineState.DECOMMISSIONING
    );
    Assertions.assertTrue(
      agentManager
        .getObject("agent1")
        .get()
        .getCurrentState()
        .getUser()
        .get()
        .equals("user1")
    );

    cleaner.drainCleanupQueue();

    Assertions.assertTrue(
      agentManager.getObject("agent1").get().getCurrentState().getState() ==
      MachineState.DECOMMISSIONING
    );
    Assertions.assertTrue(
      agentManager
        .getObject("agent1")
        .get()
        .getCurrentState()
        .getUser()
        .get()
        .equals("user1")
    );

    scheduler.drainPendingQueue();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent4", "host4", Optional.of("rack2")))
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent3", "host3", Optional.of("rack2")))
      )
      .join();

    for (SingularityTask task : taskManager.getTasksOnAgent(
      taskManager.getActiveTaskIds(),
      agentManager.getObject("agent4").get()
    )) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }
    for (SingularityTask task : taskManager.getTasksOnAgent(
      taskManager.getActiveTaskIds(),
      agentManager.getObject("agent3").get()
    )) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }

    // all task should have moved.
    cleaner.drainCleanupQueue();
    scheduler.drainPendingQueue();

    Assertions.assertTrue(
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent4").get()
        )
        .size() ==
      1
    );
    Assertions.assertTrue(
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent3").get()
        )
        .size() ==
      1
    );
    Assertions.assertEquals(1, taskManager.getKilledTaskIdRecords().size());

    // kill the task
    statusUpdate(
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent1").get()
        )
        .get(0),
      TaskState.TASK_KILLED
    );

    Assertions.assertTrue(
      agentManager.getObject("agent1").get().getCurrentState().getState() ==
      MachineState.DECOMMISSIONED
    );
    Assertions.assertTrue(
      agentManager
        .getObject("agent1")
        .get()
        .getCurrentState()
        .getUser()
        .get()
        .equals("user1")
    );

    // let's DECOMMission rack2
    Assertions.assertEquals(
      StateChangeResult.SUCCESS,
      rackManager.changeState(
        "rack2",
        MachineState.STARTING_DECOMMISSION,
        Optional.empty(),
        Optional.of("user2")
      )
    );

    // it shouldn't place any on here, since it's DECOMMissioned
    scheduler.drainPendingQueue();
    scheduler.checkForDecomissions();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent1", "host1", Optional.of("rack1")))
      )
      .join();

    Assertions.assertEquals(
      0,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent1").get()
        )
        .size()
    );

    scheduler.drainPendingQueue();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent1", "host1", Optional.of("rack1")))
      )
      .join();

    Assertions.assertEquals(
      0,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent1").get()
        )
        .size()
    );

    agentResource.activateAgent(singularityUser, "agent1", null);

    scheduler.drainPendingQueue();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent1", "host1", Optional.of("rack1")))
      )
      .join();

    Assertions.assertEquals(
      1,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent1").get()
        )
        .size()
    );

    Assertions.assertTrue(
      rackManager.getObject("rack2").get().getCurrentState().getState() ==
      MachineState.DECOMMISSIONING
    );

    scheduler.drainPendingQueue();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent2", "host2", Optional.of("rack1")))
      )
      .join();

    for (SingularityTask task : taskManager.getTasksOnAgent(
      taskManager.getActiveTaskIds(),
      agentManager.getObject("agent1").get()
    )) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }
    for (SingularityTask task : taskManager.getTasksOnAgent(
      taskManager.getActiveTaskIds(),
      agentManager.getObject("agent2").get()
    )) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }

    cleaner.drainCleanupQueue();

    // kill the tasks
    statusUpdate(
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent3").get()
        )
        .get(0),
      TaskState.TASK_KILLED
    );

    Assertions.assertTrue(
      rackManager.getObject("rack2").get().getCurrentState().getState() ==
      MachineState.DECOMMISSIONING
    );

    statusUpdate(
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent4").get()
        )
        .get(0),
      TaskState.TASK_KILLED
    );

    Assertions.assertTrue(
      rackManager.getObject("rack2").get().getCurrentState().getState() ==
      MachineState.DECOMMISSIONED
    );
  }

  @Test
  public void testEmptyDecommissioning() {
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent1", "host1", Optional.of("rack1")))
      )
      .join();

    Assertions.assertEquals(
      StateChangeResult.SUCCESS,
      agentManager.changeState(
        "agent1",
        MachineState.STARTING_DECOMMISSION,
        Optional.empty(),
        Optional.of("user1")
      )
    );

    scheduler.drainPendingQueue();
    scheduler.checkForDecomissions();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 129, 1025, "agent1", "host1", Optional.of("rack1")))
      )
      .join();

    Assertions.assertEquals(
      MachineState.DECOMMISSIONED,
      agentManager.getObject("agent1").get().getCurrentState().getState()
    );
  }

  @Test
  @Order(-1) // TODO - check why this fails when run after other tests
  public void testFrozenSlaveTransitions() {
    initRequest();
    initFirstDeploy();

    resourceOffers();

    // test transitions out of frozen
    Assertions.assertEquals(
      StateChangeResult.SUCCESS,
      agentManager.changeState(
        "agent1",
        MachineState.FROZEN,
        Optional.empty(),
        Optional.of("user1")
      )
    );
    Assertions.assertEquals(
      StateChangeResult.FAILURE_ALREADY_AT_STATE,
      agentManager.changeState(
        "agent1",
        MachineState.FROZEN,
        Optional.empty(),
        Optional.of("user1")
      )
    );
    Assertions.assertEquals(
      StateChangeResult.FAILURE_ILLEGAL_TRANSITION,
      agentManager.changeState(
        "agent1",
        MachineState.DECOMMISSIONING,
        Optional.empty(),
        Optional.of("user1")
      )
    );
    Assertions.assertEquals(
      StateChangeResult.FAILURE_ILLEGAL_TRANSITION,
      agentManager.changeState(
        "agent1",
        MachineState.DECOMMISSIONED,
        Optional.empty(),
        Optional.of("user1")
      )
    );
    Assertions.assertEquals(
      StateChangeResult.SUCCESS,
      agentManager.changeState(
        "agent1",
        MachineState.ACTIVE,
        Optional.empty(),
        Optional.of("user1")
      )
    );

    // test transitions into frozen
    Assertions.assertEquals(
      StateChangeResult.SUCCESS,
      agentManager.changeState(
        "agent2",
        MachineState.STARTING_DECOMMISSION,
        Optional.empty(),
        Optional.of("user2")
      )
    );
    Assertions.assertEquals(
      StateChangeResult.FAILURE_ILLEGAL_TRANSITION,
      agentManager.changeState(
        "agent2",
        MachineState.FROZEN,
        Optional.empty(),
        Optional.of("user2")
      )
    );
    Assertions.assertEquals(
      StateChangeResult.SUCCESS,
      agentManager.changeState(
        "agent2",
        MachineState.DECOMMISSIONING,
        Optional.empty(),
        Optional.of("user2")
      )
    );
    Assertions.assertEquals(
      StateChangeResult.FAILURE_ILLEGAL_TRANSITION,
      agentManager.changeState(
        "agent2",
        MachineState.FROZEN,
        Optional.empty(),
        Optional.of("user2")
      )
    );
    Assertions.assertEquals(
      StateChangeResult.SUCCESS,
      agentManager.changeState(
        "agent2",
        MachineState.DECOMMISSIONED,
        Optional.empty(),
        Optional.of("user2")
      )
    );
    Assertions.assertEquals(
      StateChangeResult.FAILURE_ILLEGAL_TRANSITION,
      agentManager.changeState(
        "agent2",
        MachineState.FROZEN,
        Optional.empty(),
        Optional.of("user2")
      )
    );
    Assertions.assertEquals(
      StateChangeResult.SUCCESS,
      agentManager.changeState(
        "agent2",
        MachineState.ACTIVE,
        Optional.empty(),
        Optional.of("user2")
      )
    );
    Assertions.assertEquals(
      StateChangeResult.SUCCESS,
      agentManager.changeState(
        "agent2",
        MachineState.FROZEN,
        Optional.empty(),
        Optional.of("user2")
      )
    );
  }

  @Test
  public void testFrozenSlaveDoesntLaunchTasks() {
    initRequest();
    initFirstDeploy();

    resourceOffers();

    Assertions.assertEquals(
      StateChangeResult.SUCCESS,
      agentManager.changeState(
        "agent1",
        MachineState.FROZEN,
        Optional.empty(),
        Optional.of("user1")
      )
    );

    saveAndSchedule(request.toBuilder().setInstances(Optional.of(2)));

    resourceOffers();

    Assertions.assertEquals(
      0,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent1").get()
        )
        .size()
    );
    Assertions.assertEquals(
      2,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent2").get()
        )
        .size()
    );
  }

  @Test
  public void testUnfrozenSlaveLaunchesTasks() {
    initRequest();
    initFirstDeploy();

    resourceOffers();

    Assertions.assertEquals(
      StateChangeResult.SUCCESS,
      agentManager.changeState(
        "agent1",
        MachineState.FROZEN,
        Optional.empty(),
        Optional.of("user1")
      )
    );

    saveAndSchedule(
      request
        .toBuilder()
        .setInstances(Optional.of(2))
        .setAgentPlacement(Optional.of(AgentPlacement.SEPARATE))
    );

    resourceOffers();

    Assertions.assertEquals(
      0,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent1").get()
        )
        .size()
    );
    Assertions.assertEquals(
      1,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent2").get()
        )
        .size()
    );

    Assertions.assertEquals(
      StateChangeResult.SUCCESS,
      agentManager.changeState(
        "agent1",
        MachineState.ACTIVE,
        Optional.empty(),
        Optional.of("user1")
      )
    );

    resourceOffers();

    Assertions.assertEquals(
      1,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent1").get()
        )
        .size()
    );
    Assertions.assertEquals(
      1,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent2").get()
        )
        .size()
    );
  }

  @Test
  public void testFrozenSlaveCanBeDecommissioned() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(
      request
        .toBuilder()
        .setAgentPlacement(Optional.of(AgentPlacement.GREEDY))
        .setInstances(Optional.of(2))
    );

    scheduler.drainPendingQueue();
    sms
      .resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1")))
      .join();
    sms
      .resourceOffers(Arrays.asList(createOffer(1, 128, 1024, "agent2", "host2")))
      .join();

    // freeze agent1
    Assertions.assertEquals(
      StateChangeResult.SUCCESS,
      agentManager.changeState(
        "agent1",
        MachineState.FROZEN,
        Optional.empty(),
        Optional.of("user1")
      )
    );

    // mark tasks as running
    for (SingularityTask task : taskManager.getTasksOnAgent(
      taskManager.getActiveTaskIds(),
      agentManager.getObject("agent1").get()
    )) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }
    for (SingularityTask task : taskManager.getTasksOnAgent(
      taskManager.getActiveTaskIds(),
      agentManager.getObject("agent2").get()
    )) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }

    // assert Request is spread over the two agents
    Assertions.assertEquals(
      1,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent1").get()
        )
        .size()
    );
    Assertions.assertEquals(
      1,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent2").get()
        )
        .size()
    );

    // decommission frozen agent1
    Assertions.assertEquals(
      StateChangeResult.SUCCESS,
      agentManager.changeState(
        "agent1",
        MachineState.STARTING_DECOMMISSION,
        Optional.empty(),
        Optional.of("user1")
      )
    );

    scheduler.checkForDecomissions();
    scheduler.drainPendingQueue();
    resourceOffers();
    cleaner.drainCleanupQueue();

    // assert agent1 is decommissioning
    Assertions.assertTrue(
      agentManager.getObject("agent1").get().getCurrentState().getState() ==
      MachineState.DECOMMISSIONING
    );

    // mark tasks as running
    for (SingularityTask task : taskManager.getTasksOnAgent(
      taskManager.getActiveTaskIds(),
      agentManager.getObject("agent2").get()
    )) {
      statusUpdate(task, TaskState.TASK_RUNNING);
    }

    // all tasks should have moved
    cleaner.drainCleanupQueue();

    // kill decommissioned task
    statusUpdate(
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent1").get()
        )
        .get(0),
      TaskState.TASK_KILLED
    );

    // assert all tasks on agent2 + agent1 is decommissioned
    Assertions.assertEquals(
      0,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent1").get()
        )
        .size()
    );
    Assertions.assertEquals(
      2,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent2").get()
        )
        .size()
    );
    Assertions.assertTrue(
      agentManager.getObject("agent1").get().getCurrentState().getState() ==
      MachineState.DECOMMISSIONED
    );
  }

  @Test
  public void testLoadSlavesFromMasterDataOnStartup() {
    MesosMasterStateObject state = getMasterState(3);
    singularityAgentAndRackManager.loadAgentsAndRacksFromMaster(state, true);

    List<SingularityAgent> agents = agentManager.getObjects();

    Assertions.assertEquals(3, agents.size());
    for (SingularityAgent agent : agents) {
      Assertions.assertEquals(MachineState.ACTIVE, agent.getCurrentState().getState());
    }
  }

  @Test
  public void testReconcileSlaves() {
    // Load 3 agents on startup
    MesosMasterStateObject state = getMasterState(3);
    singularityAgentAndRackManager.loadAgentsAndRacksFromMaster(state, true);

    MesosMasterStateObject newState = getMasterState(2); // 2 agents, third has died
    singularityAgentAndRackManager.loadAgentsAndRacksFromMaster(newState, false);
    List<SingularityAgent> agents = agentManager.getObjects();

    Assertions.assertEquals(3, agents.size());

    for (SingularityAgent agent : agents) {
      if (agent.getId().equals("2")) {
        Assertions.assertEquals(MachineState.DEAD, agent.getCurrentState().getState());
      } else {
        Assertions.assertEquals(MachineState.ACTIVE, agent.getCurrentState().getState());
      }
    }
  }

  @Test
  public void testReconcileSlavesOnStartup() {
    // Load 3 agents on startup
    MesosMasterStateObject state = getMasterState(3);
    singularityAgentAndRackManager.loadAgentsAndRacksFromMaster(state, true);

    // Load 2 agents on startup
    MesosMasterStateObject newState = getMasterState(2); // 2 agents, third has died
    singularityAgentAndRackManager.loadAgentsAndRacksFromMaster(newState, true);
    List<SingularityAgent> agents = agentManager.getObjects();

    Assertions.assertEquals(3, agents.size());

    for (SingularityAgent agent : agents) {
      if (agent.getId().equals("2")) {
        Assertions.assertEquals(
          MachineState.MISSING_ON_STARTUP,
          agent.getCurrentState().getState()
        );
      } else {
        Assertions.assertEquals(MachineState.ACTIVE, agent.getCurrentState().getState());
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

    List<MesosMasterAgentObject> agents = new ArrayList<>();
    for (Integer i = 0; i < numSlaves; i++) {
      agents.add(
        new MesosMasterAgentObject(
          i.toString(),
          i.toString(),
          String.format("localhost:505%s", i),
          now,
          new MesosResourcesObject(resources),
          attributes,
          new MesosResourcesObject(resources),
          new MesosResourcesObject(resources),
          new MesosResourcesObject(resources),
          new MesosResourcesObject(resources),
          "",
          true
        )
      );
    }

    MesosFrameworkObject framework = new MesosFrameworkObject(
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      now,
      now,
      now,
      true,
      true,
      new MesosResourcesObject(resources),
      new MesosResourcesObject(resources),
      new MesosResourcesObject(resources),
      Collections.emptyList()
    );

    return new MesosMasterStateObject(
      "",
      "",
      "",
      "",
      now,
      "",
      now,
      now,
      "",
      "",
      "",
      0,
      0,
      "",
      "",
      "",
      Collections.emptyMap(),
      agents,
      Collections.singletonList(framework)
    );
  }

  @Test
  public void testExpiringMachineState() {
    MesosMasterStateObject state = getMasterState(1);
    singularityAgentAndRackManager.loadAgentsAndRacksFromMaster(state, true);

    SingularityAgent agent = agentManager.getObjects().get(0);

    agentResource.freezeAgent(
      singularityUser,
      agent.getId(),
      new SingularityMachineChangeRequest(
        Optional.of(1L),
        Optional.empty(),
        Optional.empty(),
        Optional.of(MachineState.ACTIVE),
        Optional.empty()
      )
    );

    Assertions.assertEquals(
      MachineState.FROZEN,
      agentManager.getObjects().get(0).getCurrentState().getState()
    );

    try {
      Thread.sleep(10);
    } catch (Exception e) {
      // didn't see that
    }

    expiringUserActionPoller.runActionOnPoll();

    Assertions.assertEquals(
      MachineState.ACTIVE,
      agentManager.getObjects().get(0).getCurrentState().getState()
    );
  }

  private SingularityAgent getSingleSlave() {
    MesosMasterStateObject state = getMasterState(1);
    singularityAgentAndRackManager.loadAgentsAndRacksFromMaster(state, true);
    return agentManager.getObjects().get(0);
  }

  @Test
  public void testCannotUseStateReservedForSystem() {
    SingularityAgent agent = getSingleSlave();
    Assertions.assertThrows(
      WebApplicationException.class,
      () ->
        agentResource.freezeAgent(
          singularityUser,
          agent.getId(),
          new SingularityMachineChangeRequest(
            Optional.of(1L),
            Optional.empty(),
            Optional.empty(),
            Optional.of(MachineState.DEAD),
            Optional.empty()
          )
        )
    );
  }

  @Test
  public void testBadExpiringStateTransition() {
    SingularityAgent agent = getSingleSlave();
    Assertions.assertThrows(
      WebApplicationException.class,
      () ->
        agentResource.decommissionAgent(
          singularityUser,
          agent.getId(),
          new SingularityMachineChangeRequest(
            Optional.of(1L),
            Optional.empty(),
            Optional.empty(),
            Optional.of(MachineState.FROZEN),
            Optional.empty()
          )
        )
    );
  }

  @Test
  public void testInvalidTransitionToDecommissioned() {
    SingularityAgent agent = getSingleSlave();
    Assertions.assertThrows(
      WebApplicationException.class,
      () ->
        agentResource.decommissionAgent(
          singularityUser,
          agent.getId(),
          new SingularityMachineChangeRequest(
            Optional.of(1L),
            Optional.empty(),
            Optional.empty(),
            Optional.of(MachineState.DECOMMISSIONED),
            Optional.empty()
          )
        )
    );
  }

  @Test
  public void testValidTransitionToDecommissioned() {
    initRequest();
    initFirstDeploy();
    requestResource.postRequest(
      request.toBuilder().setInstances(Optional.of(2)).build(),
      singularityUser
    );
    scheduler.drainPendingQueue();
    resourceOffers(1);
    SingularityAgent agent = agentManager.getObjects().get(0);

    agentResource.decommissionAgent(
      singularityUser,
      agent.getId(),
      new SingularityMachineChangeRequest(
        Optional.of(1L),
        Optional.empty(),
        Optional.empty(),
        Optional.of(MachineState.DECOMMISSIONED),
        Optional.of(true)
      )
    );
    Assertions.assertEquals(
      MachineState.STARTING_DECOMMISSION,
      agentManager.getObjects().get(0).getCurrentState().getState()
    );
    scheduler.checkForDecomissions();
    scheduler.drainPendingQueue();
    Assertions.assertEquals(
      TaskCleanupType.DECOMISSIONING,
      taskManager.getCleanupTasks().get(0).getCleanupType()
    );

    expiringUserActionPoller.runActionOnPoll();
    Assertions.assertEquals(
      MachineState.DECOMMISSIONED,
      agentManager.getObjects().get(0).getCurrentState().getState()
    );
    Assertions.assertEquals(
      TaskCleanupType.DECOMMISSION_TIMEOUT,
      taskManager.getCleanupTasks().get(0).getCleanupType()
    );
  }

  @Test
  public void testSystemChangeClearsExpiringChangeIfInvalid() {
    SingularityAgent agent = getSingleSlave();
    agentResource.freezeAgent(singularityUser, agent.getId(), null);
    agentResource.activateAgent(
      singularityUser,
      agent.getId(),
      new SingularityMachineChangeRequest(
        Optional.of(1L),
        Optional.empty(),
        Optional.empty(),
        Optional.of(MachineState.FROZEN),
        Optional.empty()
      )
    );
    Assertions.assertTrue(agentManager.getExpiringObject(agent.getId()).isPresent());
    agentResource.decommissionAgent(singularityUser, agent.getId(), null);
    Assertions.assertFalse(agentManager.getExpiringObject(agent.getId()).isPresent());
  }

  @Test
  public void itShouldContainAnInactiveHostWhenHostDeactivated() {
    inactiveAgentManager.deactivateAgent("localhost");

    Assertions.assertTrue(inactiveAgentManager.getInactiveAgents().contains("localhost"));
  }

  @Test
  public void itShouldNotContainHostAfterActivatingHost() {
    inactiveAgentManager.deactivateAgent("localhost");
    inactiveAgentManager.activateAgent("localhost");

    Assertions.assertFalse(
      inactiveAgentManager.getInactiveAgents().contains("localhost")
    );
  }

  @Test
  public void itShouldMarkSlavesFromInactiveHostAsDecommissioned() {
    inactiveAgentManager.deactivateAgent("host1");

    resourceOffers();
    SingularityAgent agent = agentManager.getObject("agent1").get();
    Assertions.assertTrue(agent.getCurrentState().getState().isDecommissioning());
  }

  @Test
  public void testSlavePlacementSeparate() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(
      request
        .toBuilder()
        .setInstances(Optional.of(2))
        .setAgentPlacement(Optional.of(AgentPlacement.SEPARATE))
    );

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(20, 20000, 50000, "agent1", "host1"),
          createOffer(20, 20000, 50000, "agent1", "host1")
        )
      )
      .join();

    Assertions.assertTrue(taskManager.getPendingTaskIds().size() == 1);
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    sms
      .resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "agent1", "host1")))
      .join();

    Assertions.assertTrue(taskManager.getPendingTaskIds().size() == 1);
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    sms
      .resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "agent1", "host1")))
      .join();

    Assertions.assertTrue(taskManager.getPendingTaskIds().size() == 1);
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    sms
      .resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "agent2", "host2")))
      .join();

    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 2);
  }

  @Test
  public void testAgentPlacementOverride() {
    initRequest();
    initFirstDeployWithResources(10, 10000);

    saveAndSchedule(
      request
        .toBuilder()
        .setInstances(Optional.of(4))
        .setAgentPlacement(Optional.of(AgentPlacement.SEPARATE))
    );

    // initially respects separate placement
    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(20, 10000, 50000, "agent1", "host1"),
          createOffer(20, 10000, 50000, "agent1", "host1")
        )
      )
      .join();

    Assertions.assertTrue(taskManager.getPendingTaskIds().size() == 3);
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    // with override, ignores request placement strategy
    // note - if we ever bother parallelizing the tests this'll cause issues with other placement tests
    overrides.setAgentPlacementOverride(Optional.of(AgentPlacement.GREEDY));
    sms
      .resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "agent1", "host1")))
      .join();

    Assertions.assertTrue(taskManager.getPendingTaskIds().size() == 1);
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 3);

    // without override, respects request placement strategy but leaves running tasks
    overrides.setAgentPlacementOverride(Optional.empty());
    sms
      .resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "agent1", "host1")))
      .join();

    Assertions.assertTrue(taskManager.getPendingTaskIds().size() == 1);
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 3);
  }

  @Test
  public void testSlavePlacementSpread() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(
      request
        .toBuilder()
        .setInstances(Optional.of(1))
        .setAgentPlacement(Optional.of(AgentPlacement.SPREAD_ALL_AGENTS))
    );

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(20, 20000, 50000, "agent1", "host1", Optional.of("rack1"))
        )
      )
      .join();

    // assert one Request on one agent.
    Assertions.assertTrue(agentManager.getNumObjectsAtState(MachineState.ACTIVE) == 1);
    Assertions.assertTrue(taskManager.getPendingTaskIds().size() == 0);
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    sms
      .resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "agent2", "host2")))
      .join();
    Assertions.assertTrue(agentManager.getNumObjectsAtState(MachineState.ACTIVE) == 2);

    spreadAllPoller.runActionOnPoll();
    scheduler.drainPendingQueue();

    sms
      .resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "agent2", "host2")))
      .join();

    // assert Request is spread over the two agents
    Assertions.assertTrue(taskManager.getPendingTaskIds().size() == 0);
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 2);
    Assertions.assertEquals(
      1,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent1").get()
        )
        .size()
    );
    Assertions.assertEquals(
      1,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent2").get()
        )
        .size()
    );

    // decommission agent and kill task
    agentManager.changeState(
      "agent2",
      MachineState.FROZEN,
      Optional.<String>empty(),
      Optional.<String>empty()
    );
    agentManager.changeState(
      "agent2",
      MachineState.STARTING_DECOMMISSION,
      Optional.<String>empty(),
      Optional.<String>empty()
    );
    cleaner.drainCleanupQueue();
    statusUpdate(
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent2").get()
        )
        .get(0),
      TaskState.TASK_KILLED
    );

    spreadAllPoller.runActionOnPoll();
    scheduler.drainPendingQueue();

    Assertions.assertTrue(taskManager.getPendingTaskIds().isEmpty());
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testSlavePlacementOptimistic() {
    initRequest();
    initFirstDeploy();

    saveAndSchedule(
      request
        .toBuilder()
        .setInstances(Optional.of(20))
        .setAgentPlacement(Optional.of(AgentPlacement.OPTIMISTIC))
    );

    // Default behavior if we don't have info about other hosts that can run this task: be greedy.
    sms
      .resourceOffers(Arrays.asList(createOffer(2, 128 * 2, 1024 * 2, "agent1", "host1")))
      .join();
    Assertions.assertEquals(2, taskManager.getActiveTaskIds().size());

    // Now that at least one other host is running tasks for this request, we expect an even-ish spread,
    // but because we have many tasks pending, we allow quite a bit of unevenness.
    sms
      .resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "agent2", "host2")))
      .join();
    Assertions.assertEquals(13, taskManager.getActiveTaskIds().size());

    // ...but now we won't schedule more tasks on host2, because it's hosting a disproportionate number of tasks.
    sms
      .resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "agent2", "host2")))
      .join();
    Assertions.assertEquals(13, taskManager.getActiveTaskIds().size());

    // ...but since host1 is only hosting 2 tasks, we will schedule more tasks on it when an offer is received.
    sms
      .resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "agent1", "host1")))
      .join();
    Assertions.assertEquals(20, taskManager.getActiveTaskIds().size());

    Map<String, List<SingularityTaskId>> tasksByHost = taskManager
      .getActiveTaskIdsForRequest(request.getId())
      .stream()
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

    saveAndSchedule(
      request
        .toBuilder()
        .setInstances(Optional.of(3))
        .setAgentPlacement(Optional.of(AgentPlacement.GREEDY))
    );

    sms
      .resourceOffers(Arrays.asList(createOffer(20, 20000, 50000, "agent1", "host1")))
      .join();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 3);
  }

  @Test
  public void testReservedSlaveAttribute() {
    Map<String, List<String>> reservedAttributes = new HashMap<>();
    reservedAttributes.put("reservedKey", Arrays.asList("reservedValue1"));
    configuration.setReserveAgentsWithAttributes(reservedAttributes);

    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)));

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent1",
            "host1",
            Optional.<String>empty(),
            ImmutableMap.of("reservedKey", "reservedValue1")
          )
        )
      )
      .join();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent2",
            "host2",
            Optional.<String>empty(),
            ImmutableMap.of("reservedKey", "notAReservedValue")
          )
        )
      )
      .join();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testReservedSlaveWithMatchinRequestAttribute() {
    Map<String, List<String>> reservedAttributes = new HashMap<>();
    reservedAttributes.put("reservedKey", Arrays.asList("reservedValue1"));
    configuration.setReserveAgentsWithAttributes(reservedAttributes);

    Map<String, String> reservedAttributesMap = ImmutableMap.of(
      "reservedKey",
      "reservedValue1"
    );
    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)));

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent1",
            "host1",
            Optional.<String>empty(),
            reservedAttributesMap
          )
        )
      )
      .join();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    saveAndSchedule(
      request
        .toBuilder()
        .setInstances(Optional.of(1))
        .setRequiredAgentAttributes(Optional.of(reservedAttributesMap))
    );

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent1",
            "host1",
            Optional.<String>empty(),
            ImmutableMap.of("reservedKey", "reservedValue1")
          )
        )
      )
      .join();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testAllowedSlaveAttributes() {
    Map<String, List<String>> reservedAttributes = new HashMap<>();
    reservedAttributes.put("reservedKey", Arrays.asList("reservedValue1"));
    configuration.setReserveAgentsWithAttributes(reservedAttributes);

    Map<String, String> allowedAttributes = new HashMap<>();
    allowedAttributes.put("reservedKey", "reservedValue1");

    initRequest();
    initFirstDeploy();
    saveAndSchedule(request.toBuilder().setInstances(Optional.of(1)));

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent1",
            "host1",
            Optional.<String>empty(),
            ImmutableMap.of("reservedKey", "reservedValue1")
          )
        )
      )
      .join();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    saveAndSchedule(
      request
        .toBuilder()
        .setInstances(Optional.of(1))
        .setAllowedAgentAttributes(Optional.of(allowedAttributes))
    );

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent1",
            "host1",
            Optional.<String>empty(),
            ImmutableMap.of("reservedKey", "reservedValue1")
          )
        )
      )
      .join();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testRequiredSlaveAttributesForRequest() {
    Map<String, String> requiredAttributes = new HashMap<>();
    requiredAttributes.put("requiredKey", "requiredValue1");

    initRequest();
    initFirstDeploy();
    saveAndSchedule(
      request
        .toBuilder()
        .setInstances(Optional.of(1))
        .setRequiredAgentAttributes(Optional.of(requiredAttributes))
    );

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent1",
            "host1",
            Optional.<String>empty(),
            ImmutableMap.of("requiredKey", "notTheRightValue")
          )
        )
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent2",
            "host2",
            Optional.<String>empty(),
            ImmutableMap.of("notTheRightKey", "requiredValue1")
          )
        )
      )
      .join();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent2",
            "host2",
            Optional.<String>empty(),
            requiredAttributes
          )
        )
      )
      .join();

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
        .setRequiredAgentAttributeOverrides(requiredAttributes)
        .build()
    );

    scheduler.drainPendingQueue();

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent1",
            "host1",
            Optional.<String>empty(),
            ImmutableMap.of("requiredKey", "notTheRightValue")
          )
        )
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent2",
            "host2",
            Optional.<String>empty(),
            ImmutableMap.of("notTheRightKey", "requiredValue1")
          )
        )
      )
      .join();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent2",
            "host2",
            Optional.<String>empty(),
            requiredAttributes
          )
        )
      )
      .join();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testMultipleRequiredAttributes() {
    Map<String, String> requiredAttributes = new HashMap<>();
    requiredAttributes.put("requiredKey1", "requiredValue1");
    requiredAttributes.put("requiredKey2", "requiredValue2");

    initRequest();
    initFirstDeploy();
    saveAndSchedule(
      request
        .toBuilder()
        .setInstances(Optional.of(1))
        .setRequiredAgentAttributes(Optional.of(requiredAttributes))
    );

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent1",
            "host1",
            Optional.<String>empty(),
            ImmutableMap.of("requiredKey1", "requiredValue1")
          )
        )
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent2",
            "host2",
            Optional.<String>empty(),
            ImmutableMap.of(
              "requiredKey1",
              "requiredValue1",
              "someotherkey",
              "someothervalue"
            )
          )
        )
      )
      .join();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent2",
            "host2",
            Optional.<String>empty(),
            requiredAttributes
          )
        )
      )
      .join();

    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }

  @Test
  public void testEvenRackPlacement() {
    // Set up 3 active racks
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent2", "host2", Optional.of("rack2")))
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent3", "host3", Optional.of("rack3")))
      )
      .join();

    initRequest();
    initFirstDeploy();
    saveAndSchedule(
      request.toBuilder().setInstances(Optional.of(7)).setRackSensitive(Optional.of(true))
    );

    // rack1 -> 1, rack2 -> 2, rack3 -> 3
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent2", "host2", Optional.of("rack2")))
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent3", "host3", Optional.of("rack3")))
      )
      .join();

    Assertions.assertEquals(3, taskManager.getActiveTaskIds().size());

    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    Assertions.assertEquals(4, taskManager.getActiveTaskIds().size());

    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    Assertions.assertEquals(4, taskManager.getActiveTaskIds().size());

    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent2", "host2", Optional.of("rack2")))
      )
      .join();
    Assertions.assertEquals(5, taskManager.getActiveTaskIds().size());

    // rack1 should not get a third instance until rack3 has a second
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    Assertions.assertEquals(5, taskManager.getActiveTaskIds().size());

    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent3", "host3", Optional.of("rack3")))
      )
      .join();
    Assertions.assertEquals(6, taskManager.getActiveTaskIds().size());

    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    Assertions.assertEquals(7, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testRackSensitiveOverride() {
    // Set up 3 active racks
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent2", "host2", Optional.of("rack2")))
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent3", "host3", Optional.of("rack3")))
      )
      .join();

    initRequest();
    initFirstDeploy();
    saveAndSchedule(
      request.toBuilder().setInstances(Optional.of(7)).setRackSensitive(Optional.of(true))
    );

    // rack1 -> 1, rack2 -> 2, rack3 -> 3
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent2", "host2", Optional.of("rack2")))
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent3", "host3", Optional.of("rack3")))
      )
      .join();

    Assertions.assertEquals(3, taskManager.getActiveTaskIds().size());

    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    Assertions.assertEquals(4, taskManager.getActiveTaskIds().size());

    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    Assertions.assertEquals(4, taskManager.getActiveTaskIds().size());

    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent2", "host2", Optional.of("rack2")))
      )
      .join();
    Assertions.assertEquals(5, taskManager.getActiveTaskIds().size());

    // with the override, rack1 should get a third instance despite rack3 not having a second
    overrides.setAllowRackSensitivity(false);
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    Assertions.assertEquals(6, taskManager.getActiveTaskIds().size());

    // without the override, rack1 should not get another instance
    overrides.setAllowRackSensitivity(true);
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    Assertions.assertEquals(6, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testRackPlacementOnScaleDown() {
    try {
      configuration.setRebalanceRacksOnScaleDown(true);
      // Set up 3 active racks
      sms
        .resourceOffers(
          Arrays.asList(createOffer(0.1, 1, 1, "agent1", "host1", Optional.of("agent1")))
        )
        .join();
      sms
        .resourceOffers(
          Arrays.asList(createOffer(0.1, 1, 1, "agent2", "host2", Optional.of("agent2")))
        )
        .join();
      sms
        .resourceOffers(
          Arrays.asList(createOffer(0.1, 1, 1, "agent3", "host3", Optional.of("agent3")))
        )
        .join();

      request =
        new SingularityRequestBuilder(requestId, RequestType.WORKER)
          .setInstances(Optional.of(7))
          .setRackSensitive(Optional.of(true))
          .build();
      saveRequest(request);
      initFirstDeploy();
      launchTask(
        request,
        firstDeploy,
        System.currentTimeMillis(),
        System.currentTimeMillis(),
        1,
        TaskState.TASK_RUNNING,
        true,
        Optional.empty(),
        Optional.of("agent1")
      );
      launchTask(
        request,
        firstDeploy,
        System.currentTimeMillis(),
        System.currentTimeMillis(),
        2,
        TaskState.TASK_RUNNING,
        true,
        Optional.empty(),
        Optional.of("agent1")
      );
      launchTask(
        request,
        firstDeploy,
        System.currentTimeMillis(),
        System.currentTimeMillis(),
        3,
        TaskState.TASK_RUNNING,
        true,
        Optional.empty(),
        Optional.of("agent2")
      );
      launchTask(
        request,
        firstDeploy,
        System.currentTimeMillis(),
        System.currentTimeMillis(),
        4,
        TaskState.TASK_RUNNING,
        true,
        Optional.empty(),
        Optional.of("agent2")
      );
      launchTask(
        request,
        firstDeploy,
        System.currentTimeMillis(),
        System.currentTimeMillis(),
        5,
        TaskState.TASK_RUNNING,
        true,
        Optional.empty(),
        Optional.of("agent3")
      );
      launchTask(
        request,
        firstDeploy,
        System.currentTimeMillis(),
        System.currentTimeMillis(),
        6,
        TaskState.TASK_RUNNING,
        true,
        Optional.empty(),
        Optional.of("agent3")
      );
      launchTask(
        request,
        firstDeploy,
        System.currentTimeMillis(),
        System.currentTimeMillis(),
        7,
        TaskState.TASK_RUNNING,
        true,
        Optional.empty(),
        Optional.of("agent3")
      );

      requestResource.postRequest(
        request
          .toBuilder()
          .setInstances(Optional.of(4))
          .setRackSensitive(Optional.of(true))
          .build(),
        singularityUser
      );

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
  public void testItRespectsExpectedRackConfiguration() {
    Optional<Integer> original = configuration.getExpectedRacksCount();

    try {
      // Tell Singularity to expect 2 racks
      configuration.setExpectedRacksCount(Optional.of(2));

      // Set up 4 active racks
      sms
        .resourceOffers(
          Arrays.asList(
            createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1"))
          )
        )
        .join();
      sms
        .resourceOffers(
          Arrays.asList(
            createOffer(1, 128, 1024, "agent2", "host2", Optional.of("rack2"))
          )
        )
        .join();
      sms
        .resourceOffers(
          Arrays.asList(
            createOffer(1, 128, 1024, "agent3", "host3", Optional.of("rack3"))
          )
        )
        .join();
      sms
        .resourceOffers(
          Arrays.asList(
            createOffer(1, 128, 1024, "agent4", "host4", Optional.of("rack4"))
          )
        )
        .join();

      initRequest();
      initFirstDeploy();
      saveAndSchedule(
        request
          .toBuilder()
          .setInstances(Optional.of(7))
          .setRackSensitive(Optional.of(true))
      );

      // tasks per rack = ceil(7 / 2), not ceil(7 / 4)
      sms
        .resourceOffers(
          Arrays.asList(
            createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1"))
          )
        )
        .join();
      sms
        .resourceOffers(
          Arrays.asList(
            createOffer(1, 128, 1024, "agent2", "host2", Optional.of("rack2"))
          )
        )
        .join();
      sms
        .resourceOffers(
          Arrays.asList(
            createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1"))
          )
        )
        .join();
      sms
        .resourceOffers(
          Arrays.asList(
            createOffer(1, 128, 1024, "agent2", "host2", Optional.of("rack2"))
          )
        )
        .join();
      sms
        .resourceOffers(
          Arrays.asList(
            createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1"))
          )
        )
        .join();
      sms
        .resourceOffers(
          Arrays.asList(
            createOffer(1, 128, 1024, "agent2", "host2", Optional.of("rack2"))
          )
        )
        .join();
      sms
        .resourceOffers(
          Arrays.asList(
            createOffer(1, 128, 1024, "agent3", "host3", Optional.of("rack3"))
          )
        )
        .join();

      // everything should be scheduled
      Assertions.assertEquals(7, taskManager.getActiveTaskIds().size());
    } finally {
      configuration.setExpectedRacksCount(original);
    }
  }

  @Test
  public void testPlacementOfBounceTasks() {
    // Set up 1 active rack
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();

    initRequest();
    initFirstDeploy();
    SingularityRequest newRequest = request
      .toBuilder()
      .setInstances(Optional.of(2))
      .setRackSensitive(Optional.of(true))
      .setAgentPlacement(Optional.of(AgentPlacement.SEPARATE))
      .setAllowBounceToSameHost(Optional.of(true))
      .build();
    saveAndSchedule(newRequest.toBuilder());
    scheduler.drainPendingQueue();

    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent2", "host2", Optional.of("rack1")))
      )
      .join();
    Assertions.assertEquals(2, taskManager.getActiveTaskIds().size());

    requestResource.bounce(requestId, Optional.empty(), singularityUser);
    cleaner.drainCleanupQueue();
    scheduler.drainPendingQueue();

    Assertions.assertEquals(2, taskManager.getNumCleanupTasks());
    Assertions.assertEquals(2, taskManager.getPendingTaskIds().size());
    Assertions.assertEquals(
      taskManager.getCleanupTasks().get(0).getActionId().get(),
      taskManager.getPendingTasks().get(0).getActionId().get()
    );

    // BOUNCE should allow a task to launch on the same host
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    Assertions.assertEquals(3, taskManager.getActiveTaskIds().size());

    // But not a second one from the same bounce
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    Assertions.assertEquals(3, taskManager.getActiveTaskIds().size());

    // Other pending type should not allow tasks on same host
    saveAndSchedule(newRequest.toBuilder().setInstances(Optional.of(2)));
    sms
      .resourceOffers(
        Arrays.asList(createOffer(1, 128, 1024, "agent1", "host1", Optional.of("rack1")))
      )
      .join();
    Assertions.assertEquals(3, taskManager.getActiveTaskIds().size());
  }

  @Test
  public void testSlaveAttributeMinimumsAreNotForciblyViolated() {
    Map<String, List<String>> reservedAttributes = new HashMap<>();
    reservedAttributes.put("instance_lifecycle_type", Arrays.asList("spot"));
    configuration.setReserveAgentsWithAttributes(reservedAttributes);

    Map<String, String> allowedAttributes = new HashMap<>();
    allowedAttributes.put("instance_lifecycle_type", "spot");

    Map<String, Map<String, Integer>> attributeMinimums = new HashMap<>();
    attributeMinimums.put("instance_lifecycle_type", ImmutableMap.of("non_spot", 70));

    initRequest();
    initFirstDeploy();

    saveAndSchedule(
      request
        .toBuilder()
        .setInstances(Optional.of(10))
        .setAllowedAgentAttributes(Optional.of(allowedAttributes))
        .setAgentAttributeMinimums(Optional.of(attributeMinimums))
    );

    // The schedule should only accept as many "spot" instances so as to not force a violation of the minimum "non_spot" instances
    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent1",
            "host1",
            Optional.<String>empty(),
            ImmutableMap.of("instance_lifecycle_type", "spot")
          )
        )
      )
      .join();
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 3);
    Assertions.assertEquals(
      3,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent1").get()
        )
        .size()
    );

    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent2",
            "host2",
            Optional.<String>empty(),
            ImmutableMap.of("instance_lifecycle_type", "non_spot")
          )
        )
      )
      .join();
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 10);
    Assertions.assertEquals(
      3,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent1").get()
        )
        .size()
    );
    Assertions.assertEquals(
      7,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent2").get()
        )
        .size()
    );
  }

  @Test
  public void testSlaveAttributeMinimumsCanBeExceeded() {
    Map<String, List<String>> reservedAttributes = new HashMap<>();
    reservedAttributes.put("instance_lifecycle_type", Arrays.asList("spot"));
    configuration.setReserveAgentsWithAttributes(reservedAttributes);

    Map<String, String> allowedAttributes = new HashMap<>();
    allowedAttributes.put("instance_lifecycle_type", "spot");

    Map<String, Map<String, Integer>> attributeMinimums = new HashMap<>();
    attributeMinimums.put("instance_lifecycle_type", ImmutableMap.of("non_spot", 70));

    initRequest();
    initFirstDeploy();

    saveAndSchedule(
      request
        .toBuilder()
        .setInstances(Optional.of(10))
        .setAllowedAgentAttributes(Optional.of(allowedAttributes))
        .setAgentAttributeMinimums(Optional.of(attributeMinimums))
    );

    // Ensure we can go over the minimum if there are enough resources available
    sms
      .resourceOffers(
        Arrays.asList(
          createOffer(
            20,
            20000,
            50000,
            "agent1",
            "host1",
            Optional.<String>empty(),
            ImmutableMap.of("instance_lifecycle_type", "non_spot")
          )
        )
      )
      .join();
    Assertions.assertTrue(taskManager.getActiveTaskIds().size() == 10);
    Assertions.assertEquals(
      10,
      taskManager
        .getTasksOnAgent(
          taskManager.getActiveTaskIds(),
          agentManager.getObject("agent1").get()
        )
        .size()
    );
  }
}
