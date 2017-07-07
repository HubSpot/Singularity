package com.hubspot.singularity.proxy;

import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.OrderDirection;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityPaginatedResponse;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.HISTORY_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class HistoryResource {

  @GET
  @Path("/task/{taskId}")
  public SingularityTaskHistory getHistoryForTask(@PathParam("taskId") String taskId) {

  }


  @GET
  @Path("/request/{requestId}/tasks/active")
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(@PathParam("requestId") String requestId) {

  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}")
  public SingularityDeployHistory getDeploy(@PathParam("requestId") String requestId, @PathParam("deployId") String deployId) {

  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}/tasks/active")
  public List<SingularityTaskIdHistory> getActiveDeployTasks( @PathParam("requestId") String requestId, @PathParam("deployId") String deployId) {

  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}/tasks/inactive")
  public List<SingularityTaskIdHistory> getInactiveDeployTasks(
      @PathParam("requestId") String requestId, @PathParam("deployId") String deployId,
       @QueryParam("count") Integer count, @QueryParam("page") Integer page) {

  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}/tasks/inactive/withmetadata")
  public SingularityPaginatedResponse<SingularityTaskIdHistory> getInactiveDeployTasksWithMetadata(
      @PathParam("requestId") String requestId, @PathParam("deployId") String deployId,
      @QueryParam("count") Integer count, @QueryParam("page") Integer page) {

  }

  @GET
  @Path("/tasks")
  public List<SingularityTaskIdHistory> getTaskHistory(
      @QueryParam("requestId") Optional<String> requestId, @QueryParam("deployId") Optional<String> deployId, @QueryParam("runId") Optional<String> runId,
      @QueryParam("host") Optional<String> host, @QueryParam("lastTaskStatus") Optional<ExtendedTaskState> lastTaskStatus, @QueryParam("startedBefore") Optional<Long> startedBefore,
      @QueryParam("startedAfter") Optional<Long> startedAfter, @QueryParam("updatedBefore") Optional<Long> updatedBefore, @QueryParam("updatedAfter") Optional<Long> updatedAfter,
      @QueryParam("orderDirection") Optional<OrderDirection> orderDirection, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {

  }

  @GET
  @Path("/tasks/withmetadata")
  public SingularityPaginatedResponse<SingularityTaskIdHistory> getTaskHistoryWithMetadata(
      @QueryParam("requestId") Optional<String> requestId, @QueryParam("deployId") Optional<String> deployId, @QueryParam("runId") Optional<String> runId,
      @QueryParam("host") Optional<String> host, @QueryParam("lastTaskStatus") Optional<ExtendedTaskState> lastTaskStatus, @QueryParam("startedBefore") Optional<Long> startedBefore,
      @QueryParam("startedAfter") Optional<Long> startedAfter, @QueryParam("updatedBefore") Optional<Long> updatedBefore, @QueryParam("updatedAfter") Optional<Long> updatedAfter,
      @QueryParam("orderDirection") Optional<OrderDirection> orderDirection, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {

  }

  @GET
  @Path("/request/{requestId}/tasks")
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(
      @PathParam("requestId") String requestId, @QueryParam("deployId") Optional<String> deployId, @QueryParam("runId") Optional<String> runId,
      @QueryParam("host") Optional<String> host, @QueryParam("lastTaskStatus") Optional<ExtendedTaskState> lastTaskStatus, @QueryParam("startedBefore") Optional<Long> startedBefore,
      @QueryParam("startedAfter") Optional<Long> startedAfter, @QueryParam("updatedBefore") Optional<Long> updatedBefore, @QueryParam("updatedAfter") Optional<Long> updatedAfter,
      @QueryParam("orderDirection") Optional<OrderDirection> orderDirection, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {

  }

  @GET
  @Path("/request/{requestId}/tasks/withmetadata")
  public SingularityPaginatedResponse<SingularityTaskIdHistory> getTaskHistoryForRequestWithMetadata(
      @PathParam("requestId") String requestId, @QueryParam("deployId") Optional<String> deployId, @QueryParam("runId") Optional<String> runId,
      @QueryParam("host") Optional<String> host, @QueryParam("lastTaskStatus") Optional<ExtendedTaskState> lastTaskStatus, @QueryParam("startedBefore") Optional<Long> startedBefore,
      @QueryParam("startedAfter") Optional<Long> startedAfter, @QueryParam("updatedBefore") Optional<Long> updatedBefore, @QueryParam("updatedAfter") Optional<Long> updatedAfter,
      @QueryParam("orderDirection") Optional<OrderDirection> orderDirection, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {

  }

  @GET
  @Path("/request/{requestId}/run/{runId}")
  public Optional<SingularityTaskIdHistory> getTaskHistoryForRequestAndRunId(
      @PathParam("requestId") String requestId, @PathParam("runId") String runId) {
  }

  @GET
  @Path("/request/{requestId}/deploys")
  public List<SingularityDeployHistory> getDeploys(
      @PathParam("requestId") String requestId, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {

  }

  @GET
  @Path("/request/{requestId}/deploys/withmetadata")
  public SingularityPaginatedResponse<SingularityDeployHistory> getDeploysWithMetadata(
      @PathParam("requestId") String requestId, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {

  }

  @GET
  @Path("/request/{requestId}/requests")
  public List<SingularityRequestHistory> getRequestHistoryForRequest(
      @PathParam("requestId") String requestId, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {

  }

  @GET
  @Path("/request/{requestId}/requests/withmetadata")
  public SingularityPaginatedResponse<SingularityRequestHistory> getRequestHistoryForRequestWithMetadata(
      @PathParam("requestId") String requestId, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {

  }

  @GET
  @Path("/requests/search")
  public Iterable<String> getRequestHistoryForRequestLike(@QueryParam("requestIdLike") String requestIdLike, @QueryParam("count") Integer count, @QueryParam("page") Integer page, @QueryParam("useWebCache") Boolean useWebCache) {

  }

  @GET
  @Path("/request/{requestId}/command-line-args")
  public Set<List<String>> getRecentCommandLineArgs(@PathParam("requestId") String requestId, @QueryParam("count") Optional<Integer> count) {

  }
}
