package com.hubspot.singularity.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import org.glassfish.jersey.server.internal.inject.AbstractContainerRequestValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.auth.authenticator.SingularityAuthenticator;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityAuthedUserFactory extends AbstractContainerRequestValueFactory<SingularityUser> {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityAuthedUserFactory.class);

  private final Set<SingularityAuthenticator> authenticators;
  private final SingularityConfiguration configuration;

  @javax.inject.Inject
  public SingularityAuthedUserFactory(Set<SingularityAuthenticator> authenticators, SingularityConfiguration configuration) {
    this.authenticators = authenticators;
    this.configuration = configuration;
  }

  @Override
  public SingularityUser provide() {
    List<String> unauthorizedExceptionMessages = new ArrayList<>();
    for (SingularityAuthenticator authenticator : authenticators) {
      try {
        Optional<SingularityUser> maybeUser = authenticator.getUser(getContainerRequest());
        if (maybeUser.isPresent()) {
          return maybeUser.get();
        }
      } catch (Throwable t) {
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
    return SingularityUser.DEFAULT_USER;
  }
}
