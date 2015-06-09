package com.hubspot.singularity.resources;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.SingularityLDAPManager;
import com.hubspot.singularity.data.SingularityValidator;

@Path(AuthResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class AuthResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/auth";

  private final Optional<SingularityUser> user;
  private final SingularityValidator validator;
  private final SingularityLDAPManager ldapManager;

  @Inject
  public AuthResource(Optional<SingularityUser> user, SingularityValidator validator, SingularityLDAPManager ldapManager) {
    this.user = user;
    this.validator = validator;
    this.ldapManager = ldapManager;
  }

  @GET
  @Path("/user")
  public Optional<SingularityUser> getUser() {
    return user;
  }

  @POST
  @Path("/cache/clear")
  public void clearAuthCache() {
    validator.checkForAdminAuthorization(user);

    ldapManager.clearGroupCache();
  }
}
