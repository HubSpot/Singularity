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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityUpdatePendingDeployRequest;
import com.hubspot.singularity.api.SingularityDeployRequest;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.DEPLOY_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class DeployResource extends ProxyResource {

  @Inject
  public DeployResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    super(configuration, httpClient, objectMapper);
  }

  @GET
  @Path("/pending")
  public Iterable<SingularityPendingDeploy> getPendingDeploys() {
    throw new RuntimeException("not implemented");
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequestParent deploy(@Context HttpServletRequest requestContext, SingularityDeployRequest deployRequest) {
    throw new RuntimeException("not implemented");
  }

  @DELETE
  @Path("/deploy/{deployId}/request/{requestId}")
  public SingularityRequestParent cancelDeploy(@PathParam("requestId") String requestId, @PathParam("deployId") String deployId) {
    throw new RuntimeException("not implemented");
  }

  @POST
  @Path("/update")
  public SingularityRequestParent updatePendingDeploy(SingularityUpdatePendingDeployRequest updateRequest) {
    throw new RuntimeException("not implemented");
  }
}
