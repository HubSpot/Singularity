package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkUnauthorized;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.SingularityUserSettings;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.data.UserManager;
import com.wordnik.swagger.annotations.ApiParam;

@Path(UserResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class UserResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/users";

  private final UserManager userManager;
  private final Optional<SingularityUser> user;
  private final SingularityAuthorizationHelper authorizationHelper;

  @Inject
  public UserResource(UserManager userManager, Optional<SingularityUser> user, SingularityAuthorizationHelper authorizationHelper) {
    this.userManager = userManager;
    this.user = user;
    this.authorizationHelper = authorizationHelper;
  }

  @GET
  @Path("/settings")
  public SingularityUserSettings getUserSettings() {
    return userManager.getUserSettings(authorizationHelper.getAuthUserId(user)).or(SingularityUserSettings.empty());
  }

  @POST
  @Path("/settings")
  public void setUserSettings(@ApiParam("Update all settings for a user") SingularityUserSettings settings) {
    userManager.updateUserSettings(authorizationHelper.getAuthUserId(user), settings);
  }

  @POST
  @Path("/settings/starred-requests")
  public void addStarredRequests(@ApiParam("Add starred requests for a user") SingularityUserSettings settings) {
    userManager.addStarredRequestIds(authorizationHelper.getAuthUserId(user), settings.getStarredRequestIds());
  }

  @DELETE
  @Path("/settings/starred-requests")
  public void deleteStarredRequests(@ApiParam("Remove starred requests for a user") SingularityUserSettings settings) {
    userManager.deleteStarredRequestIds(authorizationHelper.getAuthUserId(user), settings.getStarredRequestIds());
  }
}
