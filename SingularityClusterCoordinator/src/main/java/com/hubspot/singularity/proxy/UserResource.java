package com.hubspot.singularity.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityUserSettings;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.USER_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class UserResource extends ProxyResource {
  // TODO - replicate to all data centers?

  @Inject
  public UserResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    super(configuration, httpClient, objectMapper);
  }

  @GET
  @Path("/settings")
  public SingularityUserSettings getUserSettings(@Context HttpServletRequest request) {
    return routeToDefaultDataCenter(request, TypeRefs.USER_SETTINGS_REF);
  }

  @POST
  @Path("/settings")
  public Response setUserSettings(@Context HttpServletRequest request, SingularityUserSettings settings) {
    return routeToDefaultDataCenter(request, TypeRefs.RESPONSE_REF);
  }

  @POST
  @Path("/settings/starred-requests")
  public Response addStarredRequests(@Context HttpServletRequest request, SingularityUserSettings settings) {
    return routeToDefaultDataCenter(request, settings, TypeRefs.RESPONSE_REF);
  }

  @DELETE
  @Path("/settings/starred-requests")
  public Response deleteStarredRequests(@Context HttpServletRequest request, SingularityUserSettings settings) {
    return routeToDefaultDataCenter(request, settings, TypeRefs.RESPONSE_REF);
  }
}
