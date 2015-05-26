package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkBadRequest;
import static com.hubspot.singularity.data.SingularityValidator.userIsAuthorizedForRequest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.DeployHistoryHelper;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.RequestHistoryHelper;
import com.hubspot.singularity.data.history.TaskHistoryHelper;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path(HistoryResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages historical data for tasks, requests, and deploys.", value=HistoryResource.PATH)
public class HistoryResource extends AbstractHistoryResource {
  public static final String PATH = SingularityService.API_BASE_PATH +  "/history";

  private final DeployHistoryHelper deployHistoryHelper;
  private final TaskHistoryHelper taskHistoryHelper;
  private final RequestHistoryHelper requestHistoryHelper;
  private final RequestManager requestManager;

  @Inject
  public HistoryResource(HistoryManager historyManager, TaskManager taskManager, DeployManager deployManager, DeployHistoryHelper deployHistoryHelper, TaskHistoryHelper taskHistoryHelper,
      RequestHistoryHelper requestHistoryHelper, RequestManager requestManager, SingularityValidator validator, Optional<SingularityUser> user) {
    super(historyManager, taskManager, deployManager, validator, user);

    this.requestHistoryHelper = requestHistoryHelper;
    this.deployHistoryHelper = deployHistoryHelper;
    this.taskHistoryHelper = taskHistoryHelper;
    this.requestManager = requestManager;
  }

  @GET
  @Path("/task/{taskId}")
  @ApiOperation("Retrieve the history for a specific task.")
  public SingularityTaskHistory getHistoryForTask(@ApiParam("Task ID to look up") @PathParam("taskId") String taskId) {
    validator.checkForAuthorizationByTaskId(taskId, user);

    SingularityTaskId taskIdObj = getTaskIdObject(taskId);

    return getTaskHistory(taskIdObj);
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
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(@ApiParam("Request ID to look up") @PathParam("requestId") String requestId) {
    validator.checkForAuthorizationByRequestId(requestId, user);

    List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIdsForRequest(requestId);

    return taskHistoryHelper.getHistoriesFor(activeTaskIds);
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}")
  @ApiOperation("Retrieve the history for a specific deploy.")
  public SingularityDeployHistory getDeploy(@ApiParam("Request ID for deploy") @PathParam("requestId") String requestId,
      @ApiParam("Deploy ID") @PathParam("deployId") String deployId) {
    validator.checkForAuthorizationByRequestId(requestId, user);
    return getDeployHistory(requestId, deployId);
  }

  @GET
  @Path("/request/{requestId}/tasks")
  @ApiOperation("Retrieve the history for all tasks of a specific request.")
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(@ApiParam("Request ID to look up") @PathParam("requestId") String requestId,
      @ApiParam("Maximum number of items to return") @QueryParam("count") Integer count,
      @ApiParam("Which page of items to view") @QueryParam("page") Integer page) {
    final Integer limitCount = getLimitCount(count);
    final Integer limitStart = getLimitStart(limitCount, page);

    validator.checkForAuthorizationByRequestId(requestId, user);

    return taskHistoryHelper.getBlendedHistory(requestId, limitStart, limitCount);
  }

  @GET
  @Path("/request/{requestId}/deploys")
  @ApiOperation("")
  public List<SingularityDeployHistory> getDeploys(@ApiParam("Request ID to look up") @PathParam("requestId") String requestId,
      @ApiParam("Maximum number of items to return") @QueryParam("count") Integer count,
      @ApiParam("Which page of items to view") @QueryParam("page") Integer page) {
    final Integer limitCount = getLimitCount(count);
    final Integer limitStart = getLimitStart(limitCount, page);

    validator.checkForAuthorizationByRequestId(requestId, user);

    return deployHistoryHelper.getBlendedHistory(requestId, limitStart, limitCount);
  }

  @GET
  @Path("/request/{requestId}/requests")
  @ApiOperation("")
  public List<SingularityRequestHistory> getRequestHistoryForRequest(@ApiParam("Request ID to look up") @PathParam("requestId") String requestId,
      @ApiParam("Naximum number of items to return") @QueryParam("count") Integer count,
      @ApiParam("Which page of items to view") @QueryParam("page") Integer page) {
    final Integer limitCount = getLimitCount(count);
    final Integer limitStart = getLimitStart(limitCount, page);

    validator.checkForAuthorizationByRequestId(requestId, user);

    return requestHistoryHelper.getBlendedHistory(requestId, limitStart, limitCount);
  }

  @GET
  @Path("/requests/search")
  @ApiOperation("Search for requests.")
  public Iterable<String> getRequestHistoryForRequestLike(@ApiParam("Request ID prefix to search for") @QueryParam("requestIdLike") String requestIdLike,
      @ApiParam("Maximum number of items to return") @QueryParam("count") Integer count,
      @ApiParam("Which page of items to view") @QueryParam("page") Integer page) {
    final Integer limitCount = getLimitCount(count);
    final Integer limitStart = getLimitStart(limitCount, page);

    return Iterables.filter(historyManager.getRequestHistoryLike(requestIdLike, limitStart, limitCount), new Predicate<String>() {
      @Override
      public boolean apply(String input) {
        return userIsAuthorizedForRequest(user, requestManager.getRequest(input));
      }
    });
  }

}
