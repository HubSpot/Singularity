package com.hubspot.singularity.data.zkmigrations;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.data.MetadataManager;

public class ZkDataMigrationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(ZkDataMigrationRunner.class);

  private final MetadataManager metadataManager;
  private final List<ZkDataMigration> migrations;

  @Inject
  public ZkDataMigrationRunner(MetadataManager metadataManager, List<ZkDataMigration> migrations) {
    this.metadataManager = metadataManager;
    this.migrations = migrations;
  }

  public void checkMigrations() {
    Collections.sort(migrations);

    final Optional<String> currentVersion = metadataManager.getZkDataVersion();
    final int intVersionNumber = Integer.parseInt(currentVersion.or("0"));

    LOG.info("Current ZK data version is {}, known migrations: {}", intVersionNumber, migrations);

    int lastAppliedMigration = intVersionNumber;

    for (ZkDataMigration migration : migrations) {
      if (migration.getMigrationNumber() > intVersionNumber) {
        final long migrationStart = System.currentTimeMillis();

        LOG.info("Applying {}", migration);

        migration.applyMigration();

        LOG.info("Applied {} in {}", migration, JavaUtils.duration(migrationStart));

        lastAppliedMigration = migration.getMigrationNumber();
      }
    }

    if (lastAppliedMigration > intVersionNumber) {
      LOG.info("Setting new version to {}", lastAppliedMigration);
      metadataManager.setZkDataVersion(Integer.toString(lastAppliedMigration));
    }
  }

}
