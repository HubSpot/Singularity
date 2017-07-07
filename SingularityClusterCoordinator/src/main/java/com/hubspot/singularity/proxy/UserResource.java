package com.hubspot.singularity.proxy;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityUserSettings;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.USER_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class UserResource extends ProxyResource {

  @Inject
  public UserResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    super(configuration, httpClient, objectMapper);
  }

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
