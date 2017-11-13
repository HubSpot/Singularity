package com.hubspot.singularity.auth;

import org.glassfish.jersey.server.internal.inject.ParamInjectionResolver;

import com.google.inject.Singleton;

import io.dropwizard.auth.Auth;

@Singleton
public class SingularityAuthParamInjectionResolver extends ParamInjectionResolver<Auth> {
  public SingularityAuthParamInjectionResolver() {
    super(SingularityAuthFactoryProvider.class);
  }
}
