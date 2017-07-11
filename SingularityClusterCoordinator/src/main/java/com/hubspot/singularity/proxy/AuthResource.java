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

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.AUTH_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class AuthResource extends ProxyResource {

  @Inject
  public AuthResource() {}

  @GET
  @Path("/user")
  public Response getUser(@Context HttpServletRequest request) {
    return routeToDefaultDataCenter(request);
  }

  @GET
  @Path("/{requestId}/auth-check/{userId}")
  public Response checkReadOnlyAuth(@Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("userId") String userId, @QueryParam("scope") Optional<SingularityAuthorizationScope> scope) {
    return routeToDefaultDataCenter(request);
  }
}
