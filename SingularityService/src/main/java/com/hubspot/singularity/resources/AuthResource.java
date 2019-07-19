package com.hubspot.singularity.resources;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityTokenRequest;
import com.hubspot.singularity.SingularityTokenResponse;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.SingularityUserHolder;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.AuthTokenManager;
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
  private final AuthTokenManager authTokenManager;

  @Inject
  public AuthResource(UserManager userManager,
                      SingularityConfiguration configuration,
                      SingularityAuthorizationHelper authorizationHelper,
                      SingularityAuthDatastore authDatastore,
                      AuthTokenManager authTokenManager) {
    this.userManager = userManager;
    this.configuration = configuration;
    this.authorizationHelper = authorizationHelper;
    this.authDatastore = authDatastore;
    this.authTokenManager = authTokenManager;
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
    authorizationHelper.checkForAuthorizationByRequestId(requestId, authDatastore.getUser(userId).orElse(SingularityUser.DEFAULT_USER), scope.orElse(SingularityAuthorizationScope.READ));
    return Response.ok().build();
  }

  @GET
  @Path("/{requestId}/auth-check")
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
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, scope.orElse(SingularityAuthorizationScope.READ));
    return Response.ok().build();
  }

  @POST
  @Path("/token")
  @Operation(
      summary = "Generate a new auth token for the provided user data, or for current authed user if no user provided in post body. Only one token can be active for a user at a time",
      responses = {
          @ApiResponse(responseCode = "200", description = "the user data and generated token")
      }
  )
  public SingularityTokenResponse generateToken(@Parameter(hidden = true) @Auth SingularityUser user,
                                                SingularityTokenRequest tokenRequest) throws NoSuchAlgorithmException, InvalidKeySpecException {
    if (tokenRequest.getUser().isPresent()) {
      authorizationHelper.checkAdminAuthorization(user);
    } else {
      authorizationHelper.checkUserInRequiredGroups(user);
    }
    SingularityUser userData = tokenRequest.getUser().orElse(user);
    authTokenManager.clearTokensForUser(userData.getName());
    if (tokenRequest.getToken().isPresent()) {
      return authTokenManager.saveToken(tokenRequest.getToken().get(), userData);
    } else {
      return authTokenManager.generateToken(userData);
    }
  }

  @DELETE
  @Path("/token/{user}")
  @Operation(
      summary = "Clear tokens for a user",
      responses = {
          @ApiResponse(responseCode = "200", description = "tokens cleared successfully")
      }
  )
  public Response generateToken(@Parameter(hidden = true) @Auth SingularityUser user, @PathParam("user") String userName) throws NoSuchAlgorithmException, InvalidKeySpecException {
    authorizationHelper.checkAdminAuthorization(user);
    authTokenManager.clearTokensForUser(userName);
    return Response.ok().build();
  }
}
