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
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import io.dropwizard.auth.Auth;

@Path(ApiPaths.SLAVE_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity slaves.", value=ApiPaths.SLAVE_RESOURCE_PATH)
public class SlaveResource extends AbstractMachineResource<SingularitySlave> {
  @Inject
  public SlaveResource(SlaveManager slaveManager, SingularityAuthorizationHelper authorizationHelper, SingularityValidator validator) {
    super(slaveManager, authorizationHelper, validator);
  }

  @Override
  protected String getObjectTypeString() {
    return "Slave";
  }

  @GET
  @Path("/")
  @ApiOperation("Retrieve the list of all known slaves, optionally filtering by a particular state")
  public List<SingularitySlave> getSlaves(@Auth SingularityUser user,
                                          @ApiParam("Optionally specify a particular state to filter slaves by") @QueryParam("state") Optional<MachineState> filterState) {
    authorizationHelper.checkAdminAuthorization(user);
    return manager.getObjectsFiltered(filterState);
  }

  @GET
  @Path("/slave/{slaveId}")
  @ApiOperation("Retrieve the history of a given slave")
  public List<SingularityMachineStateHistoryUpdate> getSlaveHistory(@Auth SingularityUser user, @ApiParam("Slave ID") @PathParam("slaveId") String slaveId) {
    authorizationHelper.checkAdminAuthorization(user);
    return manager.getHistory(slaveId);
  }

  @GET
  @Path("/slave/{slaveId}/details")
  @ApiOperation("Get information about a particular slave")
  public Optional<SingularitySlave> getSlave(@Auth SingularityUser user, @ApiParam("Slave ID") @PathParam("slaveId") String slaveId) {
    authorizationHelper.checkAdminAuthorization(user);
    return manager.getObject(slaveId);
  }

  @DELETE
  @Path("/slave/{slaveId}")
  @ApiOperation("Remove a known slave, erasing history. This operation will cancel decomissioning of the slave")
  public void removeSlave(@Auth SingularityUser user, @ApiParam("Active SlaveId") @PathParam("slaveId") String slaveId) {
    super.remove(slaveId, user);
  }

  @POST
  @Path("/slave/{slaveId}/decommission")
  @ApiOperation("Begin decommissioning a specific active slave")
  public void decommissionSlave(@Auth SingularityUser user, @ApiParam("Active slaveId") @PathParam("slaveId") String slaveId, SingularityMachineChangeRequest changeRequest) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.fromNullable(changeRequest);
    super.decommission(slaveId, maybeChangeRequest, user, SingularityAction.DECOMMISSION_SLAVE);
  }

  @POST
  @Path("/slave/{slaveId}/freeze")
  @ApiOperation("Freeze tasks on a specific slave")
  public void freezeSlave(@Auth SingularityUser user, @ApiParam("Slave ID") @PathParam("slaveId") String slaveId, SingularityMachineChangeRequest changeRequest) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.fromNullable(changeRequest);
    super.freeze(slaveId, maybeChangeRequest, user, SingularityAction.FREEZE_SLAVE);
  }

  @POST
  @Path("/slave/{slaveId}/activate")
  @ApiOperation("Activate a decomissioning slave, canceling decomission without erasing history")
  public void activateSlave(@Auth SingularityUser user, @ApiParam("Active slaveId") @PathParam("slaveId") String slaveId, SingularityMachineChangeRequest changeRequest) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.fromNullable(changeRequest);
    super.activate(slaveId, maybeChangeRequest, user, SingularityAction.ACTIVATE_SLAVE);
  }

  @DELETE
  @Path("/slave/{slaveId}/expiring")
  @ApiOperation("Delete any expiring machine state changes for this slave")
  public void deleteExpiringStateChange(@Auth SingularityUser user, @ApiParam("Active slaveId") @PathParam("slaveId") String slaveId) {
    super.cancelExpiring(slaveId, user);
  }

  @GET
  @Path("/expiring")
  @ApiOperation("Get all expiring state changes for all slaves")
  public List<SingularityExpiringMachineState> getExpiringStateChanges(@Auth SingularityUser user) {
    return super.getExpiringStateChanges(user);
  }

}
