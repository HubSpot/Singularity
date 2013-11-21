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
import com.hubspot.singularity.data.AbstractMachineManager.DecomissionResult;
import com.hubspot.singularity.data.CuratorManager.DeleteResult;
import com.hubspot.singularity.data.SlaveManager;
import com.sun.jersey.api.ConflictException;
import com.sun.jersey.api.NotFoundException;

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
      throw new NotFoundException(String.format("Couldn't find a dead slave with id %s", slaveId));
    }
  }
  
  @DELETE
  @Path("/slave/{slaveId}/decomissioning")
  public void removeDecomissioningSlave(@PathParam("slaveId") String slaveId) {
    if (slaveManager.removeDecomissioning(slaveId) == DeleteResult.DIDNT_EXIST) {
      throw new NotFoundException(String.format("Couldn't find a decomissioning slave with id %s", slaveId));
    }
  }
  
  @POST
  @Path("/slave/{slaveId}/decomission")
  public void decomissionRack(@PathParam("slaveId") String slaveId) {
    DecomissionResult result = slaveManager.decomission(slaveId);
  
    if (result == DecomissionResult.FAILURE_NOT_FOUND || result == DecomissionResult.FAILURE_DEAD) {
      throw new NotFoundException(String.format("Couldn't find an active slave with id %s (result: %s)", slaveId, result.name()));
    } else if (result == DecomissionResult.FAILURE_ALREADY_DECOMISSIONING) {
      throw new ConflictException(String.format("Slave %s is already in decomissioning state", slaveId));
    }
  }
 
}
