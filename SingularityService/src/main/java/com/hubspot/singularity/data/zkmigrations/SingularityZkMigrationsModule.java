package com.hubspot.singularity.data.zkmigrations;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;

public class SingularityZkMigrationsModule implements Module {

  @Override
  public void configure(Binder binder) {
    binder.bind(ZkDataMigrationRunner.class).in(Scopes.SINGLETON);

    Multibinder<ZkDataMigration> dataMigrations = Multibinder.newSetBinder(binder, ZkDataMigration.class);
    dataMigrations.addBinding().to(LastTaskStatusMigration.class);
    dataMigrations.addBinding().to(SingularityPendingTaskIdMigration.class);
    dataMigrations.addBinding().to(SlaveAndRackMigration.class);
    dataMigrations.addBinding().to(SingularityCmdLineArgsMigration.class);
    dataMigrations.addBinding().to(TaskManagerRequiredParentsForTransactionsMigration.class);
    dataMigrations.addBinding().to(SlaveAndRackMigration2.class);
    dataMigrations.addBinding().to(ScheduleMigration.class);
    dataMigrations.addBinding().to(SingularityRequestTypeMigration.class);
    dataMigrations.addBinding().to(PendingRequestDataMigration.class);
  }

  @Provides
  public List<ZkDataMigration> getMigrationsInOrder(Set<ZkDataMigration> migrations) {
    final List<ZkDataMigration> sortedMigrationList = Lists.newArrayList(migrations);
    Collections.sort(sortedMigrationList);

    return sortedMigrationList;
  }

}
