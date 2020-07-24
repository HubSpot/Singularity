package com.hubspot.singularity.data.zkmigrations;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.ShuffleConfigurationManager;

@Singleton
public class ShuffleBlacklistMigration extends ZkDataMigration {
  private final SingularityConfiguration configuration;
  private final ShuffleConfigurationManager manager;

  @Inject
  public ShuffleBlacklistMigration(
    SingularityConfiguration configuration,
    ShuffleConfigurationManager manager
  ) {
    super(16);
    this.configuration = configuration;
    this.manager = manager;
  }

  @Override
  public void applyMigration() {
    for (String requestId : configuration.getDoNotShuffleRequests()) {
      manager.addToShuffleBlacklist(requestId);
    }
  }
}
