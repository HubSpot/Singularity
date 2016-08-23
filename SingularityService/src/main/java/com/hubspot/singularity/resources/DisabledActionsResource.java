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
import com.hubspot.singularity.data.DisabledActionManager;
import com.wordnik.swagger.annotations.Api;

@Path(DisabledActionsResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
@Api(description="Manages Singularity Deploys for existing requests", value=DisabledActionsResource.PATH)
public class DisabledActionsResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/disabled-actions";

  private final DisabledActionManager disabledActionManager;
  private final SingularityAuthorizationHelper authorizationHelper;
  private final Optional<SingularityUser> user;

  @Inject
  public DisabledActionsResource(DisabledActionManager disabledActionManager, SingularityAuthorizationHelper authorizationHelper, Optional<SingularityUser> user) {
    this.disabledActionManager = disabledActionManager;
    this.authorizationHelper = authorizationHelper;
    this.user = user;
  }

  @GET
  public List<SingularityDisabledAction> disabledActions() {
    authorizationHelper.checkAdminAuthorization(user);
    return disabledActionManager.getDisabledActions();
  }

  @POST
  @Path("/{action}")
  public void disableAction(@PathParam("action") SingularityDisabledActionType action, String message) {
    authorizationHelper.checkAdminAuthorization(user);
    disabledActionManager.disable(action, Optional.fromNullable(message), user);
  }

  @DELETE
  @Path("/{action}")
  public void enableAction(@PathParam("action") SingularityDisabledActionType action) {
    authorizationHelper.checkAdminAuthorization(user);
    disabledActionManager.enable(action);
  }
}
