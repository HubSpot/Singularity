package com.hubspot.singularity.resources;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.SingularityUserHolder;
import com.hubspot.singularity.api.SingularityUpdateGroupsRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.UserManager;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import io.dropwizard.auth.Auth;

@Path(ApiPaths.AUTH_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class AuthResource {
  private final UserManager userManager;
  private final SingularityConfiguration configuration;
  private final SingularityAuthorizationHelper authorizationHelper;
  private final SingularityAuthDatastore authDatastore;

  @Inject
  public AuthResource(UserManager userManager,
                      SingularityConfiguration configuration,
                      SingularityAuthorizationHelper authorizationHelper,
                      SingularityAuthDatastore authDatastore) {
    this.userManager = userManager;
    this.configuration = configuration;
    this.authorizationHelper = authorizationHelper;
    this.authDatastore = authDatastore;
  }

  @GET
  @Path("/user")
  public SingularityUserHolder getUser(@Auth SingularityUser user) {
    return new SingularityUserHolder(
      Optional.of(user),
      userManager.getUserSettings(user.getId()),
      true,
      configuration.getAuthConfiguration().isEnabled());
  }

  @GET
  @Path("/{requestId}/auth-check/{userId}")
  @ApiOperation("Check if the specified user is authorized for a request")
  public Response checkReadOnlyAuth(@PathParam("requestId") String requestId, @PathParam("userId") String userId,
                                    @QueryParam("scope") Optional<SingularityAuthorizationScope> scope) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, authDatastore.getUser(userId).orElse(SingularityUser.DEFAULT_USER), scope.or(SingularityAuthorizationScope.READ));
    return Response.ok().build();
  }

  @GET
  @Path("/{requestId}/auth-check}")
  @ApiOperation("Check if the specified user is authorized for a request")
  public Response checkReadOnlyAuth(@Auth SingularityUser user,
                                    @PathParam("requestId") String requestId, @PathParam("userId") String userId,
                                    @QueryParam("scope") Optional<SingularityAuthorizationScope> scope) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, scope.or(SingularityAuthorizationScope.READ));
    return Response.ok().build();
  }

  @POST
  @Path("/groups/auth-check")
  @ApiOperation(value="Check authorization for updating the group, readOnlyGroups, and readWriteGroups for a SingularityReques, without commiting the change")
  @ApiResponses({
      @ApiResponse(code=200, message="User is authorized to make these changes"),
      @ApiResponse(code=401, message="User is not authorized to make these updates"),
  })
  public Response checkAuthForGroups(@Auth SingularityUser user,
                                     SingularityUpdateGroupsRequest updateGroupsRequest) {
    authorizationHelper.checkForAuthorization(
        user,
        Sets.union(updateGroupsRequest.getGroup().asSet(), updateGroupsRequest.getReadWriteGroups()),
        updateGroupsRequest.getReadOnlyGroups(),
        SingularityAuthorizationScope.WRITE,
        Optional.absent());
    return Response.ok().build();
  }
}
