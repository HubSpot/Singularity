package com.hubspot.singularity.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityUser;

@Path(AuthResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class AuthResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/auth";

  private final Optional<SingularityUser> user;

  @Inject
  public AuthResource(Optional<SingularityUser> user) {
    this.user = user;
  }

  @GET
  @Path("/user")
  public Optional<SingularityUser> getUser() {
    return user;
  }
}
