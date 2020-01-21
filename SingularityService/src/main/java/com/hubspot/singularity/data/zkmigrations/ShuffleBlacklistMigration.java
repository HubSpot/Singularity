package com.hubspot.singularity.data.zkmigrations;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.ShuffleConfigurationManager;
import org.apache.curator.framework.CuratorFramework;

@Singleton
public class ShuffleBlacklistMigration extends ZkDataMigration {

  private final CuratorFramework curator;
  private final SingularityConfiguration configuration;
  private final ShuffleConfigurationManager manager;


  @Inject
  public ShuffleBlacklistMigration(CuratorFramework curator, SingularityConfiguration configuration, ShuffleConfigurationManager manager) {
    super(16);
    this.curator = curator;
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
