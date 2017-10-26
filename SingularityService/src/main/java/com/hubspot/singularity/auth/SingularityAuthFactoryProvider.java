package com.hubspot.singularity.auth;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.internal.inject.AbstractValueFactoryProvider;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.Parameter.Source;

import com.hubspot.singularity.SingularityUser;

import io.dropwizard.auth.Auth;

public class SingularityAuthFactoryProvider extends AbstractValueFactoryProvider {
  private SingularityAuthedUserFactory authFactory;

  @javax.inject.Inject
  public SingularityAuthFactoryProvider(final MultivaluedParameterExtractorProvider extractorProvider,
                                        ServiceLocator locator,
                                        SingularityAuthedUserFactory authFactory) {
    super(extractorProvider, locator, Source.UNKNOWN);
    this.authFactory = authFactory;
  }

  @Override
  protected Factory<?> createValueFactory(Parameter parameter) {
    Class<?> paramType = parameter.getRawType();
    Auth annotation = parameter.getAnnotation(Auth.class);
    if (annotation != null && paramType.isAssignableFrom(SingularityUser.class)) {
      return authFactory;
    }
    return null;
  }
}
