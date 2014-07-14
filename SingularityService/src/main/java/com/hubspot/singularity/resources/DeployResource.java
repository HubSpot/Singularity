package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.jackson.jaxrs.PropertyFiltering;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.data.DeployManager;

@Path(SingularityService.API_BASE_PATH + "/deploys")
@Produces({ MediaType.APPLICATION_JSON })
public class DeployResource {

  private final DeployManager deployManager;
  
  @Inject
  public DeployResource(DeployManager deployManager) {
    this.deployManager = deployManager;
  }

  @GET
  @PropertyFiltering
  @Path("/pending")
  public List<SingularityPendingDeploy> getPendingDeploys() {
    return deployManager.getPendingDeploys();
  }

}
