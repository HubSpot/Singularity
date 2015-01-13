package com.hubspot.singularity.scheduler;


import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityCuratorTestBase;
import com.hubspot.singularity.data.MetadataManager;
import com.hubspot.singularity.data.zkmigrations.ZkDataMigrationRunner;

public class ZkMigrationTest extends SingularityCuratorTestBase {

  @Inject
  private ZkDataMigrationRunner migrationRunner;
  @Inject
  private MetadataManager metadataManager;

  @Test
  public void testMigrationRunner() {
    Assert.assertTrue(migrationRunner.checkMigrations() == 2);

    Assert.assertTrue(metadataManager.getZkDataVersion().isPresent() && metadataManager.getZkDataVersion().get().equals("2"));

    Assert.assertTrue(migrationRunner.checkMigrations() == 0);
  }

  @Test
  public void testPendingTaskIdMigration() {

  }


}
