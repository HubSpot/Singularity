package com.hubspot.singularity.cache;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class SingularityCacheModule extends AbstractModule {

  @Override
  public void configure() {
    bind(AtomixProvider.class).in(Scopes.SINGLETON);
    bind(SingularityCache.class).in(Scopes.SINGLETON);
    bind(CacheUtils.class).in(Scopes.SINGLETON);
  }
}
