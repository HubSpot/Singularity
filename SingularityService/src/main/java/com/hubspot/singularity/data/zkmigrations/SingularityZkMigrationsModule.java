package com.hubspot.singularity.data.zkmigrations;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;

public class SingularityZkMigrationsModule implements Module {

  @Override
  public void configure(Binder binder) {
    binder.bind(ZkDataMigrationRunner.class).in(Scopes.SINGLETON);

    Multibinder<ZkDataMigration> dataMigrations = Multibinder.newSetBinder(binder, ZkDataMigration.class);
    dataMigrations.addBinding().to(LastTaskStatusMigration.class);
  }
}
