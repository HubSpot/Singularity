package com.hubspot.singularity.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequestGroup;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.REQUEST_GROUP_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class RequestGroupResource extends ProxyResource {

  @Inject
  public RequestGroupResource() {}

  @GET
  public Response getRequestGroupIds(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/group/{requestGroupId}")
  public Response getRequestGroup(@Context HttpServletRequest request, @PathParam("requestGroupId") String requestGroupId) {
    return routeByRequestGroupId(request, requestGroupId);
  }

  @DELETE
  @Path("/group/{requestGroupId}")
  public Response deleteRequestGroup(@Context HttpServletRequest request, @PathParam("requestGroupId") String requestGroupId) {
    return routeByRequestGroupId(request, requestGroupId);
  }

  @POST
  public Response saveRequestGroup(@Context HttpServletRequest request, SingularityRequestGroup requestGroup) {
    // TODO - route by more than first request id?
    // TODO - add to internal list of groups?
    // TODO - error if list is empty?
    return routeByRequestId(request, requestGroup.getRequestIds().get(0), requestGroup);
  }
}
