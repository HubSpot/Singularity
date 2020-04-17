package com.hubspot.singularity.data.history;

import java.util.concurrent.locks.ReentrantLock;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.hubspot.singularity.data.history.SingularityMappers.SingularityIdMapper;
import com.hubspot.singularity.data.history.SingularityMappers.SingularityJsonStringMapper;

public class SingularityHistoryModule extends AbstractModule {
  public static final String PERSISTER_LOCK = "history.persister.lock";

  public SingularityHistoryModule() {}

  @Override
  public void configure() {
    Multibinder<RowMapper<?>> rowMappers = Multibinder.newSetBinder(binder(), new TypeLiteral<RowMapper<?>>() {});
    rowMappers.addBinding().to(SingularityMappers.SingularityRequestHistoryMapper.class).in(Scopes.SINGLETON);
    rowMappers.addBinding().to(SingularityMappers.SingularityTaskIdHistoryMapper.class).in(Scopes.SINGLETON);
    rowMappers.addBinding().to(SingularityMappers.SingularityDeployHistoryLiteMapper.class).in(Scopes.SINGLETON);
    rowMappers.addBinding().to(SingularityMappers.SingularityRequestIdCountMapper.class).in(Scopes.SINGLETON);
    rowMappers.addBinding().to(SingularityMappers.SingularityTaskUsageMapper.class).in(Scopes.SINGLETON);
    rowMappers.addBinding().to(SingularityMappers.SingularityRequestWithTimeMapper.class).in(Scopes.SINGLETON);

    Multibinder<ColumnMapper<?>> columnMappers = Multibinder.newSetBinder(binder(), new TypeLiteral<ColumnMapper<?>>() {});
    columnMappers.addBinding().to(SingularityMappers.SingularityBytesMapper.class).in(Scopes.SINGLETON);
    columnMappers.addBinding().to(SingularityIdMapper.class).in(Scopes.SINGLETON);
    columnMappers.addBinding().to(SingularityJsonStringMapper.class).in(Scopes.SINGLETON);
    columnMappers.addBinding().to(SingularityMappers.DateMapper.class).in(Scopes.SINGLETON);
    columnMappers.addBinding().to(SingularityMappers.SingularityTimestampMapper.class).in(Scopes.SINGLETON);

    bind(TaskHistoryHelper.class).in(Scopes.SINGLETON);
    bind(RequestHistoryHelper.class).in(Scopes.SINGLETON);
    bind(DeployHistoryHelper.class).in(Scopes.SINGLETON);
    bind(DeployTaskHistoryHelper.class).in(Scopes.SINGLETON);
    bind(SingularityRequestHistoryPersister.class).in(Scopes.SINGLETON);
    bind(SingularityDeployHistoryPersister.class).in(Scopes.SINGLETON);
    bind(SingularityTaskHistoryPersister.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  @Named(PERSISTER_LOCK)
  public ReentrantLock providePersisterLock() {
    return new ReentrantLock();
  }

}
