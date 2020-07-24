package com.hubspot.singularity.auth.dw;

import com.google.inject.Singleton;
import io.dropwizard.auth.Auth;
import org.glassfish.jersey.server.internal.inject.ParamInjectionResolver;

@Singleton
public class SingularityAuthParamInjectionResolver extends ParamInjectionResolver<Auth> {

  public SingularityAuthParamInjectionResolver() {
    super(SingularityAuthFactoryProvider.class);
  }
}
