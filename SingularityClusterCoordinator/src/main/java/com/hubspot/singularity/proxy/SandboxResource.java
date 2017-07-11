package com.hubspot.singularity.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.SANDBOX_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class SandboxResource extends ProxyResource {

  @Inject
  public SandboxResource() {}

  @GET
  @Path("/{taskId}/browse")
  public Response browse(@Context HttpServletRequest request, @PathParam("taskId") String taskId) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId());
  }

  @GET
  @Path("/{taskId}/read")
  public Response read(@Context HttpServletRequest request, @PathParam("taskId") String taskId) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId());
  }
}
