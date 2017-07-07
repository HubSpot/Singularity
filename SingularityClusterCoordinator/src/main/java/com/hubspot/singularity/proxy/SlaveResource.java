package com.hubspot.singularity.proxy;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;

@Path(ApiPaths.SLAVE_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class SlaveResource {

  @GET
  @Path("/")
  public List<SingularitySlave> getSlaves(@QueryParam("state") Optional<MachineState> filterState) {

  }

  @GET
  @Path("/slave/{slaveId}")
  public List<SingularityMachineStateHistoryUpdate> getSlaveHistory(@PathParam("slaveId") String slaveId) {

  }

  @GET
  @Path("/slave/{slaveId}/details")
  public Optional<SingularitySlave> getSlave(@PathParam("slaveId") String slaveId) {

  }

  @DELETE
  @Path("/slave/{slaveId}")
  public void removeSlave(@PathParam("slaveId") String slaveId) {

  }

  @POST
  @Path("/slave/{slaveId}/decommission")
  public void decommissionSlave(@PathParam("slaveId") String slaveId, SingularityMachineChangeRequest changeRequest) {

  }

  @POST
  @Path("/slave/{slaveId}/freeze")
  public void freezeSlave(@PathParam("slaveId") String slaveId, SingularityMachineChangeRequest changeRequest) {

  }

  @POST
  @Path("/slave/{slaveId}/activate")
  public void activateSlave(@PathParam("slaveId") String slaveId, SingularityMachineChangeRequest changeRequest) {

  }

  @DELETE
  @Path("/slave/{slaveId}/expiring")

  public void deleteExpiringStateChange(@PathParam("slaveId") String slaveId) {

  }

  @GET
  @Path("/expiring")
  public List<SingularityExpiringMachineState> getExpiringStateChanges() {

  }
}
