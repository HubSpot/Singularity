package com.hubspot.singularity.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.SingularityUserHolder;
import com.hubspot.singularity.SingularityUserSettings;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.UserManager;
import com.wordnik.swagger.annotations.ApiOperation;

@Path(ApiPaths.AUTH_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class AuthResource {
  private final Optional<SingularityUser> user;
  private final UserManager userManager;
  private final SingularityConfiguration configuration;
  private final SingularityAuthorizationHelper authorizationHelper;
  private final SingularityAuthDatastore authDatastore;

  @Inject
  public AuthResource(Optional<SingularityUser> user,
                      UserManager userManager,
                      SingularityConfiguration configuration,
                      SingularityAuthorizationHelper authorizationHelper,
                      SingularityAuthDatastore authDatastore) {
    this.user = user;
    this.userManager = userManager;
    this.configuration = configuration;
    this.authorizationHelper = authorizationHelper;
    this.authDatastore = authDatastore;
  }

  @GET
  @Path("/user")
  public SingularityUserHolder getUser() {
    return new SingularityUserHolder(
      user,
      user.isPresent() ? userManager.getUserSettings(user.get().getId()) : Optional.<SingularityUserSettings>absent(),
      user.isPresent(),
      configuration.getAuthConfiguration().isEnabled());
  }

  @GET
  @Path("/{requestId}/auth-check/{userId}")
  @ApiOperation("Check if the specified user is authorized for a request")
  public Response checkReadOnlyAuth(@PathParam("requestId") String requestId, @PathParam("userId") String userId, @QueryParam("scope") Optional<SingularityAuthorizationScope> scope) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, authDatastore.getUser(userId), scope.or(SingularityAuthorizationScope.READ));
    return Response.ok().build();
  }
}
