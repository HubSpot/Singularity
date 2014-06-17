package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.mesos.MesosClient;

@Path(SingularityService.API_BASE_PATH + "/slaves")
@Produces({ MediaType.APPLICATION_JSON })
public class SlaveResource extends AbstractMachineResource<SingularitySlave> {
  
  private final SlaveManager slaveManager;
  private final MesosClient mesosClient;
  
  @Inject
  public SlaveResource(SlaveManager slaveManager, MesosClient mesosClient) {
    super(slaveManager);
    
    this.slaveManager = slaveManager;
    this.mesosClient = mesosClient;
  }
  
  @Override
  protected String getObjectTypeString() {
    return "Slave";
  }

  @GET
  @Path("/active")
  public List<SingularitySlave> getSlaves() {
    return slaveManager.getActiveObjects();
  }
  
  @GET
  @Path("/dead")
  public List<SingularitySlave> getDead() {
    return slaveManager.getDeadObjects();
  }
  
  @GET
  @Path("/decomissioning")
  public List<SingularitySlave> getDecomissioning() {
    return slaveManager.getDecomissioningObjects();
  }

  @DELETE
  @Path("/slave/{slaveId}/dead")
  public void removeDeadSlave(@PathParam("slaveId") String slaveId) {
    super.removeDead(slaveId);
  }
  
  @DELETE
  @Path("/slave/{slaveId}/decomissioning")
  public void removeDecomissioningSlave(@PathParam("slaveId") String slaveId) {
    super.removeDecomissioning(slaveId);
  }
  
  @POST
  @Path("/slave/{slaveId}/decomission")
  public void decomissionRack(@PathParam("slaveId") String slaveId) {
    super.decomission(slaveId);
  }

  @GET
  @Path("/slave/{slaveId}/statistics")
  public Optional<List<MesosTaskMonitorObject>> getSlaveTaskStatistics(@PathParam("slaveId") String slaveId) {
    final Optional<SingularitySlave> maybeSlave = slaveManager.getActiveObject(slaveId);

    if (maybeSlave.isPresent()) {
      return Optional.of(mesosClient.getSlaveResourceUsage(maybeSlave.get().getHost()));
    } else {
      return Optional.absent();
    }
  }
}
