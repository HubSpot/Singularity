package com.hubspot.singularity.auth;

import java.io.IOException;
import java.util.Optional;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.ext.Provider;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.authenticator.SingularityMultiLevelAuthenticator;
import com.hubspot.singularity.config.SingularityConfiguration;

import io.dropwizard.auth.AuthFilter;

@Priority(Priorities.AUTHENTICATION)
@Provider
public class SingularityAuthFilter extends AuthFilter<ContainerRequestContext, SingularityUser> {
  private final SingularityMultiLevelAuthenticator authenticator;

  @Inject
  public SingularityAuthFilter(SingularityMultiLevelAuthenticator authenticator, SingularityConfiguration configuration) {
    this.authenticator = authenticator;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    Optional<SingularityUser> maybeUser = authenticator.authenticate(requestContext);
  }
}
