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
import com.hubspot.singularity.data.UserManager;
import com.wordnik.swagger.annotations.ApiParam;

@Path(UserResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class UserResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/users";

  private final UserManager userManager;
  private final Optional<SingularityUser> user;

  @Inject
  public UserResource(UserManager userManager,
                      Optional<SingularityUser> user) {
    this.userManager = userManager;
    this.user = user;
  }

  private String getAuthUserId() {
    checkUnauthorized(user.isPresent(), "Please log in to perform this action.");
    return user.get().getId();
  }

  @GET
  @Path("/settings")
  public Optional<SingularityUserSettings> getUserSettings() {
    return userManager.getUserSettings(getAuthUserId());
  }

  @POST
  @Path("/settings")
  public void setUserSettings(@ApiParam("The new settings") SingularityUserSettings settings) {
    userManager.updateUserSettings(getAuthUserId(), settings);
  }

  @POST
  @Path("/settings/starred-requests")
  public void addStarredRequests(@ApiParam("The new starred requests") SingularityUserSettings settings) {
    userManager.addStarredRequestIds(getAuthUserId(), settings.getStarredRequestIds());
  }

  @DELETE
  @Path("/settings/starred-requests")
  public void deleteStarredRequests(@ApiParam("The new starred requests") SingularityUserSettings settings) {
    userManager.deleteStarredRequestIds(getAuthUserId(), settings.getStarredRequestIds());
  }
}
