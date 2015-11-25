package com.hubspot.singularity.auth.authenticator;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityHeaderPassthroughAuthenticator implements SingularityAuthenticator {
  private final SingularityAuthDatastore datastore;
  private final String requestUserHeaderName;
  private final Provider<HttpServletRequest> requestProvider;

  @Inject
  public SingularityHeaderPassthroughAuthenticator(SingularityAuthDatastore datastore, SingularityConfiguration configuration, Provider<HttpServletRequest> requestProvider) {
    this.datastore = datastore;
    this.requestUserHeaderName = configuration.getAuthConfiguration().getRequestUserHeaderName();
    this.requestProvider = requestProvider;
  }

  private Optional<String> getUserId() {
    try {
      return Optional.fromNullable(Strings.emptyToNull(requestProvider.get().getHeader(requestUserHeaderName)));
    } catch (ProvisionException pe) {
      return Optional.absent();
    }
  }

  @Override
  public Optional<SingularityUser> get() {
    final Optional<String> maybeUsername = getUserId();

    if (!maybeUsername.isPresent()) {
      return Optional.absent();
    }

    return datastore.getUser(maybeUsername.get());
  }
}
