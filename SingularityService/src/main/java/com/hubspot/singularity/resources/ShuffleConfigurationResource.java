package com.hubspot.singularity.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.ShuffleConfigurationManager;
import com.ning.http.client.AsyncHttpClient;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;

@Path(ApiPaths.STATE_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Schema(title = "Manages shuffle configuration of Singularity.")
@Tags({@Tag(name = "Shuffle")})
public class ShuffleConfigurationResource extends AbstractLeaderAwareResource {
  private static final Logger LOG = LoggerFactory.getLogger(ShuffleConfigurationResource.class);

  private final ShuffleConfigurationManager shuffleCfgManager;
  private final SingularityAuthorizationHelper authorizationHelper;
  private final RequestManager requestManager;

  @Inject
  public ShuffleConfigurationResource(
      AsyncHttpClient httpClient, LeaderLatch leaderLatch, ObjectMapper objectMapper,
      ShuffleConfigurationManager shuffleCfgManager,
      SingularityAuthorizationHelper authorizationHelper,
      RequestManager requestManager
  ) {
    super(httpClient, leaderLatch, objectMapper);

    this.shuffleCfgManager = shuffleCfgManager;
    this.authorizationHelper = authorizationHelper;
    this.requestManager = requestManager;
  }

  private void checkReadAuthorization(SingularityUser user, String requestId) {
    checkRequestAuthorization(user, requestId, SingularityAuthorizationScope.READ);
  }

  private void checkWriteAuthorization(SingularityUser user, String requestId) {
    checkRequestAuthorization(user, requestId, SingularityAuthorizationScope.WRITE);
  }

  private void checkRequestAuthorization(SingularityUser user, String requestId, SingularityAuthorizationScope scope) {
    Optional<SingularityRequestWithState> request = requestManager.getRequest(requestId);

    if (request.isPresent()) {
      authorizationHelper.checkForAuthorization(request.get().getRequest(), user, scope);
    } else {
      throw WebExceptions.notFound("No request matching id %s", requestId);
    }
  }

  @GET
  @Path("/shuffle/config/blacklist")
  @Operation(summary = "Retrieve the set of request IDs that should not be shuffled.")
  public List<String> getShuffleBlacklist(@Parameter(hidden = true) @Auth SingularityUser user) {
    authorizationHelper.checkReadAuthorization(user);
    return shuffleCfgManager.getShuffleBlacklist();
  }

  @GET
  @Path("/shuffle/config/blacklist/{requestId}}")
  @Operation(summary = "Check if a request ID is on the shuffle blacklist.")
  public boolean isOnShuffleBlacklist(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The Request ID to fetch crash loops for") @PathParam("requestId") String requestId
  ) {
    checkReadAuthorization(user, requestId);
    return shuffleCfgManager.isOnShuffleBlacklist(requestId);
  }

  @POST
  @Path("/shuffle/config/blacklist/{requestId}}")
  @Operation(summary = "Add a request ID to the shuffle blacklist. No effect if already present.")
  public void addToShuffleBlacklist(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The Request ID to fetch crash loops for") @PathParam("requestId") String requestId
  ) {
    checkWriteAuthorization(user, requestId);
    shuffleCfgManager.addToShuffleBlacklist(requestId);
  }

  @DELETE
  @Path("/shuffle/config/blacklist/{requestId}}")
  @Operation(summary = "Remove a request ID from the shuffle blacklist. No effect if not present.")
  public void removeFromShuffleBlacklist(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The Request ID to fetch crash loops for") @PathParam("requestId") String requestId
  ) {
    checkWriteAuthorization(user, requestId);
    shuffleCfgManager.removeFromShuffleBlacklist(requestId);
  }
}
