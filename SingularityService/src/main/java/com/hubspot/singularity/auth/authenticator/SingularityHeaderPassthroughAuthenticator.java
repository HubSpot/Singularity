package com.hubspot.singularity.auth.authenticator;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;
import com.hubspot.singularity.config.SingularityConfiguration;
import java.util.Optional;
import javax.ws.rs.container.ContainerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityHeaderPassthroughAuthenticator
  implements SingularityAuthenticator {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityHeaderPassthroughAuthenticator.class
  );

  private final SingularityAuthDatastore authDatastore;
  private final String requestUserHeaderName;

  @Inject
  public SingularityHeaderPassthroughAuthenticator(
    SingularityAuthDatastore authDatastore,
    SingularityConfiguration configuration
  ) {
    this.authDatastore = authDatastore;
    this.requestUserHeaderName =
      configuration.getAuthConfiguration().getRequestUserHeaderName();
  }

  private Optional<String> getUserId(ContainerRequestContext context) {
    try {
      return Optional.ofNullable(
        Strings.emptyToNull(context.getHeaderString(requestUserHeaderName))
      );
    } catch (ProvisionException pe) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<SingularityUser> getUser(ContainerRequestContext context) {
    final Optional<String> maybeUsername = getUserId(context);

    if (!maybeUsername.isPresent()) {
      throw WebExceptions.unauthorized(
        "(HeaderPassthrough) Could not determine username from header"
      );
    }

    LOG.trace("(HeaderPassthrough) Found user {}", maybeUsername.get());

    return authDatastore.getUser(maybeUsername.get());
  }
}
