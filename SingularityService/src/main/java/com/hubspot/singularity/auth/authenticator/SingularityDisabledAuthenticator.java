package com.hubspot.singularity.auth.authenticator;

import java.util.Optional;

import javax.ws.rs.container.ContainerRequestContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityUser;

@Singleton
public class SingularityDisabledAuthenticator implements SingularityAuthenticator {
  @Inject
  public SingularityDisabledAuthenticator() {}

  @Override
  public Optional<SingularityUser> getUser(ContainerRequestContext context) {
    return Optional.empty();
  }
}
