package com.hubspot.singularity.auth.authenticator;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;

@RequestScoped
public class SingularityQueryParamAuthenticator implements SingularityAuthenticator {
  private final HttpServletRequest request;
  private final SingularityAuthDatastore authDatastore;

  @Inject
  public SingularityQueryParamAuthenticator(HttpServletRequest request, SingularityAuthDatastore authDatastore) {
    this.request = request;
    this.authDatastore = authDatastore;
  }

  @Override
  public Optional<SingularityUser> get() {
    final Optional<String> maybeUser = Optional.fromNullable(Strings.emptyToNull(request.getParameter("user")));

    if (maybeUser.isPresent()) {
      return authDatastore.getUser(maybeUser.get());
    } else {
      return Optional.absent();
    }
  }
}
