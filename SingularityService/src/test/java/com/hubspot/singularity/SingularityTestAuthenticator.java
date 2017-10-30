package com.hubspot.singularity;

import java.util.Optional;

import javax.ws.rs.container.ContainerRequestContext;

import com.google.inject.Inject;
import com.hubspot.singularity.auth.authenticator.SingularityAuthenticator;

public class SingularityTestAuthenticator implements SingularityAuthenticator {
  private Optional<SingularityUser> user = Optional.empty();

  @Inject
  public SingularityTestAuthenticator() {

  }

  @Override
  public Optional<SingularityUser> getUser(ContainerRequestContext context) {
    return user;
  }

  public void setUser(Optional<SingularityUser> user) {
    this.user = user;
  }
}
