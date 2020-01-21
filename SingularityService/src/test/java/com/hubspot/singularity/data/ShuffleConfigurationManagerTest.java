package com.hubspot.singularity.data;

import com.google.inject.Inject;
import com.hubspot.singularity.scheduler.SingularitySchedulerTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ShuffleConfigurationManagerTest extends SingularitySchedulerTestBase {

  protected String r1 = "1";
  protected String r2 = "2";

  @Inject
  protected ShuffleConfigurationManager shuffleCfgManager;

  public ShuffleConfigurationManagerTest() {
    super(false);
  }

  @Test
  public void itSupportsBlacklistingRequestIds() {
    shuffleCfgManager.addToShuffleBlacklist(r1);
    shuffleCfgManager.addToShuffleBlacklist(r2);

    Assertions.assertTrue(shuffleCfgManager.isOnShuffleBlacklist(r1));
    Assertions.assertTrue(shuffleCfgManager.isOnShuffleBlacklist(r2));
    Assertions.assertTrue(shuffleCfgManager.getShuffleBlacklist().contains(r1));
    Assertions.assertTrue(shuffleCfgManager.getShuffleBlacklist().contains(r2));
  }

  @Test
  public void itSupportsRemovingBlacklistedRequestIds() {
    shuffleCfgManager.addToShuffleBlacklist(r1);
    shuffleCfgManager.removeFromShuffleBlacklist(r1);

    Assertions.assertFalse(shuffleCfgManager.isOnShuffleBlacklist(r1));
  }

  @Test
  public void itDoesNotBlacklistUnrecognizedRequestIds() {
    Assertions.assertFalse(shuffleCfgManager.isOnShuffleBlacklist("unrecognized"));
  }
}
