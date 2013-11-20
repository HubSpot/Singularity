package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.data.CuratorManager.DeleteResult;
import com.hubspot.singularity.data.SlaveManager;

@Path("/slaves")
@Produces({ MediaType.APPLICATION_JSON })
public class SlaveResource {
  
  private final SlaveManager slaveManager;
  
  @Inject
  public SlaveResource(SlaveManager slaveManager) {
    this.slaveManager = slaveManager;
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
    if (slaveManager.removeDead(slaveId) == DeleteResult.DIDNT_EXIST) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }
  }
  
  @DELETE
  @Path("/slave/{slaveId}/decomissioning")
  public void removeDecomissioningSlave(@PathParam("slaveId") String slaveId) {
    if (slaveManager.removeDecomissioning(slaveId) == DeleteResult.DIDNT_EXIST) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }
  }
  
  @POST
  @Path("/slave/{slaveId}/decomission")
  public void decomissionRack(@PathParam("slaveId") String slaveId) {
    // TODO check if this mofo is already decomissioning or some shit
    slaveManager.decomission(slaveId);
  }
 
}
