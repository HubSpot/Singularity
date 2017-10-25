package com.hubspot.singularity.auth.authenticator;

import java.util.Set;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityUserProvider implements Provider<Optional<SingularityUser>>  {
  private Set<SingularityAuthenticator> authenticators;
  private boolean authEnabled;

  @Inject
  public SingularityUserProvider(Set<SingularityAuthenticator> authenticators, SingularityConfiguration configuration) {
    this.authenticators = authenticators;
    this.authEnabled = configuration.getAuthConfiguration().isEnabled();
  }

  @Override
  public Optional<SingularityUser> get() {
    Exception maybeException = null;
    for (SingularityAuthenticator authenticator : authenticators) {
      try {
        Optional<SingularityUser> maybeUser = authenticator.get();
        if (maybeUser.isPresent()) {
          return maybeUser;
        }
      } catch (Exception e) {
        maybeException = e;
      }
    }
    if (authEnabled) {
      throw WebExceptions.unauthorized(
          maybeException != null ? maybeException.getMessage() : String.format("Not authorized using authenticators: %s", authenticators));
    } else {
      return Optional.absent();
    }
  }
}
