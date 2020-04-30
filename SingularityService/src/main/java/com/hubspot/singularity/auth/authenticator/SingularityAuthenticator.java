package com.hubspot.singularity.auth.authenticator;

import com.hubspot.singularity.SingularityUser;
import java.util.Optional;
import javax.ws.rs.container.ContainerRequestContext;

public interface SingularityAuthenticator {
  Optional<SingularityUser> getUser(ContainerRequestContext context);
}
