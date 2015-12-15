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

@Singleton
public class SingularityQueryParamAuthenticator implements SingularityAuthenticator {
  private final Provider<HttpServletRequest> requestProvider;
  private final SingularityAuthDatastore authDatastore;

  @Inject
  public SingularityQueryParamAuthenticator(Provider<HttpServletRequest> requestProvider, SingularityAuthDatastore authDatastore) {
    this.requestProvider = requestProvider;
    this.authDatastore = authDatastore;
  }

  private Optional<String> getUserId() {
    try {
      return Optional.fromNullable(Strings.emptyToNull(requestProvider.get().getParameter("user")));
    } catch (ProvisionException pe) {
      return Optional.absent();
    }
  }

  @Override
  public Optional<SingularityUser> get() {
    final Optional<String> maybeUser = getUserId();

    if (maybeUser.isPresent()) {
      return authDatastore.getUser(maybeUser.get());
    } else {
      return Optional.absent();
    }
  }
}
