package com.hubspot.singularity.auth.authenticator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.config.SingularityConfiguration;

import io.dropwizard.auth.Authenticator;

public class SingularityMultiMethodAuthenticator implements Authenticator<ContainerRequestContext, SingularityUser> {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityMultiMethodAuthenticator.class);

  private final Set<SingularityAuthenticator> authenticators;
  private final SingularityConfiguration configuration;

  @javax.inject.Inject
  public SingularityMultiMethodAuthenticator(Set<SingularityAuthenticator> authenticators, SingularityConfiguration configuration) {
    this.authenticators = authenticators;
    this.configuration = configuration;
  }

  public Optional<SingularityUser> authenticate(ContainerRequestContext context) {
    List<String> unauthorizedExceptionMessages = new ArrayList<>();
    for (SingularityAuthenticator authenticator : authenticators) {
      try {
        Optional<SingularityUser> maybeUser = authenticator.getUser(context);
        if (maybeUser.isPresent()) {
          return maybeUser;
        }
      } catch (Throwable t) {
        LOG.trace("Unauthenticated: {}", t.getMessage());
        if (t instanceof WebApplicationException) {
          WebApplicationException wae = (WebApplicationException) t;
          unauthorizedExceptionMessages.add(String.format("%s (%s)", authenticator.getClass().getSimpleName(), wae.getResponse().getEntity().toString()));
        } else {
          unauthorizedExceptionMessages.add(String.format("%s (%s)", authenticator.getClass().getSimpleName(), t.getMessage()));
        }
      }
    }

    // No user found if we got here
    if (configuration.getAuthConfiguration().isEnabled()) {
      if (!unauthorizedExceptionMessages.isEmpty()) {
        throw WebExceptions.unauthorized(String.format("Unable to authenticate using methods: %s", unauthorizedExceptionMessages));
      } else {
        throw WebExceptions.unauthorized(String.format("Unable to authenticate user using methods: %s", authenticators.stream().map(SingularityAuthenticator::getClass).collect(Collectors.toList())));
      }
    }

    // Auth is disabled, return a dummy/default user
    return Optional.of(SingularityUser.DEFAULT_USER);
  }
}
