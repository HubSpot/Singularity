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
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.SlaveManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path(SlaveResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity slaves.", value=SlaveResource.PATH)
public class SlaveResource extends AbstractMachineResource<SingularitySlave> {
  public static final String PATH = SingularityService.API_BASE_PATH + "/slaves";

  private final SlaveManager slaveManager;
  private final SingularityValidator validator;

  private final Optional<SingularityUser> user;

  @Inject
  public SlaveResource(SlaveManager slaveManager, SingularityValidator validator, Optional<SingularityUser> user) {
    super(slaveManager);

    this.slaveManager = slaveManager;
    this.validator = validator;
    this.user = user;
  }

  @Override
  protected String getObjectTypeString() {
    return "Slave";
  }

  @GET
  @Path("/")
  @ApiOperation("Retrieve the list of all known slaves, optionally filtering by a particular state")
  public List<SingularitySlave> getSlaves(@ApiParam("Optionally specify a particular state to filter slaves by") @QueryParam("state") Optional<MachineState> filterState) {
    return slaveManager.getObjectsFiltered(filterState);
  }

  @GET
  @Path("/slave/{slaveId}")
  @ApiOperation("Retrieve the history of a given slave")
  public List<SingularityMachineStateHistoryUpdate> getSlaveHistory(@ApiParam("Slave ID") @PathParam("slaveId") String slaveId) {
    return slaveManager.getHistory(slaveId);
  }

  @DELETE
  @Path("/slave/{slaveId}")
  @ApiOperation("Remove a known slave, erasing history. This operation will cancel decomissioning of the slave")
  public void removeSlave(@ApiParam("Active SlaveId") @PathParam("slaveId") String slaveId, @QueryParam("user") Optional<String> queryUser) {
    validator.checkForAdminAuthorization(user);
    super.remove(slaveId);
  }

  @POST
  @Path("/slave/{slaveId}/decomission")
  @Deprecated
  public void decomissionSlave(@ApiParam("Active slaveId") @PathParam("slaveId") String slaveId,
      @ApiParam("User requesting the decommisioning") @QueryParam("user") Optional<String> queryUser) {
    validator.checkForAdminAuthorization(user);
    super.decommission(slaveId, queryUser);
  }

  @POST
  @Path("/slave/{slaveId}/decommission")
  @ApiOperation("Begin decommissioning a specific active slave")
  public void decommissionSlave(@ApiParam("Active slaveId") @PathParam("slaveId") String slaveId,
      @ApiParam("User requesting the decommisioning") @QueryParam("user") Optional<String> queryUser) {
    validator.checkForAdminAuthorization(user);
    super.decommission(slaveId, queryUser);
  }

  @POST
  @Path("/slave/{slaveId}/activate")
  @ApiOperation("Activate a decomissioning slave, canceling decomission without erasing history")
  public void activateSlave(@ApiParam("Active slaveId") @PathParam("slaveId") String slaveId,
      @ApiParam("User requesting the activate") @QueryParam("user") Optional<String> queryUser) {
    validator.checkForAdminAuthorization(user);
    super.activate(slaveId, queryUser);
  }

}
