package com.hubspot.singularity.auth.authenticator;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityUser;

@Singleton
public class SingularityUserProvider implements Provider<Optional<SingularityUser>>  {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityUserProvider.class);

  private Set<SingularityAuthenticator> authenticators;

  @Inject
  public SingularityUserProvider(Set<SingularityAuthenticator> authenticators) {
    this.authenticators = authenticators;
  }

  @Override
  public Optional<SingularityUser> get() {
    Exception maybeException = null;
    for (SingularityAuthenticator authenticator : authenticators) {
      try {
        LOG.trace("Attempting to authenticate using {}", authenticator.getClass().getSimpleName());
        Optional<SingularityUser> maybeUser = authenticator.get();
        if (maybeUser.isPresent()) {
          return maybeUser;
        }
      } catch (Exception e) {
        maybeException = e;
      }
    }
    LOG.trace("Not authenticated {}", maybeException != null ? maybeException.getMessage() : "");
    return Optional.absent();
  }
}
