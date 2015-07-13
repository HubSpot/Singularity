package com.hubspot.singularity.auth.authenticator;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityUser;

@Singleton
public class SingularityDisabledAuthenticator implements SingularityAuthenticator {
  @Inject
  public SingularityDisabledAuthenticator() {}

  @Override
  public Optional<SingularityUser> get() {
    return Optional.absent();
  }
}
