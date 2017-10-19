package com.hubspot.singularity.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.api.SingularityS3SearchRequest;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.exceptions.NotImplemenedException;

@Path(ApiPaths.S3_LOG_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class S3LogResource extends ProxyResource {

  @Inject
  public S3LogResource() {}

  @GET
  @Path("/task/{taskId}")
  public Response getS3LogsForTask(@Context HttpServletRequest request, @PathParam("taskId") String taskId) throws Exception {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId());
  }

  @GET
  @Path("/request/{requestId}")
  public Response getS3LogsForRequest(@Context HttpServletRequest request, @PathParam("requestId") String requestId) throws Exception {
    return routeByRequestId(request, requestId);
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}")
  public Response getS3LogsForDeploy(@Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("deployId") String deployId) throws Exception {
    return routeByRequestId(request, requestId);
  }

  @POST
  @Path("/search")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response getPaginatedS3Logs(@Context HttpServletRequest request, SingularityS3SearchRequest search) throws Exception {
    // TODO - merge search results from multiple data centers, route if request id set
    throw new NotImplemenedException();
  }
}
