package com.hubspot.singularity.auth;

import javax.inject.Singleton;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import com.hubspot.singularity.auth.authenticator.SingularityMultiLevelAuthenticator;

import io.dropwizard.auth.Auth;

@javax.ws.rs.ext.Provider
public class SingularityAuthFeature implements Feature {
  @javax.inject.Inject
  SingularityAuthFeature() {}

  @Override
  public boolean configure(FeatureContext context) {
    context.register(new AbstractBinder() {
      @Override
      public void configure() {
        bind(SingularityMultiLevelAuthenticator.class).to(SingularityMultiLevelAuthenticator.class).in(Singleton.class);
        bind(SingularityAuthedUserFactory.class).to(SingularityAuthedUserFactory.class).in(Singleton.class);
        bind(SingularityAuthFactoryProvider.class).to(SingularityAuthFactoryProvider.class).in(Singleton.class);
        bind(SingularityAuthParamInjectionResolver.class).to(new TypeLiteral<InjectionResolver<Auth>>(){}).in(Singleton.class);
      }
    });
    return true;
  }
}
