package com.hubspot.singularity.scheduler;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityCuratorTestBase;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.SlaveManager;


public class SlaveTest extends SingularityCuratorTestBase {

  @Inject
  protected SlaveManager slaveManager;

  @Inject
  protected SingularityConfiguration configuration;

  @Inject
  protected SingularityDeadSlavePoller deadSlavePoller;

  public SlaveTest() {
    super(false);
  }

  @Test
  public void testDeadSlavesArePurged() {
    SingularitySlave liveSlave = new SingularitySlave("1", "h1", "r1", ImmutableMap.of("uniqueAttribute", "1"));
    SingularitySlave deadSlave = new SingularitySlave("2", "h1", "r1", ImmutableMap.of("uniqueAttribute", "2"));

    final long now = System.currentTimeMillis();

    liveSlave = liveSlave.changeState(new SingularityMachineStateHistoryUpdate("1", MachineState.ACTIVE, 100, Optional.<String> absent(), Optional.<String> absent()));
    deadSlave = deadSlave.changeState(new SingularityMachineStateHistoryUpdate("2", MachineState.DEAD, now - TimeUnit.HOURS.toMillis(10), Optional.<String> absent(), Optional.<String> absent()));

    slaveManager.saveObject(liveSlave);
    slaveManager.saveObject(deadSlave);

    deadSlavePoller.runActionOnPoll();

    Assert.assertEquals(1, slaveManager.getObjectsFiltered(MachineState.ACTIVE).size());
    Assert.assertEquals(1, slaveManager.getObjectsFiltered(MachineState.DEAD).size());

    configuration.setDeleteDeadSlavesAfterHours(1);

    deadSlavePoller.runActionOnPoll();

    Assert.assertEquals(1, slaveManager.getObjectsFiltered(MachineState.ACTIVE).size());
    Assert.assertEquals(0, slaveManager.getObjectsFiltered(MachineState.DEAD).size());
  }

}
