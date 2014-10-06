package com.hubspot.singularity.scheduler;

import org.junit.Test;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityCuratorTestBase;
import com.hubspot.singularity.data.MetadataManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.zkmigrations.ZkDataMigrationRunner;

public class ZkMigrationTest extends SingularityCuratorTestBase {

  @Inject
  private ZkDataMigrationRunner migrationRunner;
  @Inject
  private MetadataManager metadataManager;
  @Inject
  private TaskManager taskManager;

  @Test
  public void testMigrationRunner() {
    metadataManager.getZkDataVersion();
  }

}
