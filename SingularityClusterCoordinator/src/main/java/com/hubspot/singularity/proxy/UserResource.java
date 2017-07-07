package com.hubspot.singularity.proxy;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.hubspot.singularity.SingularityUserSettings;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.USER_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class UserResource {

  @GET
  @Path("/settings")
  public SingularityUserSettings getUserSettings() {

  }

  @POST
  @Path("/settings")
  public void setUserSettings(SingularityUserSettings settings) {

  }

  @POST
  @Path("/settings/starred-requests")
  public void addStarredRequests(SingularityUserSettings settings) {

  }

  @DELETE
  @Path("/settings/starred-requests")
  public void deleteStarredRequests(SingularityUserSettings settings) {

  }
}
