package com.hubspot.singularity.resources;

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
import com.hubspot.singularity.SingularityDisabledAction;
import com.hubspot.singularity.SingularityDisabledActionType;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.data.DisasterManager;
import com.wordnik.swagger.annotations.Api;

@Path(DisastersResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
@Api(description="Manages Singularity Deploys for existing requests", value=DisastersResource.PATH)
public class DisastersResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/disasters";

  private final DisasterManager disasterManager;
  private final SingularityAuthorizationHelper authorizationHelper;
  private final Optional<SingularityUser> user;

  @Inject
  public DisastersResource(DisasterManager disasterManager, SingularityAuthorizationHelper authorizationHelper, Optional<SingularityUser> user) {
    this.disasterManager = disasterManager;
    this.authorizationHelper = authorizationHelper;
    this.user = user;
  }

  @GET
  @Path("/disabled-actions")
  public List<SingularityDisabledAction> disabledActions() {
    authorizationHelper.checkAdminAuthorization(user);
    return disasterManager.getDisabledActions();
  }

  @POST
  @Path("/disabled-actions/{action}")
  public void disableAction(@PathParam("action") SingularityDisabledActionType action, String message) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.disable(action, Optional.fromNullable(message), user, false);
  }

  @DELETE
  @Path("/disabled-actions/{action}")
  public void enableAction(@PathParam("action") SingularityDisabledActionType action) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.enable(action);
  }
}
