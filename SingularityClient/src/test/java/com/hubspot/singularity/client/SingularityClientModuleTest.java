package com.hubspot.singularity.client;

import java.util.Collections;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;

public class SingularityClientModuleTest {

  @Inject
  SingularityClient client;

  @Test
  public void testModuleWithHosts() {
    final Injector injector = Guice.createInjector(Stage.PRODUCTION,
        new GuiceDisableModule(),
        new SingularityClientModule(Collections.singletonList("http://example.com")));

    injector.injectMembers(this);
  }

  private static class GuiceDisableModule extends AbstractModule {
    @Override
    protected void configure()
    {
      binder().disableCircularProxies();
      binder().requireAtInjectOnConstructors();
      binder().requireExactBindingAnnotations();
      binder().requireExplicitBindings();
    }
  }
}
