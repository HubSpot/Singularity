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
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path(ApiPaths.RACK_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api( description="Manages Singularity racks.", value=ApiPaths.RACK_RESOURCE_PATH )
public class RackResource extends AbstractMachineResource<SingularityRack> {

  @Inject
  public RackResource(RackManager rackManager, SingularityAuthorizationHelper authorizationHelper, Optional<SingularityUser> user, SingularityValidator validator) {
    super(rackManager, authorizationHelper, user, validator);
  }

  @Override
  protected String getObjectTypeString() {
    return "Rack";
  }

  @GET
  @Path("/")
  @ApiOperation("Retrieve the list of all known racks, optionally filtering by a particular state")
  public List<SingularityRack> getRacks(@ApiParam("Optionally specify a particular state to filter racks by") @QueryParam("state") Optional<MachineState> filterState) {
    return manager.getObjectsFiltered(filterState);
  }

  @GET
  @Path("/rack/{rackId}")
  @ApiOperation("Retrieve the history of a given rack")
  public List<SingularityMachineStateHistoryUpdate> getRackHistory(@ApiParam("Rack ID") @PathParam("rackId") String rackId) {
    return manager.getHistory(rackId);
  }

  @DELETE
  @Path("/rack/{rackId}")
  @ApiOperation("Remove a known rack, erasing history. This operation will cancel decommissioning of racks")
  public void removeRack(@ApiParam("Rack ID") @PathParam("rackId") String rackId) {
    super.remove(rackId);
  }

  @POST
  @Path("/rack/{rackId}/decommission")
  @ApiOperation("Begin decommissioning a specific active rack")
  public void decommissionRack(@ApiParam("Active rack ID") @PathParam("rackId") String rackId, SingularityMachineChangeRequest changeRequest) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.fromNullable(changeRequest);
    super.decommission(rackId, maybeChangeRequest, JavaUtils.getUserEmail(user), SingularityAction.DECOMMISSION_RACK);
  }

  @POST
  @Path("/rack/{rackId}/freeze")
  @ApiOperation("Freeze a specific rack")
  public void freezeRack(@ApiParam("Rack ID") @PathParam("rackId") String rackId, SingularityMachineChangeRequest changeRequest) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.fromNullable(changeRequest);
    super.freeze(rackId, maybeChangeRequest, JavaUtils.getUserEmail(user), SingularityAction.FREEZE_RACK);
  }

  @POST
  @Path("/rack/{rackId}/activate")
  @ApiOperation("Activate a decomissioning rack, canceling decomission without erasing history")
  public void activateRack(@ApiParam("Active rackId") @PathParam("rackId") String rackId, SingularityMachineChangeRequest changeRequest) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.fromNullable(changeRequest);
    super.activate(rackId, maybeChangeRequest, JavaUtils.getUserEmail(user), SingularityAction.ACTIVATE_RACK);
  }

  @DELETE
  @Path("/rack/{rackId}/expiring")
  @ApiOperation("Delete any expiring machine state changes for this rack")
  public void deleteExpiringStateChange(@ApiParam("Active slaveId") @PathParam("rackId") String rackId) {
    super.cancelExpiring(rackId);
  }

  @GET
  @Path("/expiring")
  @ApiOperation("Get all expiring state changes for all racks")
  public List<SingularityExpiringMachineState> getExpiringStateChanges() {
    return super.getExpiringStateChanges();
  }

}
