package com.hubspot.singularity.proxy;

import java.util.List;

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
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;

@Path(ApiPaths.RACK_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class RackResource extends ProxyResource {

  @Inject
  public RackResource() {}

  @GET
  @Path("/")
  public List<SingularityRack> getRacks(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.RACK_LIST_REF);
  }

  @GET
  @Path("/rack/{rackId}")
  public List<SingularityMachineStateHistoryUpdate> getRackHistory(@Context HttpServletRequest request, @PathParam("rackId") String rackId) {
    return routeByRackId(request, rackId, TypeRefs.MACHINE_UPDATE_LIST_REF);
  }

  @DELETE
  @Path("/rack/{rackId}")
  public Response removeRack(@Context HttpServletRequest request, @PathParam("rackId") String rackId) {
    // TODO - remove from internal list as well?
    return routeByRackId(request, rackId, TypeRefs.RESPONSE_REF);
  }

  @POST
  @Path("/rack/{rackId}/decommission")
  public Response decommissionRack(@Context HttpServletRequest request, @PathParam("rackId") String rackId, SingularityMachineChangeRequest changeRequest) {
    return routeByRackId(request, rackId, TypeRefs.RESPONSE_REF);
  }

  @POST
  @Path("/rack/{rackId}/freeze")
  public Response freezeRack(@Context HttpServletRequest request, @PathParam("rackId") String rackId, SingularityMachineChangeRequest changeRequest) {
    return routeByRackId(request, rackId, TypeRefs.RESPONSE_REF);
  }

  @POST
  @Path("/rack/{rackId}/activate")
  public Response activateRack(@Context HttpServletRequest request, @PathParam("rackId") String rackId, SingularityMachineChangeRequest changeRequest) {
    return routeByRackId(request, rackId, TypeRefs.RESPONSE_REF);
  }

  @DELETE
  @Path("/rack/{rackId}/expiring")
  public Response deleteExpiringStateChange(@Context HttpServletRequest request, @PathParam("rackId") String rackId) {
    return routeByRackId(request, rackId, TypeRefs.RESPONSE_REF);
  }

  @GET
  @Path("/expiring")
  public List<SingularityExpiringMachineState> getExpiringStateChanges(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.EXPIRING_MACHINE_STATE_LIST_REF);
  }

}
