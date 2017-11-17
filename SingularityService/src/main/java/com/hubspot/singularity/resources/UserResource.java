package com.hubspot.singularity.resources;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.SingularityUserSettings;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.UserManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiParam;

import io.dropwizard.auth.Auth;

@Path(ApiPaths.USER_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description = "Retrieve settings for a particular user", value = ApiPaths.USER_RESOURCE_PATH)
public class UserResource {
  private final UserManager userManager;

  @Inject
  public UserResource(UserManager userManager) {
    this.userManager = userManager;
  }

  @GET
  @Path("/settings")
  public SingularityUserSettings getUserSettings(@Auth SingularityUser user) {
    return userManager.getUserSettings(user.getId()).or(SingularityUserSettings.empty());
  }

  @POST
  @Path("/settings")
  public void setUserSettings(@Auth SingularityUser user, @ApiParam("Update all settings for a user") SingularityUserSettings settings) {
    userManager.updateUserSettings(user.getId(), settings);
  }

  @POST
  @Path("/settings/starred-requests")
  public void addStarredRequests(@Auth SingularityUser user, @ApiParam("Add starred requests for a user") SingularityUserSettings settings) {
    userManager.addStarredRequestIds(user.getId(), settings.getStarredRequestIds());
  }

  @DELETE
  @Path("/settings/starred-requests")
  public void deleteStarredRequests(@Auth SingularityUser user, @ApiParam("Remove starred requests for a user") SingularityUserSettings settings) {
    userManager.deleteStarredRequestIds(user.getId(), settings.getStarredRequestIds());
  }
}
