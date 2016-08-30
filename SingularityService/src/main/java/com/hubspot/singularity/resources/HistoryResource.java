package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkBadRequest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.OrderDirection;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryQuery;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.DeployHistoryHelper;
import com.hubspot.singularity.data.history.DeployTaskHistoryHelper;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.RequestHistoryHelper;
import com.hubspot.singularity.data.history.TaskHistoryHelper;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path(HistoryResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description = "Manages historical data for tasks, requests, and deploys.", value = HistoryResource.PATH)
public class HistoryResource extends AbstractHistoryResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/history";

  private final DeployHistoryHelper deployHistoryHelper;
  private final TaskHistoryHelper taskHistoryHelper;
  private final RequestHistoryHelper requestHistoryHelper;
  private final DeployTaskHistoryHelper deployTaskHistoryHelper;

  @Inject
  public HistoryResource(HistoryManager historyManager, TaskManager taskManager, DeployManager deployManager, DeployHistoryHelper deployHistoryHelper, TaskHistoryHelper taskHistoryHelper,
      RequestHistoryHelper requestHistoryHelper, SingularityAuthorizationHelper authorizationHelper, Optional<SingularityUser> user, DeployTaskHistoryHelper deployTaskHistoryHelper) {
    super(historyManager, taskManager, deployManager, authorizationHelper, user);

    this.requestHistoryHelper = requestHistoryHelper;
    this.deployHistoryHelper = deployHistoryHelper;
    this.taskHistoryHelper = taskHistoryHelper;
    this.deployTaskHistoryHelper = deployTaskHistoryHelper;
  }

  @GET
  @Path("/task/{taskId}")
  @ApiOperation("Retrieve the history for a specific task.")
  public SingularityTaskHistory getHistoryForTask(@ApiParam("Task ID to look up") @PathParam("taskId") String taskId) {
    SingularityTaskId taskIdObj = getTaskIdObject(taskId);

    return getTaskHistoryRequired(taskIdObj);
  }

  private Integer getLimitCount(Integer countParam) {
    if (countParam == null) {
      return 100;
    }

    checkBadRequest(countParam >= 0, "count param must be non-zero");

    if (countParam > 1000) {
      return 1000;
    }

    return countParam;
  }

  private Integer getLimitStart(Integer limitCount, Integer pageParam) {
    if (pageParam == null) {
      return 0;
    }

    checkBadRequest(pageParam >= 1, "page param must be 1 or greater");

    return limitCount * (pageParam - 1);
  }

  @GET
  @Path("/request/{requestId}/tasks/active")
  @ApiOperation("Retrieve the history for all active tasks of a specific request.")
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(
      @ApiParam("Request ID to look up") @PathParam("requestId") String requestId) {

    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, SingularityAuthorizationScope.READ);
    List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIdsForRequest(requestId);

    return taskHistoryHelper.getTaskHistoriesFor(taskManager, activeTaskIds);
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}")
  @ApiOperation("Retrieve the history for a specific deploy.")
  public SingularityDeployHistory getDeploy(@ApiParam("Request ID for deploy") @PathParam("requestId") String requestId,
      @ApiParam("Deploy ID") @PathParam("deployId") String deployId) {
    return getDeployHistory(requestId, deployId);
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}/tasks/active")
  @ApiOperation("Retrieve the task history for a specific deploy.")
  public List<SingularityTaskIdHistory> getActiveDeployTasks(
      @ApiParam("Request ID for deploy") @PathParam("requestId") String requestId,
      @ApiParam("Deploy ID") @PathParam("deployId") String deployId) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, SingularityAuthorizationScope.READ);
    List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIdsForDeploy(requestId, deployId);
    return taskHistoryHelper.getTaskHistoriesFor(taskManager, activeTaskIds);
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}/tasks/inactive")
  @ApiOperation("Retrieve the task history for a specific deploy.")
  public List<SingularityTaskIdHistory> getInactiveDeployTasks(
      @ApiParam("Request ID for deploy") @PathParam("requestId") String requestId,
      @ApiParam("Deploy ID") @PathParam("deployId") String deployId,
      @ApiParam("Maximum number of items to return") @QueryParam("count") Integer count,
      @ApiParam("Which page of items to view") @QueryParam("page") Integer page) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, SingularityAuthorizationScope.READ);

    final Integer limitCount = getLimitCount(count);
    final Integer limitStart = getLimitStart(limitCount, page);

    SingularityDeployKey key = new SingularityDeployKey(requestId, deployId);
    return deployTaskHistoryHelper.getBlendedHistory(key, limitStart, limitCount);
  }

  @GET
  @Path("/tasks")
  @ApiOperation("Retrieve the history sorted by startedAt for all inactive tasks.")
  public List<SingularityTaskIdHistory> getTaskHistory(
      @ApiParam("Optional Request ID to match") @QueryParam("requestId") Optional<String> requestId,
      @ApiParam("Optional deploy ID to match") @QueryParam("deployId") Optional<String> deployId,
      @ApiParam("Optional host to match") @QueryParam("host") Optional<String> host,
      @ApiParam("Optional last task status to match") @QueryParam("lastTaskStatus") Optional<ExtendedTaskState> lastTaskStatus,
      @ApiParam("Optionally match only tasks started after") @QueryParam("startedAfter") Optional<Long> startedAfter,
      @ApiParam("Optionally match only tasks started before") @QueryParam("startedBefore") Optional<Long> startedBefore,
      @ApiParam("Optionally match tasks last updated before") @QueryParam("updatedBefore") Optional<Long> updatedBefore,
      @ApiParam("Optionally match tasks last updated after") @QueryParam("updatedAfter") Optional<Long> updatedAfter,
      @ApiParam("Sort direction") @QueryParam("orderDirection") Optional<OrderDirection> orderDirection,
      @ApiParam("Maximum number of items to return") @QueryParam("count") Integer count,
      @ApiParam("Which page of items to view") @QueryParam("page") Integer page) {
    if (requestId.isPresent()) {
      authorizationHelper.checkForAuthorizationByRequestId(requestId.get(), user, SingularityAuthorizationScope.READ);
    } else {
      authorizationHelper.checkAdminAuthorization(user);
    }

    final Integer limitCount = getLimitCount(count);
    final Integer limitStart = getLimitStart(limitCount, page);

    return taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(requestId, deployId, host, lastTaskStatus, startedBefore, startedAfter,
        updatedBefore, updatedAfter, orderDirection), limitStart, limitCount);
  }

  @GET
  @Path("/request/{requestId}/tasks")
  @ApiOperation("Retrieve the history sorted by startedAt for all inactive tasks of a specific request.")
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(
      @ApiParam("Request ID to match") @PathParam("requestId") String requestId,
      @ApiParam("Optional deploy ID to match") @QueryParam("deployId") Optional<String> deployId,
      @ApiParam("Optional host to match") @QueryParam("host") Optional<String> host,
      @ApiParam("Optional last task status to match") @QueryParam("lastTaskStatus") Optional<ExtendedTaskState> lastTaskStatus,
      @ApiParam("Optionally match only tasks started after") @QueryParam("startedAfter") Optional<Long> startedAfter,
      @ApiParam("Optionally match only tasks started before") @QueryParam("startedBefore") Optional<Long> startedBefore,
      @ApiParam("Optionally match tasks last updated before") @QueryParam("updatedBefore") Optional<Long> updatedBefore,
      @ApiParam("Optionally match tasks last updated after") @QueryParam("updatedAfter") Optional<Long> updatedAfter,
      @ApiParam("Sort direction") @QueryParam("orderDirection") Optional<OrderDirection> orderDirection,
      @ApiParam("Maximum number of items to return") @QueryParam("count") Integer count,
      @ApiParam("Which page of items to view") @QueryParam("page") Integer page) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, SingularityAuthorizationScope.READ);

    final Integer limitCount = getLimitCount(count);
    final Integer limitStart = getLimitStart(limitCount, page);

    return taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(Optional.of(requestId), deployId, host, lastTaskStatus, startedBefore, startedAfter,
        updatedBefore, updatedAfter, orderDirection), limitStart, limitCount);
  }

  @GET
  @Path("/request/{requestId}/run/{runId}")
  @ApiOperation("Retrieve the history for a task by runId")
  public Optional<SingularityTaskIdHistory> getTaskHistoryForRequestAndRunId(
      @ApiParam("Request ID to look up") @PathParam("requestId") String requestId,
      @ApiParam("runId to look up") @PathParam("runId") String runId) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, SingularityAuthorizationScope.READ);

    return taskHistoryHelper.getByRunId(requestId, runId);
  }

  @GET
  @Path("/request/{requestId}/deploys")
  @ApiOperation("Get deploy history for a single request")
  public List<SingularityDeployHistory> getDeploys(
      @ApiParam("Request ID to look up") @PathParam("requestId") String requestId,
      @ApiParam("Maximum number of items to return") @QueryParam("count") Integer count,
      @ApiParam("Which page of items to view") @QueryParam("page") Integer page) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, SingularityAuthorizationScope.READ);

    final Integer limitCount = getLimitCount(count);
    final Integer limitStart = getLimitStart(limitCount, page);

    return deployHistoryHelper.getBlendedHistory(requestId, limitStart, limitCount);
  }

  @GET
  @Path("/request/{requestId}/requests")
  @ApiOperation("Get request history for a single request")
  public List<SingularityRequestHistory> getRequestHistoryForRequest(
      @ApiParam("Request ID to look up") @PathParam("requestId") String requestId,
      @ApiParam("Naximum number of items to return") @QueryParam("count") Integer count,
      @ApiParam("Which page of items to view") @QueryParam("page") Integer page) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, SingularityAuthorizationScope.READ);

    final Integer limitCount = getLimitCount(count);
    final Integer limitStart = getLimitStart(limitCount, page);

    return requestHistoryHelper.getBlendedHistory(requestId, limitStart, limitCount);
  }

  @GET
  @Path("/requests/search")
  @ApiOperation("Search for requests.")
  public Iterable<String> getRequestHistoryForRequestLike(
      @ApiParam("Request ID prefix to search for") @QueryParam("requestIdLike") String requestIdLike,
      @ApiParam("Maximum number of items to return") @QueryParam("count") Integer count,
      @ApiParam("Which page of items to view") @QueryParam("page") Integer page) {
    final Integer limitCount = getLimitCount(count);
    final Integer limitStart = getLimitStart(limitCount, page);

    List<String> requestIds = historyManager.getRequestHistoryLike(requestIdLike, limitStart, limitCount);

    return authorizationHelper.filterAuthorizedRequestIds(user, requestIds, SingularityAuthorizationScope.READ);  // TODO: will this screw up pagination? A: yes.
  }

}
