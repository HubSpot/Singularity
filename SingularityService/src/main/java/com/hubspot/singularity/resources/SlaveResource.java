package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.data.SlaveManager;

@Path("/v1/slaves")
@Produces({ MediaType.APPLICATION_JSON })
public class SlaveResource extends AbstractMachineResource<SingularitySlave> {
  
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
 
}
