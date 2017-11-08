package com.hubspot.mesos;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class MesosUtilsModule extends AbstractModule {
  @Override
  public void configure() {
    bind(MesosProtosUtils.class).in(Scopes.SINGLETON);
  }
}
