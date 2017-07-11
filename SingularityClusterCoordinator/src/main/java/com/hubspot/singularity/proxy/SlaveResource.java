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

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;

@Path(ApiPaths.SLAVE_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class SlaveResource extends ProxyResource {

  @Inject
  public SlaveResource() {}

  @GET
  public List<SingularitySlave> getSlaves(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.SLAVE_LIST_REF);
  }

  @GET
  @Path("/slave/{slaveId}")
  public List<SingularityMachineStateHistoryUpdate> getSlaveHistory(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId) {
    return routeBySlaveId(request, slaveId, TypeRefs.MACHINE_UPDATE_LIST_REF);
  }

  @GET
  @Path("/slave/{slaveId}/details")
  public Optional<SingularitySlave> getSlave(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId) {
    return routeBySlaveId(request, slaveId, TypeRefs.OPTIONAL_SLAVE_REF);
  }

  @DELETE
  @Path("/slave/{slaveId}")
  public Response removeSlave(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId) {
    return routeBySlaveId(request, slaveId, TypeRefs.RESPONSE_REF);
  }

  @POST
  @Path("/slave/{slaveId}/decommission")
  public Response decommissionSlave(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId, SingularityMachineChangeRequest changeRequest) {
    return routeBySlaveId(request, slaveId, TypeRefs.RESPONSE_REF);
  }

  @POST
  @Path("/slave/{slaveId}/freeze")
  public Response freezeSlave(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId, SingularityMachineChangeRequest changeRequest) {
    return routeBySlaveId(request, slaveId, TypeRefs.RESPONSE_REF);
  }

  @POST
  @Path("/slave/{slaveId}/activate")
  public Response activateSlave(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId, SingularityMachineChangeRequest changeRequest) {
    return routeBySlaveId(request, slaveId, TypeRefs.RESPONSE_REF);
  }

  @DELETE
  @Path("/slave/{slaveId}/expiring")
  public Response deleteExpiringStateChange(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId) {
    return routeBySlaveId(request, slaveId, TypeRefs.RESPONSE_REF);
  }

  @GET
  @Path("/expiring")
  public List<SingularityExpiringMachineState> getExpiringStateChanges(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.EXPIRING_MACHINE_STATE_LIST_REF);
  }
}
