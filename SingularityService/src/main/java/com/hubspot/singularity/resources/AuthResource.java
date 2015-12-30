package com.hubspot.singularity.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.SingularityUserHolder;
import com.hubspot.singularity.config.SingularityConfiguration;

@Path(AuthResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class AuthResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/auth";

  private final Optional<SingularityUser> user;
  private final SingularityConfiguration configuration;

  @Inject
  public AuthResource(Optional<SingularityUser> user,
      SingularityConfiguration configuration) {
    this.user = user;
    this.configuration = configuration;
  }

  @GET
  @Path("/user")
  public SingularityUserHolder getUser() {
    return new SingularityUserHolder(user, user.isPresent(), configuration.getAuthConfiguration().isEnabled());
  }
}
