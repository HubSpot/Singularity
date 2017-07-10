package com.hubspot.singularity.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.singularity.SingularitySandbox;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.SANDBOX_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class SandboxResource extends ProxyResource {

  @Inject
  public SandboxResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper, DataCenterLocator dataCenterLocator) {
    super(configuration, httpClient, objectMapper, dataCenterLocator);
  }

  @GET
  @Path("/{taskId}/browse")
  public SingularitySandbox browse(@Context HttpServletRequest request, @PathParam("taskId") String taskId) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId(), TypeRefs.SANDBOX_REF);
  }

  @GET
  @Path("/{taskId}/read")
  public MesosFileChunkObject read(@Context HttpServletRequest request, @PathParam("taskId") String taskId) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId(), TypeRefs.FILE_CHUNK_REF);
  }
}
