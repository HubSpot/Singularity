package com.hubspot.singularity.auth;

import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;

import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.authenticator.SingularityMultiMethodAuthenticator;

import io.dropwizard.auth.Auth;
import io.dropwizard.auth.Authenticator;

@javax.ws.rs.ext.Provider
public class SingularityAuthFeature implements Feature {
  @javax.inject.Inject
  SingularityAuthFeature() {}

  @Override
  public boolean configure(FeatureContext context) {
    context.register(new AbstractBinder() {
      @Override
      public void configure() {
        bind(SingularityMultiMethodAuthenticator.class).to(new TypeLiteral<Authenticator<ContainerRequestContext, SingularityUser>>() {}).in(Singleton.class);
        bind(SingularityAuthedUserFactory.class).to(SingularityAuthedUserFactory.class).in(Singleton.class);
        bind(SingularityAuthFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class);
        bind(SingularityAuthParamInjectionResolver.class).to(new TypeLiteral<InjectionResolver<Auth>>(){}).in(Singleton.class);
      }
    });
    return true;
  }
}
