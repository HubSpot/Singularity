package com.hubspot.singularity.auth;

import javax.ws.rs.ext.Provider;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.Parameter;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
import io.dropwizard.auth.Auth;

@Provider
public class SingularityAnonymousUserProvider implements InjectableProvider<Auth, Parameter> {
  private static final AbstractHttpContextInjectable<SingularityUser> INJECTABLE = new AbstractHttpContextInjectable<SingularityUser>() {
    @Override
    public SingularityUser getValue(HttpContext c) {
      return SingularityUser.ANONYMOUS;
    }
  };

  @Override
  public ComponentScope getScope() {
    return ComponentScope.PerRequest;
  }

  @Override
  public Injectable<?> getInjectable(ComponentContext ic, Auth auth, Parameter parameter) {
    return INJECTABLE;
  }
}
