package com.hubspot.singularity;

import com.google.inject.Inject;
import com.hubspot.singularity.auth.authenticator.SingularityAuthenticator;
import java.util.Optional;
import javax.ws.rs.container.ContainerRequestContext;

public class SingularityTestAuthenticator implements SingularityAuthenticator {

  private Optional<SingularityUser> user = Optional.empty();

  @Inject
  public SingularityTestAuthenticator() {}

  @Override
  public Optional<SingularityUser> getUser(ContainerRequestContext context) {
    return user;
  }

  public void setUser(Optional<SingularityUser> user) {
    this.user = user;
  }
}
