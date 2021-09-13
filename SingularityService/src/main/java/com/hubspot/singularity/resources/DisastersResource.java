package com.hubspot.singularity.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.*;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.api.SingularityDisabledActionRequest;
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.OverrideConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.ning.http.client.AsyncHttpClient;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(ApiPaths.DISASTERS_RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Schema(title = "Manages active disasters and disabled actions")
@Tags({ @Tag(name = "Disasters") })
public class DisastersResource extends AbstractLeaderAwareResource {
  private static final Logger LOG = LoggerFactory.getLogger(DisastersResource.class);

  private final DisasterManager disasterManager;
  private final SingularityAuthorizer authorizationHelper;
  private final SingularityAbort abort;
  private final OverrideConfiguration overrides;

  @Inject
  public DisastersResource(
    DisasterManager disasterManager,
    SingularityAuthorizer authorizationHelper,
    LeaderLatch leaderLatch,
    AsyncHttpClient httpClient,
    @Singularity ObjectMapper objectMapper,
    SingularityAbort abort,
    OverrideConfiguration overrides
  ) {
    super(httpClient, leaderLatch, objectMapper);
    this.disasterManager = disasterManager;
    this.authorizationHelper = authorizationHelper;
    this.abort = abort;
    this.overrides = overrides;
  }

  @GET
  @Path("/stats")
  @Operation(summary = "Get current data related to disaster detection")
  public SingularityDisastersData disasterStats(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return disasterManager.getDisastersData();
  }

  @GET
  @Path("/active")
  @Operation(summary = "Get a list of current active disasters")
  public List<SingularityDisasterType> activeDisasters(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return disasterManager.getActiveDisasters();
  }

  @POST
  @Path("/disable")
  @Operation(
    summary = "Do not allow the automated poller to disable actions when a disaster is detected"
  )
  public void disableAutomatedDisasterCreation(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.disableAutomatedDisabledActions();
  }

  @POST
  @Path("/enable")
  @Operation(
    summary = "Allow the automated poller to disable actions when a disaster is detected"
  )
  public void enableAutomatedDisasterCreation(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.enableAutomatedDisabledActions();
  }

  @DELETE
  @Path("/active/{type}")
  @Operation(summary = "Remove an active disaster (make it inactive)")
  public void removeDisaster(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      required = true,
      description = "The type of disaster to operate on"
    ) @PathParam("type") SingularityDisasterType type
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.removeDisaster(type);
  }

  @POST
  @Path("/active/{type}")
  @Operation(summary = "Create a new active disaster")
  public void newDisaster(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      required = true,
      description = "The type of disaster to operate on"
    ) @PathParam("type") SingularityDisasterType type
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.addDisaster(type);
    disasterManager.addDisabledActionsForDisasters(Collections.singletonList(type));
  }

  @GET
  @Path("/disabled-actions")
  @Operation(summary = "Get a list of actions that are currently disable")
  public List<SingularityDisabledAction> disabledActions(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return disasterManager.getDisabledActions();
  }

  @POST
  @Path("/disabled-actions/{action}")
  @Operation(summary = "Disable a specific action")
  public void disableAction(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "The action to disable") @PathParam(
      "action"
    ) SingularityAction action,
    @RequestBody(
      description = "Notes related to a particular disabled action"
    ) SingularityDisabledActionRequest disabledActionRequest
  ) {
    final Optional<SingularityDisabledActionRequest> maybeRequest = Optional.ofNullable(
      disabledActionRequest
    );
    authorizationHelper.checkAdminAuthorization(user);
    Optional<String> message = maybeRequest.isPresent()
      ? maybeRequest.get().getMessage()
      : Optional.<String>empty();
    disasterManager.disable(action, message, Optional.of(user), false, Optional.empty());
  }

  @DELETE
  @Path("/disabled-actions/{action}")
  @Operation(summary = "Re-enable a specific action if it has been disabled")
  public void enableAction(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "The action to enable") @PathParam(
      "action"
    ) SingularityAction action
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.enable(action);
  }

  @POST
  @Path("/failover")
  @Operation(
    summary = "Force the leading Singularity instance to restart and give up leadership"
  )
  public Response forceFailover(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Context HttpServletRequest requestContext
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return maybeProxyToLeader(
      requestContext,
      Response.class,
      null,
      () -> this.runFailover(user)
    );
  }

  private Response runFailover(SingularityUser user) {
    CompletableFuture.runAsync(
      () -> {
        LOG.warn("Failover triggered by {}", user.getId());
        abort.abort(
          AbortReason.MANUAL,
          Optional.of(
            new RuntimeException(String.format("Forced failover by %s", user.getId()))
          )
        );
      },
      Executors.newSingleThreadExecutor()
    );
    return Response.ok().build();
  }

  @POST
  @Path("/rack-sensitive/enable")
  @Operation(summary = "Enable global rack sensitivity, respecting request settings")
  public Response enableGlobalRackSensitivity(
    @Context HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return maybeProxyToLeader(
      requestContext,
      Response.class,
      null,
      () -> {
        LOG.info("Config override - allowRackSensitivity=true");
        overrides.setAllowRackSensitivity(true);
        return Response.ok().build();
      }
    );
  }

  @POST
  @Path("/rack-sensitive/disable")
  @Operation(summary = "Disable global rack sensitivity, overriding request settings")
  public Response disableGlobalRackSensitivity(
    @Context HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return maybeProxyToLeader(
      requestContext,
      Response.class,
      null,
      () -> {
        LOG.info("Config override - allowRackSensitivity=false");
        overrides.setAllowRackSensitivity(false);
        return Response.ok().build();
      }
    );
  }

  @POST
  @Path("/placement-strategy/override/set/{strategy}")
  @Operation(
    summary = "Set global placement strategy override, causing scheduling to ignore the default and request settings."
  )
  public Response setPlacementStrategyOverride(
    @Context HttpServletRequest requestContext,
    @Parameter(required = false, description = "Placement strategy name") @PathParam(
      "strategy"
    ) AgentPlacement strategy,
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return maybeProxyToLeader(
      requestContext,
      Response.class,
      null,
      () -> {
        LOG.info("Config override - agentPlacementOverride={}", strategy);
        overrides.setAgentPlacementOverride(Optional.ofNullable(strategy));
        return Response.ok().build();
      }
    );
  }

  @POST
  @Path("/placement-strategy/override/clear")
  @Operation(
    summary = "Clear global placement strategy override, causing scheduling to respect the default and request settings."
  )
  public Response disableSeparatePlacement(
    @Context HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return maybeProxyToLeader(
      requestContext,
      Response.class,
      null,
      () -> {
        LOG.info("Config override - agentPlacementOverride=");
        overrides.setAgentPlacementOverride(Optional.empty());
        return Response.ok().build();
      }
    );
  }
}
