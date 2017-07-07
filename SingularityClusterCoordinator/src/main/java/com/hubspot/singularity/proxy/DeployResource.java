package com.hubspot.singularity.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityUpdatePendingDeployRequest;
import com.hubspot.singularity.api.SingularityDeployRequest;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.DEPLOY_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class DeployResource {

  @GET
  @Path("/pending")
  public Iterable<SingularityPendingDeploy> getPendingDeploys() {

  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequestParent deploy(@Context HttpServletRequest requestContext, SingularityDeployRequest deployRequest) {

  }

  @DELETE
  @Path("/deploy/{deployId}/request/{requestId}")
  public SingularityRequestParent cancelDeploy(@PathParam("requestId") String requestId, @PathParam("deployId") String deployId) {
  }

  @POST
  @Path("/update")
  public SingularityRequestParent updatePendingDeploy(SingularityUpdatePendingDeployRequest updateRequest) {

  }
}
