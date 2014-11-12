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

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.data.SlaveManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path(SlaveResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity slaves.", value=SlaveResource.PATH)
public class SlaveResource extends AbstractMachineResource<SingularitySlave> {
  public static final String PATH = SingularityService.API_BASE_PATH + "/slaves";

  private final SlaveManager slaveManager;

  @Inject
  public SlaveResource(SlaveManager slaveManager) {
    super(slaveManager);

    this.slaveManager = slaveManager;
  }

  @Override
  protected String getObjectTypeString() {
    return "Slave";
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/active")
  @ApiOperation("Retrieve the list of active slaves.")
  public List<SingularitySlave> getSlaves() {
    return slaveManager.getActiveObjects();
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/dead")
  @ApiOperation("Retrieve the list of dead slaves.")
  public List<SingularitySlave> getDead() {
    return slaveManager.getDeadObjects();
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/decomissioning")
  @ApiOperation("Retrieve the list of decommissioning slaves.")
  public List<SingularitySlave> getDecomissioning() {
    return slaveManager.getDecomissioningObjects();
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @Path("/slave/{slaveId}/dead")
  @ApiOperation("Remove a specific dead slave.")
  public void removeDeadSlave(@PathParam("slaveId") String slaveId) {
    super.removeDead(slaveId);
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @Path("/slave/{slaveId}/decomissioning")
  @ApiOperation("Remove a specific decommissioning slave")
  public void removeDecomissioningSlave(@PathParam("slaveId") String slaveId) {
    super.removeDecomissioning(slaveId);
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("/slave/{slaveId}/decomission")
  @ApiOperation("Decommission a specific slave.")
  public void decomissionSlave(@PathParam("slaveId") String slaveId, @QueryParam("user") Optional<String> user) {
    super.decomission(slaveId, user);
  }
}
