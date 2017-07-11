package com.hubspot.singularity.proxy;

import java.util.List;

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

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequestParent;
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
  public List<SingularityPendingDeploy> getPendingDeploys(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.PENDING_DEPLOY_LIST_REF);
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequestParent deploy(@Context HttpServletRequest request, SingularityDeployRequest deployRequest) {
    return routeByRequestId(request, deployRequest.getDeploy().getRequestId(), deployRequest, TypeRefs.REQUEST_PARENT_REF);
  }

  @DELETE
  @Path("/deploy/{deployId}/request/{requestId}")
  public SingularityRequestParent cancelDeploy(@Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("deployId") String deployId) {
    return routeByRequestId(request, requestId, TypeRefs.REQUEST_PARENT_REF);
  }

  @POST
  @Path("/update")
  public SingularityRequestParent updatePendingDeploy(@Context HttpServletRequest request, SingularityUpdatePendingDeployRequest updateRequest) {
    return routeByRequestId(request, updateRequest.getRequestId(), updateRequest, TypeRefs.REQUEST_PARENT_REF);
  }
}
