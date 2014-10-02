package com.hubspot.singularity.resources;

import java.util.List;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.jackson.jaxrs.PropertyFiltering;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;

@Path(SingularityService.API_BASE_PATH + "/deploys")
@Produces({ MediaType.APPLICATION_JSON })
public class DeployResource extends AbstractRequestResource {

  private final DeployManager deployManager;

  @Inject
  public DeployResource(RequestManager requestManager, DeployManager deployManager, SingularityValidator validator) {
    super(requestManager, deployManager, validator);
    this.deployManager = deployManager;
  }

  @GET
  @PropertyFiltering
  @Path("/pending")
  public List<SingularityPendingDeploy> getPendingDeploys() {
    return deployManager.getPendingDeploys();
  }

  @Override
  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequestParent deploy(SingularityDeploy pendingDeploy, @QueryParam("user") Optional<String> user) {
    return super.deploy(pendingDeploy, user);
  }

  @Override
  @DELETE
  @Path("/deploy/{deployId}/request/{requestId}")
  public SingularityRequestParent cancelDeploy(@PathParam("requestId") String requestId, @PathParam("deployId") String deployId, @QueryParam("user") Optional<String> user) {
    return super.cancelDeploy(requestId, deployId, user);
  }

}
