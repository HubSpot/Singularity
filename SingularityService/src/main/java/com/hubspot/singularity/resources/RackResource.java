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
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.data.RackManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path(RackResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api( description="Manages Singularity racks.", value=RackResource.PATH )
public class RackResource extends AbstractMachineResource<SingularityRack> {
  public static final String PATH = SingularityService.API_BASE_PATH + "/racks";

  private final RackManager rackManager;

  @Inject
  public RackResource(RackManager rackManager) {
    super(rackManager);
    this.rackManager = rackManager;
  }

  @Override
  protected String getObjectTypeString() {
    return "Rack";
  }

  @GET
  @Path("/active")
  @ApiOperation("Retrieve the list of active racks. A rack is active if it has one or more active slaves associated with it.")
  public List<SingularityRack> getRacks() {
    return rackManager.getActiveObjects();
  }

  @GET
  @Path("/dead")
  @ApiOperation("Retrieve the list of dead racks. A rack is dead if it has zero active slaves.")
  public List<SingularityRack> getDead() {
    return rackManager.getDeadObjects();
  }

  @GET
  @Path("/decomissioning")
  @ApiOperation("Retrieve the list of decommissioning racks.")
  public List<SingularityRack> getDecomissioning() {
    return rackManager.getDecomissioningObjects();
  }

  @DELETE
  @Path("/rack/{rackId}/dead")
  @ApiOperation("Remove a dead rack.")
  public void removeDeadRack(@ApiParam("Dead rack ID.") @PathParam("rackId") String rackId) {
    super.removeDead(rackId);
  }

  @DELETE
  @Path("/rack/{rackId}/decomissioning")
  @ApiOperation("Undo the decomission operation on a specific decommissioning rack.")
  public void removeDecomissioningRack(@ApiParam("Decommissioned rack ID.") @PathParam("rackId") String rackId) {
    super.removeDecomissioning(rackId);
  }

  @POST
  @Path("/rack/{rackId}/decomission")
  @ApiOperation("Decomission a specific active rack.")
  public void decomissionRack(@ApiParam("Active rack ID.") @PathParam("rackId") String rackId,
                              @ApiParam("Username of person requestin the decommisioning.") @QueryParam("user") Optional<String> user) {
    super.decomission(rackId, user);
  }

}
