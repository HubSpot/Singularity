package com.hubspot.singularity.auth.authenticator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityUser;
import java.util.Optional;
import javax.ws.rs.container.ContainerRequestContext;

@Singleton
public class SingularityDisabledAuthenticator implements SingularityAuthenticator {

  @Inject
  public SingularityDisabledAuthenticator() {}

  @Override
  public Optional<SingularityUser> getUser(ContainerRequestContext context) {
    return Optional.empty();
  }
}
