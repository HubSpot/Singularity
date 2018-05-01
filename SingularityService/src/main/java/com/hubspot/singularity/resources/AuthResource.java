package com.hubspot.singularity.resources;

import javax.ws.rs.DefaultValue;
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
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.UserManager;

import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Path(ApiPaths.AUTH_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Schema(title = "Verify authentication for a user")
@Tags({@Tag(name = "Auth")})
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
  @Operation(summary = "Get information about the currently authenticated user")
  public SingularityUserHolder getUser(@Parameter(hidden = true) @Auth SingularityUser user) {
    return new SingularityUserHolder(
      Optional.of(user),
      userManager.getUserSettings(user.getId()),
      true,
      configuration.getAuthConfiguration().isEnabled());
  }

  @GET
  @Path("/{requestId}/auth-check/{userId}")
  @Operation(
      summary = "Check if the specified user is authorized for a request",
      responses = {
          @ApiResponse(responseCode = "200", description = "The user is authorized for the request and scope provided")
      }
  )
  public Response checkReadOnlyAuth(
      @Parameter(required = true, description = "Request id to check") @PathParam("requestId") String requestId,
      @Parameter(required = true, description = "User id to check") @PathParam("userId") String userId,
      @Parameter(description = "Scope to check for") @QueryParam("scope") @DefaultValue("READ") Optional<SingularityAuthorizationScope> scope) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, authDatastore.getUser(userId).orElse(SingularityUser.DEFAULT_USER), scope.or(SingularityAuthorizationScope.READ));
    return Response.ok().build();
  }

  @GET
  @Path("/{requestId}/auth-check}")
  @Operation(
      summary = "Check if the specified user is authorized for a request",
      responses = {
          @ApiResponse(responseCode = "200", description = "The user is authorized for the request and scope provided")
      }
  )
  public Response checkReadOnlyAuth(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Request id to check") @PathParam("requestId") String requestId,
      @Parameter(description = "Scope to check for") @QueryParam("scope") @DefaultValue("READ") Optional<SingularityAuthorizationScope> scope) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, scope.or(SingularityAuthorizationScope.READ));
    return Response.ok().build();
  }
}
