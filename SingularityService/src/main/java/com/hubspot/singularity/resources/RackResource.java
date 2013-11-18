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
import com.hubspot.singularity.data.RackManager;

@Path("/racks")
@Produces({ MediaType.APPLICATION_JSON })
public class RackResource {
  
  private final RackManager rackManager;
  
  @Inject
  public RackResource(RackManager rackManager) {
    this.rackManager = rackManager;
  }
  
  @GET
  @Path("/active")
  public List<String> getRacks() {
    return rackManager.getActive();
  }
  
  @GET
  @Path("/dead")
  public List<String> getDead() {
    return rackManager.getDead();
  }
  
  @GET
  @Path("/decomissioning")
  public List<String> getDecomissioning() {
    return rackManager.getDecomissioning();
  }

  @DELETE
  @Path("/rack/{rackId}/dead")
  public void removeDeadRack(@PathParam("rackId") String rackId) {
    if (rackManager.removeDead(rackId) == DeleteResult.DIDNT_EXIST) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }
  }
  
  @DELETE
  @Path("/rack/{rackId}/decomissioning")
  public void removeDecomissioningRack(@PathParam("rackId") String rackId) {
    if (rackManager.removeDecomissioning(rackId) == DeleteResult.DIDNT_EXIST) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }
  }
  
  @POST
  @Path("/rack/{rackId}/decomission")
  public void decomissionRack(@PathParam("rackId") String rackId) {
    rackManager.decomission(rackId);
  }
 
}
