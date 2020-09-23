package com.hubspot.singularity.data.zkmigrations;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SingularityZkMigrationsModule implements Module {

  @Override
  public void configure(Binder binder) {
    binder.bind(ZkDataMigrationRunner.class).in(Scopes.SINGLETON);

    Multibinder<ZkDataMigration> dataMigrations = Multibinder.newSetBinder(
      binder,
      ZkDataMigration.class
    );
    dataMigrations.addBinding().to(LastTaskStatusMigration.class);
    dataMigrations.addBinding().to(SingularityPendingTaskIdMigration.class);
    dataMigrations.addBinding().to(AgentAndRackMigration.class);
    dataMigrations.addBinding().to(SingularityCmdLineArgsMigration.class);
    dataMigrations
      .addBinding()
      .to(TaskManagerRequiredParentsForTransactionsMigration.class);
    dataMigrations.addBinding().to(AgentAndRackMigration2.class);
    dataMigrations.addBinding().to(ScheduleMigration.class);
    dataMigrations.addBinding().to(SingularityRequestTypeMigration.class);
    dataMigrations.addBinding().to(PendingRequestDataMigration.class);
    dataMigrations.addBinding().to(SingularityPendingRequestWithRunIdMigration.class);
    dataMigrations.addBinding().to(CleanOldNodesMigration.class);
    dataMigrations.addBinding().to(NamespacePendingTasksMigration.class);
    dataMigrations.addBinding().to(NamespaceActiveTasksMigration.class);
    dataMigrations.addBinding().to(ClearUsagesMigration.class);
    dataMigrations.addBinding().to(ShuffleBlacklistMigration.class);
  }

  @Provides
  public List<ZkDataMigration> getMigrationsInOrder(Set<ZkDataMigration> migrations) {
    final List<ZkDataMigration> sortedMigrationList = Lists.newArrayList(migrations);
    Collections.sort(sortedMigrationList);

    return sortedMigrationList;
  }
}
