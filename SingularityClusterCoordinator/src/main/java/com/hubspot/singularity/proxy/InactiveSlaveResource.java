package com.hubspot.singularity.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.INACTIVE_SLAVES_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class InactiveSlaveResource extends ProxyResource {

  @Inject
  public InactiveSlaveResource() {}

  @GET
  public Response getInactiveSlaves(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @POST
  public Response deactivateSlave(@Context HttpServletRequest request, @QueryParam("host") String host) {
    return routeByHostname(request, host);
  }

  @DELETE
  public Response reactivateSlave(@Context HttpServletRequest request, @QueryParam("host") String host) {
    return routeByHostname(request, host);
  }
}
