package com.hubspot.singularity.resources;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityDisabledAction;
import com.hubspot.singularity.SingularityDisasterType;
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityTaskCredits;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityDisabledActionRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.DisasterManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path(ApiPaths.DISASTERS_RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Api(description="Manages Singularity Deploys for existing requests", value=ApiPaths.DISASTERS_RESOURCE_PATH)
public class DisastersResource {
  private final DisasterManager disasterManager;
  private final SingularityAuthorizationHelper authorizationHelper;
  private final Optional<SingularityUser> user;
  @Inject
  public DisastersResource(DisasterManager disasterManager,
                           SingularityAuthorizationHelper authorizationHelper,
                           Optional<SingularityUser> user) {
    this.disasterManager = disasterManager;
    this.authorizationHelper = authorizationHelper;
    this.user = user;
  }

  @GET
  @Path("/stats")
  @ApiOperation(value="Get current data related to disaster detection", response=SingularityDisastersData.class)
  public SingularityDisastersData disasterStats() {
    authorizationHelper.checkAdminAuthorization(user);
    return disasterManager.getDisastersData();
  }

  @GET
  @Path("/active")
  @ApiOperation(value="Get a list of current active disasters")
  public List<SingularityDisasterType> activeDisasters() {
    authorizationHelper.checkAdminAuthorization(user);
    return disasterManager.getActiveDisasters();
  }

  @POST
  @Path("/disable")
  @ApiOperation(value="Do not allow the automated poller to disable actions when a disaster is detected")
  public void disableAutomatedDisasterCreation() {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.disableAutomatedDisabledActions();
  }

  @POST
  @Path("/enable")
  @ApiOperation(value="Allow the automated poller to disable actions when a disaster is detected")
  public void enableAutomatedDisasterCreation() {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.enableAutomatedDisabledActions();
  }

  @DELETE
  @Path("/active/{type}")
  @ApiOperation(value="Remove an active disaster (make it inactive)")
  public void removeDisaster(@PathParam("type") SingularityDisasterType type) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.removeDisaster(type);
  }

  @POST
  @Path("/active/{type}")
  @ApiOperation(value="Create a new active disaster")
  public void newDisaster(@PathParam("type") SingularityDisasterType type) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.addDisaster(type);
    disasterManager.addDisabledActionsForDisasters(Collections.singletonList(type));
  }

  @GET
  @Path("/disabled-actions")
  @ApiOperation(value="Get a list of actions that are currently disable")
  public List<SingularityDisabledAction> disabledActions() {
    authorizationHelper.checkAdminAuthorization(user);
    return disasterManager.getDisabledActions();
  }

  @POST
  @Path("/disabled-actions/{action}")
  @ApiOperation(value="Disable a specific action")
  public void disableAction(@PathParam("action") SingularityAction action, SingularityDisabledActionRequest disabledActionRequest) {
    final Optional<SingularityDisabledActionRequest> maybeRequest = Optional.fromNullable(disabledActionRequest);
    authorizationHelper.checkAdminAuthorization(user);
    Optional<String> message = maybeRequest.isPresent() ? maybeRequest.get().getMessage() : Optional.<String>absent();
    disasterManager.disable(action, message, user, false, Optional.<Long>absent());
  }

  @DELETE
  @Path("/disabled-actions/{action}")
  @ApiOperation(value="Re-enable a specific action if it has been disabled")
  public void enableAction(@PathParam("action") SingularityAction action) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.enable(action);
  }

  @POST
  @Path("/task-credits")
  @ApiOperation(value="Add task credits, enables task credit system if not already enabled")
  public void addTaskCredits(@QueryParam("credits") Optional<Integer> credits) throws Exception {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.enableTaskCredits();
    disasterManager.enqueueCreditsChange(credits.or(0));
  }

  @DELETE
  @Path("/task-credits")
  @ApiOperation(value="Disable task credit system")
  public void disableTaskCredits(@Context HttpServletRequest request) throws Exception {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.disableTaskCredits();
  }

  @GET
  @Path("/task-credits")
  @ApiOperation(value="Get task credit data")
  public SingularityTaskCredits getTaskCreditData() throws Exception {
    authorizationHelper.checkAdminAuthorization(user);
    return new SingularityTaskCredits(disasterManager.isTaskCreditEnabled(), disasterManager.getTaskCredits());
  }
}
