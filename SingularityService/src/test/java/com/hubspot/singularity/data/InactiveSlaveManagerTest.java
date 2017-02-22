package com.hubspot.singularity.data;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTestBaseNoDb;

public class InactiveSlaveManagerTest extends SingularityTestBaseNoDb {
  @Inject
  InactiveSlaveManager inactiveSlaveManager;

  @Test
  public void itShouldContainAnInactiveHostWhenHostDeactivated() {
    inactiveSlaveManager.deactiveSlave("localhost");

    Assert.assertTrue(inactiveSlaveManager.getInactiveSlaves().contains("localhost"));
  }

  @Test
  public void itShouldNotContainHostAfterActivatingHost() {
    inactiveSlaveManager.deactiveSlave("localhost");
    inactiveSlaveManager.activateSlave("localhost");

    Assert.assertFalse(inactiveSlaveManager.getInactiveSlaves().contains("localhost"));
  }
}
