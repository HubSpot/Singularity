package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkBadRequest;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityUserSettings;
import com.hubspot.singularity.data.UserManager;
import com.wordnik.swagger.annotations.ApiParam;

@Path(UserResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class UserResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/users";

  private final UserManager userManager;

  @Inject
  public UserResource(UserManager userManager) {
    this.userManager = userManager;
  }

  private static String encodeZkName(String name) {
    checkBadRequest(!Strings.isNullOrEmpty(name), "Name must be present and non-null");
    final String encodedName = BaseEncoding.base64Url().encode(name.getBytes(Charsets.UTF_8));
    checkBadRequest(!encodedName.equals("zookeeper"), "Name must not encode to reserved zookeeper word");
    return encodedName;
  }

  @GET
  @Path("/settings")
  public Optional<SingularityUserSettings> getUserSettings(
      @ApiParam("The user id to use") @QueryParam("userId") String userId) {
    return userManager.getUserSettings(encodeZkName(userId));
  }

  @POST
  @Path("/settings")
  public void setUserSettings(
      @ApiParam("The user id to use") @QueryParam("userId") String userId,
      @ApiParam("The new settings") SingularityUserSettings settings) {
    userManager.updateUserSettings(encodeZkName(userId), settings);
  }
}
