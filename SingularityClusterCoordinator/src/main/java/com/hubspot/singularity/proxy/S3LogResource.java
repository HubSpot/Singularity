package com.hubspot.singularity.proxy;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityS3LogMetadata;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.api.SingularityS3SearchRequest;
import com.hubspot.singularity.api.SingularityS3SearchResult;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.hubspot.singularity.exceptions.NotImplemenedException;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.S3_LOG_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class S3LogResource extends ProxyResource {

  @Inject
  public S3LogResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper, DataCenterLocator dataCenterLocator) {
    super(configuration, httpClient, objectMapper, dataCenterLocator);
  }

  @GET
  @Path("/task/{taskId}")
  public List<SingularityS3LogMetadata> getS3LogsForTask(@Context HttpServletRequest request, @PathParam("taskId") String taskId) throws Exception {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId(), TypeRefs.LOG_METADATA_LIST_REF);
  }

  @GET
  @Path("/request/{requestId}")
  public List<SingularityS3LogMetadata> getS3LogsForRequest(@Context HttpServletRequest request, @PathParam("requestId") String requestId) throws Exception {
    return routeByRequestId(request, requestId, TypeRefs.LOG_METADATA_LIST_REF);
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}")
  public List<SingularityS3LogMetadata> getS3LogsForDeploy(@Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("deployId") String deployId) throws Exception {
    return routeByRequestId(request, requestId, TypeRefs.LOG_METADATA_LIST_REF);
  }

  @POST
  @Path("/search")
  @Consumes(MediaType.APPLICATION_JSON)
  public SingularityS3SearchResult getPaginatedS3Logs(@Context HttpServletRequest request, SingularityS3SearchRequest search) throws Exception {
    // TODO - merge search results from multiple data centers, route if request id set
    throw new NotImplemenedException();
  }
}
