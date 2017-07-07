package com.hubspot.singularity.proxy;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityS3LogMetadata;
import com.hubspot.singularity.api.SingularityS3SearchRequest;
import com.hubspot.singularity.api.SingularityS3SearchResult;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.S3_LOG_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class S3LogResource extends ProxyResource {

  @Inject
  public S3LogResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    super(configuration, httpClient, objectMapper);
  }

  @GET
  @Path("/task/{taskId}")
  public List<SingularityS3LogMetadata> getS3LogsForTask(
      @PathParam("taskId") String taskId, @QueryParam("start") Optional<Long> start, @QueryParam("end") Optional<Long> end,
      @QueryParam("excludeMetadata") boolean excludeMetadata, @QueryParam("list") boolean listOnly) throws Exception {
    throw new NotImplemenedException();
  }

  @GET
  @Path("/request/{requestId}")
  public List<SingularityS3LogMetadata> getS3LogsForRequest(
      @PathParam("requestId") String requestId, @QueryParam("start") Optional<Long> start, @QueryParam("end") Optional<Long> end,
      @QueryParam("excludeMetadata") boolean excludeMetadata, @QueryParam("list") boolean listOnly, @QueryParam("maxPerPage") Optional<Integer> maxPerPage) throws Exception {
    throw new NotImplemenedException();
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}")
  public List<SingularityS3LogMetadata> getS3LogsForDeploy(
      @PathParam("requestId") String requestId, @PathParam("deployId") String deployId, @QueryParam("start") Optional<Long> start,
      @QueryParam("end") Optional<Long> end, @QueryParam("excludeMetadata") boolean excludeMetadata, @QueryParam("list") boolean listOnly,
      @QueryParam("maxPerPage") Optional<Integer> maxPerPage) throws Exception {
    throw new NotImplemenedException();
  }

  @POST
  @Path("/search")
  @Consumes(MediaType.APPLICATION_JSON)
  public SingularityS3SearchResult getPaginatedS3Logs(SingularityS3SearchRequest search) throws Exception {
    throw new NotImplemenedException();
  }
}
