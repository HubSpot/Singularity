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

@Path(ApiPaths.SLAVE_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class SlaveResource extends ProxyResource {

  @Inject
  public SlaveResource() {}

  @GET
  public Response getSlaves(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/slave/{slaveId}")
  public Response getSlaveHistory(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId) {
    return routeBySlaveId(request, slaveId);
  }

  @GET
  @Path("/slave/{slaveId}/details")
  public Response getSlave(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId) {
    return routeBySlaveId(request, slaveId);
  }

  @DELETE
  @Path("/slave/{slaveId}")
  public Response removeSlave(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId) {
    return routeBySlaveId(request, slaveId);
  }

  @POST
  @Path("/slave/{slaveId}/decommission")
  public Response decommissionSlave(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId, SingularityMachineChangeRequest changeRequest) {
    return routeBySlaveId(request, slaveId, changeRequest);
  }

  @POST
  @Path("/slave/{slaveId}/freeze")
  public Response freezeSlave(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId, SingularityMachineChangeRequest changeRequest) {
    return routeBySlaveId(request, slaveId, changeRequest);
  }

  @POST
  @Path("/slave/{slaveId}/activate")
  public Response activateSlave(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId, SingularityMachineChangeRequest changeRequest) {
    return routeBySlaveId(request, slaveId, changeRequest);
  }

  @DELETE
  @Path("/slave/{slaveId}/expiring")
  public Response deleteExpiringStateChange(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId) {
    return routeBySlaveId(request, slaveId);
  }

  @GET
  @Path("/expiring")
  public Response getExpiringStateChanges(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }
}
