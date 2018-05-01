package com.hubspot.singularity.resources;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityDisabledAction;
import com.hubspot.singularity.SingularityDisasterType;
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityDisabledActionRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.DisasterManager;

import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Path(ApiPaths.DISASTERS_RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Schema(title = "Manages active disasters and disabled actions")
@Tags({@Tag(name = "Disasters")})
public class DisastersResource {
  private final DisasterManager disasterManager;
  private final SingularityAuthorizationHelper authorizationHelper;

  @Inject
  public DisastersResource(DisasterManager disasterManager,
                           SingularityAuthorizationHelper authorizationHelper) {
    this.disasterManager = disasterManager;
    this.authorizationHelper = authorizationHelper;
  }

  @GET
  @Path("/stats")
  @Operation(summary = "Get current data related to disaster detection")
  public SingularityDisastersData disasterStats(@Parameter(hidden = true) @Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    return disasterManager.getDisastersData();
  }

  @GET
  @Path("/active")
  @Operation(summary = "Get a list of current active disasters")
  public List<SingularityDisasterType> activeDisasters(@Parameter(hidden = true) @Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    return disasterManager.getActiveDisasters();
  }

  @POST
  @Path("/disable")
  @Operation(summary = "Do not allow the automated poller to disable actions when a disaster is detected")
  public void disableAutomatedDisasterCreation(@Parameter(hidden = true) @Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.disableAutomatedDisabledActions();
  }

  @POST
  @Path("/enable")
  @Operation(summary = "Allow the automated poller to disable actions when a disaster is detected")
  public void enableAutomatedDisasterCreation(@Parameter(hidden = true) @Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.enableAutomatedDisabledActions();
  }

  @DELETE
  @Path("/active/{type}")
  @Operation(summary = "Remove an active disaster (make it inactive)")
  public void removeDisaster(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The type of disaster to operate on") @PathParam("type") SingularityDisasterType type) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.removeDisaster(type);
  }

  @POST
  @Path("/active/{type}")
  @Operation(summary = "Create a new active disaster")
  public void newDisaster(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The type of disaster to operate on") @PathParam("type") SingularityDisasterType type) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.addDisaster(type);
    disasterManager.addDisabledActionsForDisasters(Collections.singletonList(type));
  }

  @GET
  @Path("/disabled-actions")
  @Operation(summary = "Get a list of actions that are currently disable")
  public List<SingularityDisabledAction> disabledActions(@Parameter(hidden = true) @Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    return disasterManager.getDisabledActions();
  }

  @POST
  @Path("/disabled-actions/{action}")
  @Operation(summary = "Disable a specific action")
  public void disableAction(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The action to disable") @PathParam("action") SingularityAction action,
      @RequestBody(description = "Notes related to a particular disabled action") SingularityDisabledActionRequest disabledActionRequest) {
    final Optional<SingularityDisabledActionRequest> maybeRequest = Optional.fromNullable(disabledActionRequest);
    authorizationHelper.checkAdminAuthorization(user);
    Optional<String> message = maybeRequest.isPresent() ? maybeRequest.get().getMessage() : Optional.<String>absent();
    disasterManager.disable(action, message, Optional.of(user), false, Optional.absent());
  }

  @DELETE
  @Path("/disabled-actions/{action}")
  @Operation(summary = "Re-enable a specific action if it has been disabled")
  public void enableAction(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The action to enable") @PathParam("action") SingularityAction action) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.enable(action);
  }
}
