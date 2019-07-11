package com.hubspot.singularity.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.scheduler.SingularitySchedulerTestBase;

public class InactiveSlaveManagerTest extends SingularitySchedulerTestBase {
  public InactiveSlaveManagerTest() {
    super(false);
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
}
