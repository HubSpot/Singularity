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
  public List<String> getSlaves() {
    return slaveManager.getActive();
  }
  
  @GET
  @Path("/dead")
  public List<String> getDead() {
    return slaveManager.getDead();
  }
  
  @GET
  @Path("/decomissioning")
  public List<String> getDecomissioning() {
    return slaveManager.getDecomissioning();
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
    slaveManager.decomission(slaveId);
  }
 
}
