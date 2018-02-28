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
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import io.dropwizard.auth.Auth;

@Path(ApiPaths.DISASTERS_RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Api(description="Manages Singularity Deploys for existing requests", value=ApiPaths.DISASTERS_RESOURCE_PATH)
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
  @ApiOperation(value="Get current data related to disaster detection", response=SingularityDisastersData.class)
  public SingularityDisastersData disasterStats(@Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    return disasterManager.getDisastersData();
  }

  @GET
  @Path("/active")
  @ApiOperation(value="Get a list of current active disasters")
  public List<SingularityDisasterType> activeDisasters(@Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    return disasterManager.getActiveDisasters();
  }

  @POST
  @Path("/disable")
  @ApiOperation(value="Do not allow the automated poller to disable actions when a disaster is detected")
  public void disableAutomatedDisasterCreation(@Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.disableAutomatedDisabledActions();
  }

  @POST
  @Path("/enable")
  @ApiOperation(value="Allow the automated poller to disable actions when a disaster is detected")
  public void enableAutomatedDisasterCreation(@Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.enableAutomatedDisabledActions();
  }

  @DELETE
  @Path("/active/{type}")
  @ApiOperation(value="Remove an active disaster (make it inactive)")
  public void removeDisaster(@Auth SingularityUser user,  @PathParam("type") SingularityDisasterType type) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.removeDisaster(type);
  }

  @POST
  @Path("/active/{type}")
  @ApiOperation(value="Create a new active disaster")
  public void newDisaster(@Auth SingularityUser user, @PathParam("type") SingularityDisasterType type) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.addDisaster(type);
    disasterManager.addDisabledActionsForDisasters(Collections.singletonList(type));
  }

  @GET
  @Path("/disabled-actions")
  @ApiOperation(value="Get a list of actions that are currently disable")
  public List<SingularityDisabledAction> disabledActions(@Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    return disasterManager.getDisabledActions();
  }

  @POST
  @Path("/disabled-actions/{action}")
  @ApiOperation(value="Disable a specific action")
  public void disableAction(@Auth SingularityUser user, @PathParam("action") SingularityAction action, SingularityDisabledActionRequest disabledActionRequest) {
    final Optional<SingularityDisabledActionRequest> maybeRequest = Optional.fromNullable(disabledActionRequest);
    authorizationHelper.checkAdminAuthorization(user);
    Optional<String> message = maybeRequest.isPresent() ? maybeRequest.get().getMessage() : Optional.<String>absent();
    disasterManager.disable(action, message, Optional.of(user), false, Optional.absent());
  }

  @DELETE
  @Path("/disabled-actions/{action}")
  @ApiOperation(value="Re-enable a specific action if it has been disabled")
  public void enableAction(@Auth SingularityUser user, @PathParam("action") SingularityAction action) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.enable(action);
  }
}
