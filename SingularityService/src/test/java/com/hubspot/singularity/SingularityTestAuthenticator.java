package com.hubspot.singularity;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.auth.authenticator.SingularityAuthenticator;

public class SingularityTestAuthenticator implements SingularityAuthenticator {
  private Optional<SingularityUser> user = Optional.absent();

  @Inject
  public SingularityTestAuthenticator() {

  }

  @Override
  public Optional<SingularityUser> get() {
    return user;
  }

  public void setUser(Optional<SingularityUser> user) {
    this.user = user;
  }
}
