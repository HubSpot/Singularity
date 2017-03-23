package com.hubspot.singularity.data;

import org.junit.Assert;
import org.junit.Test;

import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.scheduler.SingularitySchedulerTestBase;

public class InactiveSlaveManagerTest extends SingularitySchedulerTestBase {
  public InactiveSlaveManagerTest() {
    super(false);
  }

  @Test
  public void itShouldContainAnInactiveHostWhenHostDeactivated() {
    inactiveSlaveManager.deactivateSlave("localhost");

    Assert.assertTrue(inactiveSlaveManager.getInactiveSlaves().contains("localhost"));
  }

  @Test
  public void itShouldNotContainHostAfterActivatingHost() {
    inactiveSlaveManager.deactivateSlave("localhost");
    inactiveSlaveManager.activateSlave("localhost");

    Assert.assertFalse(inactiveSlaveManager.getInactiveSlaves().contains("localhost"));
  }

  @Test
  public void itShouldMarkSlavesFromInactiveHostAsDecommissioned() {
    inactiveSlaveManager.deactivateSlave("host1");

    resourceOffers();
    SingularitySlave slave = slaveManager.getObject("slave1").get();
    Assert.assertTrue(slave.getCurrentState().getState().isDecommissioning());
  }
}
