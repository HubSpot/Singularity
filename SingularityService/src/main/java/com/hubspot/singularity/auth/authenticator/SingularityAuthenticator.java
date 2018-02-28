package com.hubspot.singularity.auth.authenticator;

import java.util.Optional;

import javax.ws.rs.container.ContainerRequestContext;

import com.hubspot.singularity.SingularityUser;

public interface SingularityAuthenticator {
  Optional<SingularityUser> getUser(ContainerRequestContext context);
}
