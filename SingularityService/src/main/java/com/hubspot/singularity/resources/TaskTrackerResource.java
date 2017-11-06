package com.hubspot.singularity.resources;

import java.util.Collections;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskState;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import io.dropwizard.auth.Auth;

@Path(ApiPaths.TASK_TRACKER_RESOURCE_PATH)
@Produces({MediaType.APPLICATION_JSON})
@Api(description="Find a task by taskId or runId", value=ApiPaths.TASK_TRACKER_RESOURCE_PATH)
public class TaskTrackerResource {
  private final TaskManager taskManager;
  private final HistoryManager historyManager;
  private final SingularityAuthorizationHelper authorizationHelper;

  @Inject
  public TaskTrackerResource(TaskManager taskManager, HistoryManager historyManager, SingularityAuthorizationHelper authorizationHelper) {
    this.taskManager = taskManager;
    this.historyManager = historyManager;
    this.authorizationHelper = authorizationHelper;
  }

  @GET
  @Path("/task/{taskId}")
  @ApiOperation(value="Get the current state of a task by taskId whether it is active, or inactive")
  @ApiResponses({
      @ApiResponse(code=404, message="Task with this id does not exist")
  })
  public Optional<SingularityTaskState> getTaskState(@Auth SingularityUser user, @PathParam("taskId") String taskId) {
    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.READ);
    return getTaskStateFromId(SingularityTaskId.valueOf(taskId));
  }

  @GET
  @Path("/run/{requestId}/{runId}")
  @ApiOperation(value="Get the current state of a task by taskId whether it is pending, active, or inactive")
  @ApiResponses({
      @ApiResponse(code=404, message="Task with this runId does not exist")
  })
  public Optional<SingularityTaskState> getTaskStateByRunId(@Auth SingularityUser user, @PathParam("requestId") String requestId, @PathParam("runId") String runId) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, SingularityAuthorizationScope.READ);

    // Check if it's active or inactive
    Optional<SingularityTaskId> maybeTaskId = taskManager.getTaskByRunId(requestId, runId);
    if (maybeTaskId.isPresent()) {
      Optional<SingularityTaskState> maybeTaskState = getTaskStateFromId(maybeTaskId.get());
      if (maybeTaskState.isPresent()) {
        return maybeTaskState;
      }
    } else {
      Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistoryByRunId(requestId, runId);
      if (maybeTaskHistory.isPresent()) {
        return Optional.of(SingularityTaskState.fromTaskHistory(maybeTaskHistory.get()));
      }
    }
    // Check if it's pending
    for (SingularityPendingTask pendingTask : taskManager.getPendingTasksForRequest(requestId)) {
      if (pendingTask.getRunId().isPresent() && pendingTask.getRunId().get().equals(runId)) {
        return Optional.of(new SingularityTaskState(
            Optional.absent(),
            pendingTask.getPendingTaskId(),
            pendingTask.getRunId(),
            Optional.absent(),
            Collections.emptyList(),
            true
        ));
      }
    }
    return Optional.absent();
  }

  private Optional<SingularityTaskState> getTaskStateFromId(SingularityTaskId singularityTaskId) {
    Optional<SingularityTaskHistory> maybeTaskHistory = taskManager.getTaskHistory(singularityTaskId).or(historyManager.getTaskHistory(singularityTaskId.toString()));
    if (maybeTaskHistory.isPresent() && maybeTaskHistory.get().getLastTaskUpdate().isPresent()) {
      return Optional.of(SingularityTaskState.fromTaskHistory(maybeTaskHistory.get()));
    } else {
      return Optional.absent();
    }
  }
}
