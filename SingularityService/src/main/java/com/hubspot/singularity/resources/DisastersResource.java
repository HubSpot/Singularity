package com.hubspot.singularity.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.FireAlarm;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityDisabledAction;
import com.hubspot.singularity.SingularityDisasterType;
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityDisabledActionRequest;
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.config.ApiPaths;
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

  @Inject
  public DisastersResource(
    DisasterManager disasterManager,
    SingularityAuthorizer authorizationHelper,
    LeaderLatch leaderLatch,
    AsyncHttpClient httpClient,
    @Singularity ObjectMapper objectMapper,
    SingularityAbort abort
  ) {
    super(httpClient, leaderLatch, objectMapper);
    this.disasterManager = disasterManager;
    this.authorizationHelper = authorizationHelper;
    this.abort = abort;
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

  @GET
  @Path("/firealarm")
  @Operation(summary = "Set a firealarm warning in singularity")
  public Optional<FireAlarm> getFireAlarm(
    @Parameter(hidden = true) @Auth SingularityUser user,
    FireAlarm fireAlarm
  ) {
    return disasterManager.getFireAlarm();
  }

  @POST
  @Path("/firealarm")
  @Operation(summary = "Set a firealarm warning in singularity")
  public void enableFireAlarm(
    @Parameter(hidden = true) @Auth SingularityUser user,
    FireAlarm fireAlarm
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.setFireAlarm(fireAlarm);
  }

  @DELETE
  @Path("/firealarm")
  @Operation(summary = "Deleting ongoing fire alarm")
  public void disableFireAlarm(@Parameter(hidden = true) @Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.deleteFireAlarm();
  }
}
