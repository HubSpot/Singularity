package com.hubspot.singularity.auth.authenticator;

import java.util.Optional;

import javax.ws.rs.container.ContainerRequestContext;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;

@Singleton
public class SingularityQueryParamAuthenticator implements SingularityAuthenticator {
  private final SingularityAuthDatastore authDatastore;

  @Inject
  public SingularityQueryParamAuthenticator(SingularityAuthDatastore authDatastore) {
    this.authDatastore = authDatastore;
  }

  @Override
  public Optional<SingularityUser> getUser(ContainerRequestContext context) {
    final Optional<String> maybeUserId = Optional.ofNullable(Strings.emptyToNull(context.getUriInfo().getQueryParameters().getFirst("user")));

    if (!maybeUserId.isPresent()) {
      throw WebExceptions.unauthorized("(QueryParam) No user specified");
    }
    return authDatastore.getUser(maybeUserId.get());
  }
}
