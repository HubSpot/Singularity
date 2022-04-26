package com.hubspot.singularity.data;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.hubspot.singularity.scheduler.SingularitySchedulerTestBase;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ShuffleConfigurationManagerTest extends SingularitySchedulerTestBase {

  protected String r1 = "1";
  protected String r2 = "2";

  @Inject
  protected ShuffleConfigurationManager shuffleCfgManager;

  @Inject
  protected MetricRegistry metricRegistry;

  @Inject
  public ShuffleConfigurationManagerTest() {
    super(
      false,
      cfg -> {
        cfg.setDoNotShuffleRequests(Arrays.asList("no-shuffle"));
        return null;
      }
    );
  }

  @Test
  public void itSupportsBlacklistingRequestIds() {
    shuffleCfgManager.addToShuffleBlocklist(r1);
    shuffleCfgManager.addToShuffleBlocklist(r2);

    Assertions.assertTrue(shuffleCfgManager.isOnShuffleBlocklist(r1));
    Assertions.assertTrue(shuffleCfgManager.isOnShuffleBlocklist(r2));
    Assertions.assertTrue(shuffleCfgManager.getShuffleBlocklist().contains(r1));
    Assertions.assertTrue(shuffleCfgManager.getShuffleBlocklist().contains(r2));
  }

  @Test
  public void itSupportsRemovingBlacklistedRequestIds() {
    shuffleCfgManager.addToShuffleBlocklist(r1);
    Assertions.assertTrue(shuffleCfgManager.isOnShuffleBlocklist(r1));

    shuffleCfgManager.removeFromShuffleBlocklist(r1);
    Assertions.assertFalse(shuffleCfgManager.isOnShuffleBlocklist(r1));
  }

  @Test
  public void itPreloadsBlacklistFromConfiguration() {
    Assertions.assertTrue(shuffleCfgManager.isOnShuffleBlocklist("no-shuffle"));
  }

  @Test
  public void itDoesNotBlacklistUnrecognizedRequestIds() {
    Assertions.assertFalse(shuffleCfgManager.isOnShuffleBlocklist("unrecognized"));
  }
}
