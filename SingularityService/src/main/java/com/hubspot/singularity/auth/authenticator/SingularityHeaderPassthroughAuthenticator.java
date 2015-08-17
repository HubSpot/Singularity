package com.hubspot.singularity.auth.authenticator;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;
import com.hubspot.singularity.config.SingularityConfiguration;

@RequestScoped
public class SingularityHeaderPassthroughAuthenticator implements SingularityAuthenticator {
  private final SingularityAuthDatastore datastore;
  private final String requestUserHeaderName;
  private final HttpServletRequest request;

  @Inject
  public SingularityHeaderPassthroughAuthenticator(SingularityAuthDatastore datastore, SingularityConfiguration configuration, HttpServletRequest request) {
    this.datastore = datastore;
    this.requestUserHeaderName = configuration.getAuthConfiguration().getRequestUserHeaderName();
    this.request = request;
  }

  @Override
  public Optional<SingularityUser> get() {
    final Optional<String> maybeUsername = Optional.fromNullable(Strings.emptyToNull(request.getHeader(requestUserHeaderName)));

    if (!maybeUsername.isPresent()) {
      return Optional.absent();
    }

    return datastore.getUser(maybeUsername.get());
  }
}
