package com.hubspot.singularity.resources;

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
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.data.SlaveManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path(SlaveResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity slaves.", value=SlaveResource.PATH)
public class SlaveResource extends AbstractMachineResource<SingularitySlave> {
  public static final String PATH = SingularityService.API_BASE_PATH + "/slaves";

  @Inject
  public SlaveResource(SlaveManager slaveManager, SingularityAuthorizationHelper authorizationHelper, Optional<SingularityUser> user) {
    super(slaveManager, authorizationHelper, user);
  }

  @Override
  protected String getObjectTypeString() {
    return "Slave";
  }

  @GET
  @Path("/")
  @ApiOperation("Retrieve the list of all known slaves, optionally filtering by a particular state")
  public List<SingularitySlave> getSlaves(@ApiParam("Optionally specify a particular state to filter slaves by") @QueryParam("state") Optional<MachineState> filterState) {
    return manager.getObjectsFiltered(filterState);
  }

  @GET
  @Path("/slave/{slaveId}")
  @ApiOperation("Retrieve the history of a given slave")
  public List<SingularityMachineStateHistoryUpdate> getSlaveHistory(@ApiParam("Slave ID") @PathParam("slaveId") String slaveId) {
    return manager.getHistory(slaveId);
  }

  @DELETE
  @Path("/slave/{slaveId}")
  @ApiOperation("Remove a known slave, erasing history. This operation will cancel decomissioning of the slave")
  public void removeSlave(@ApiParam("Active SlaveId") @PathParam("slaveId") String slaveId) {
    super.remove(slaveId);
  }

  @POST
  @Path("/slave/{slaveId}/decomission")
  @Deprecated
  public void decomissionSlave(@ApiParam("Active slaveId") @PathParam("slaveId") String slaveId) {
    super.decommission(slaveId, JavaUtils.getUserEmail(user));
  }

  @POST
  @Path("/slave/{slaveId}/decommission")
  @ApiOperation("Begin decommissioning a specific active slave")
  public void decommissionSlave(@ApiParam("Active slaveId") @PathParam("slaveId") String slaveId) {
    super.decommission(slaveId, JavaUtils.getUserEmail(user));
  }

  @POST
  @Path("/slave/{slaveId}/freeze")
  @ApiOperation("Freeze tasks on a specific slave")
  public void freezeSlave(@ApiParam("Slave ID") @PathParam("slaveId") String slaveId) {
    super.freeze(slaveId, JavaUtils.getUserEmail(user));
  }

  @POST
  @Path("/slave/{slaveId}/activate")
  @ApiOperation("Activate a decomissioning slave, canceling decomission without erasing history")
  public void activateSlave(@ApiParam("Active slaveId") @PathParam("slaveId") String slaveId) {
    super.activate(slaveId, JavaUtils.getUserEmail(user));
  }

}
