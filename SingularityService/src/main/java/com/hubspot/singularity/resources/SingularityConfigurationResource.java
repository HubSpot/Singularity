package com.hubspot.singularity.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.AgentPlacement;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.SingularityLimits;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.OverrideConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.ning.http.client.AsyncHttpClient;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
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

@Path(ApiPaths.CONFIGURATION_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Consumes(MediaType.APPLICATION_JSON)
@Schema(title = "Exposes some live Singularity configuration")
@Tags({ @Tag(name = "Singularity configuration") })
public class SingularityConfigurationResource extends AbstractLeaderAwareResource {

  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityConfigurationResource.class
  );
  private final SingularityConfiguration config;
  private final SingularityAuthorizer auth;
  private final OverrideConfiguration overrides;

  @Inject
  public SingularityConfigurationResource(
    SingularityConfiguration config,
    OverrideConfiguration overrides,
    SingularityAuthorizer authorizationHelper,
    LeaderLatch leaderLatch,
    AsyncHttpClient httpClient,
    @Singularity ObjectMapper objectMapper
  ) {
    super(httpClient, leaderLatch, objectMapper);
    this.config = config;
    this.overrides = overrides;
    this.auth = authorizationHelper;
  }

  @GET
  @Path("/singularity-limits")
  @Operation(summary = "Retrieve configuration data for Singularity")
  public SingularityLimits getSingularityLimits() {
    return new SingularityLimits(config.getMaxDecommissioningAgents());
  }

  @POST
  @Path("/rack-sensitive/enable")
  @Operation(summary = "Enable global rack sensitivity, respecting request settings")
  public Response enableGlobalRackSensitivity(
    @Context HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    return maybeProxyToLeader(
      requestContext,
      Response.class,
      null,
      () -> {
        auth.checkAdminAuthorization(user);
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
    auth.checkAdminAuthorization(user);
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
    auth.checkAdminAuthorization(user);
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
    auth.checkAdminAuthorization(user);
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
