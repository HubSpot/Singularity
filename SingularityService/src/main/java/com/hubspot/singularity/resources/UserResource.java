package com.hubspot.singularity.resources;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityUserSettings;
import com.hubspot.singularity.data.UserManager;
import com.wordnik.swagger.annotations.ApiParam;

@Path(UserResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class UserResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/user";

  private final UserManager userManager;

  @Inject
  public UserResource(UserManager userManager) {
    this.userManager = userManager;
  }

  @GET
  @Path("/settings")
  public Optional<SingularityUserSettings> getUserSettings(
      @ApiParam("The user id to use") @QueryParam("userId") String userId) {
    return userManager.getUserSettings(userId);
  }

  @POST
  @Path("/settings")
  public void setUserSettings(
      @ApiParam("The user id to use") @QueryParam("userId") String userId,
      @ApiParam("The new settings") SingularityUserSettings settings) {
    userManager.updateUserSettings(userId, settings);
  }
}
