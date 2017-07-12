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
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityUpdatePendingDeployRequest;
import com.hubspot.singularity.api.SingularityDeployRequest;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.DEPLOY_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class DeployResource extends ProxyResource {

  @Inject
  public DeployResource() {}

  @GET
  @Path("/pending")
  public Response getPendingDeploys(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response deploy(@Context HttpServletRequest request, SingularityDeployRequest deployRequest) {
    return routeByRequestId(request, deployRequest.getDeploy().getRequestId(), deployRequest);
  }

  @DELETE
  @Path("/deploy/{deployId}/request/{requestId}")
  public Response cancelDeploy(@Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("deployId") String deployId) {
    return routeByRequestId(request, requestId);
  }

  @POST
  @Path("/update")
  public Response updatePendingDeploy(@Context HttpServletRequest request, SingularityUpdatePendingDeployRequest updateRequest) {
    return routeByRequestId(request, updateRequest.getRequestId(), updateRequest);
  }
}
