package com.hubspot.singularity.data;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.scheduler.SingularitySchedulerTestBase;
import org.junit.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShuffleConfigurationManagerTest extends SingularitySchedulerTestBase {
  protected String r1 = "1";
  protected String r2 = "2";

  @Inject
  protected ShuffleConfigurationManager shuffleCfgManager;

  @Inject
  protected MetricRegistry metricRegistry;

  @Inject
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
  public void itPreloadsBlacklistFromConfiguration() {
    // The customCfg constructor only allows primitives to be set,
    // so attempting to set the blacklist there causes opaque injector exceptions, hence this awkward workaround.
    List<String> original = configuration.getDoNotShuffleRequests();
    configuration.setDoNotShuffleRequests(Arrays.asList("do-not-shuffle"));

    ShuffleConfigurationManager tmp = new ShuffleConfigurationManager(shuffleCfgManager.curator, configuration, metricRegistry);
    Assertions.assertTrue(tmp.isOnShuffleBlacklist("do-not-shuffle"));

    configuration.setDoNotShuffleRequests(original);
  }

  @Test
  public void itDoesNotBlacklistUnrecognizedRequestIds() {
    Assertions.assertFalse(shuffleCfgManager.isOnShuffleBlacklist("unrecognized"));
  }
}
