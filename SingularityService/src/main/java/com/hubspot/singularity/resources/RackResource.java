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
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path(RackResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api( description="Manages Singularity racks.", value=RackResource.PATH )
public class RackResource extends AbstractMachineResource<SingularityRack> {
  public static final String PATH = SingularityService.API_BASE_PATH + "/racks";


  @Inject
  public RackResource(RackManager rackManager, SingularityValidator validator, Optional<SingularityUser> user) {
    super(rackManager, validator, user);
  }

  @Override
  protected String getObjectTypeString() {
    return "Rack";
  }

  @GET
  @Path("/")
  @ApiOperation("Retrieve the list of all known racks, optionally filtering by a particular state")
  public List<SingularityRack> getRacks(@ApiParam("Optionally specify a particular state to filter racks by") @QueryParam("state") Optional<MachineState> filterState) {
    validator.checkForAdminAuthorization(user);
    return manager.getObjectsFiltered(filterState);
  }

  @GET
  @Path("/rack/{rackId}")
  @ApiOperation("Retrieve the history of a given rack")
  public List<SingularityMachineStateHistoryUpdate> getRackHistory(@ApiParam("Rack ID") @PathParam("rackId") String rackId) {
    validator.checkForAdminAuthorization(user);
    return manager.getHistory(rackId);
  }

  @DELETE
  @Path("/rack/{rackId}")
  @ApiOperation("Remove a known rack, erasing history. This operation will cancel decomissioning of racks")
  public void removeRack(@ApiParam("Rack ID") @PathParam("rackId") String rackId, @QueryParam("user") Optional<String> queryUser) {
    validator.checkForAdminAuthorization(user);
    super.remove(rackId);
  }

  @POST
  @Path("/rack/{rackId}/decomission")
  @Deprecated
  public void decomissionRack(@ApiParam("Active rack ID") @PathParam("rackId") String rackId,
      @ApiParam("User requesting the decommisioning") @QueryParam("user") Optional<String> queryUser) {
    validator.checkForAdminAuthorization(user);
    super.decommission(rackId, queryUser);
  }

  @POST
  @Path("/rack/{rackId}/decommission")
  @ApiOperation("Begin decommissioning a specific active rack")
  public void decommissionRack(@ApiParam("Active rack ID") @PathParam("rackId") String rackId,
      @ApiParam("User requesting the decommisioning") @QueryParam("user") Optional<String> queryUser) {
    validator.checkForAdminAuthorization(user);
    super.decommission(rackId, queryUser);
  }

  @POST
  @Path("/rack/{rackId}/activate")
  @ApiOperation("Activate a decomissioning rack, canceling decomission without erasing history")
  public void activateSlave(@ApiParam("Active rackId") @PathParam("rackId") String rackId,
      @ApiParam("User requesting the activate") @QueryParam("user") Optional<String> queryUser) {
    validator.checkForAdminAuthorization(user);
    super.activate(rackId, queryUser);
  }

}
