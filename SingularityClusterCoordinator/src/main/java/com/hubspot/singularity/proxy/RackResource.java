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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.RACK_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class RackResource extends ProxyResource {

  @Inject
  public RackResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    super(configuration, httpClient, objectMapper);
  }

  @GET
  @Path("/")
  public List<SingularityRack> getRacks(@Context HttpServletRequest request) {
    throw new NotImplemenedException();
  }

  @GET
  @Path("/rack/{rackId}")
  public List<SingularityMachineStateHistoryUpdate> getRackHistory(@Context HttpServletRequest request, @PathParam("rackId") String rackId) {
    throw new NotImplemenedException();
  }

  @DELETE
  @Path("/rack/{rackId}")
  public void removeRack(@Context HttpServletRequest request, @PathParam("rackId") String rackId) {
    throw new NotImplemenedException();
  }

  @POST
  @Path("/rack/{rackId}/decommission")
  public void decommissionRack(@Context HttpServletRequest request, @PathParam("rackId") String rackId, SingularityMachineChangeRequest changeRequest) {
    throw new NotImplemenedException();
  }

  @POST
  @Path("/rack/{rackId}/freeze")
  public void freezeRack(@Context HttpServletRequest request, @PathParam("rackId") String rackId, SingularityMachineChangeRequest changeRequest) {
    throw new NotImplemenedException();
  }

  @POST
  @Path("/rack/{rackId}/activate")
  public void activateRack(@Context HttpServletRequest request, @PathParam("rackId") String rackId, SingularityMachineChangeRequest changeRequest) {
    throw new NotImplemenedException();
  }

  @DELETE
  @Path("/rack/{rackId}/expiring")
  public void deleteExpiringStateChange(@Context HttpServletRequest request, @PathParam("rackId") String rackId) {
    throw new NotImplemenedException();
  }

  @GET
  @Path("/expiring")
  public List<SingularityExpiringMachineState> getExpiringStateChanges(@Context HttpServletRequest request) {
    throw new NotImplemenedException();
  }

}
