package com.hubspot.singularity.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityUserHolder;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.AUTH_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class AuthResource extends ProxyResource {

  @Inject
  public AuthResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper, DataCenterLocator dataCenterLocator) {
    super(configuration, httpClient, objectMapper, dataCenterLocator);
  }

  @GET
  @Path("/user")
  public SingularityUserHolder getUser(@Context HttpServletRequest request) {
    return routeToDefaultDataCenter(request, TypeRefs.USER_HOLDER_TYPE_REF);
  }

  @GET
  @Path("/{requestId}/auth-check/{userId}")
  public Response checkReadOnlyAuth(@Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("userId") String userId, @QueryParam("scope") Optional<SingularityAuthorizationScope> scope) {
    return routeToDefaultDataCenter(request, TypeRefs.RESPONSE_REF);
  }
}
