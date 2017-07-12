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
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.RACK_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class RackResource extends ProxyResource {

  @Inject
  public RackResource() {}

  @GET
  @Path("/")
  public Response getRacks(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/rack/{rackId}")
  public Response getRackHistory(@Context HttpServletRequest request, @PathParam("rackId") String rackId) {
    return routeByRackId(request, rackId);
  }

  @DELETE
  @Path("/rack/{rackId}")
  public Response removeRack(@Context HttpServletRequest request, @PathParam("rackId") String rackId) {
    // TODO - remove from internal list as well?
    return routeByRackId(request, rackId);
  }

  @POST
  @Path("/rack/{rackId}/decommission")
  public Response decommissionRack(@Context HttpServletRequest request, @PathParam("rackId") String rackId, SingularityMachineChangeRequest changeRequest) {
    return routeByRackId(request, rackId);
  }

  @POST
  @Path("/rack/{rackId}/freeze")
  public Response freezeRack(@Context HttpServletRequest request, @PathParam("rackId") String rackId, SingularityMachineChangeRequest changeRequest) {
    return routeByRackId(request, rackId, changeRequest);
  }

  @POST
  @Path("/rack/{rackId}/activate")
  public Response activateRack(@Context HttpServletRequest request, @PathParam("rackId") String rackId, SingularityMachineChangeRequest changeRequest) {
    return routeByRackId(request, rackId, changeRequest);
  }

  @DELETE
  @Path("/rack/{rackId}/expiring")
  public Response deleteExpiringStateChange(@Context HttpServletRequest request, @PathParam("rackId") String rackId) {
    return routeByRackId(request, rackId);
  }

  @GET
  @Path("/expiring")
  public Response getExpiringStateChanges(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

}
