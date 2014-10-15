package com.hubspot.singularity.client;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;

import org.junit.Test;

public class SingularityClientModuleTest {

  @Inject
  SingularityClusterManager manager;

  @Inject
  SingularityClient client;

  @Test
  public void testSingularityClientModule() {
    final Injector injector = Guice.createInjector(Stage.PRODUCTION, new AbstractModule() {
      @Override
      protected void configure()
      {
        binder().disableCircularProxies();
        binder().requireAtInjectOnConstructors();
        binder().requireExplicitBindings();
      }
    });

    injector.injectMembers(this);
  }
}
