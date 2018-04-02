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
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskState;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;

import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Path(ApiPaths.TASK_TRACKER_RESOURCE_PATH)
@Produces({MediaType.APPLICATION_JSON})
@Schema(title = "Retrieve a task by taskId or runId")
@Tags({@Tag(name = "Task Tracking")})
public class TaskTrackerResource {
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final HistoryManager historyManager;
  private final SingularityAuthorizationHelper authorizationHelper;

  @Inject
  public TaskTrackerResource(TaskManager taskManager, RequestManager requestManager, HistoryManager historyManager, SingularityAuthorizationHelper authorizationHelper) {
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.historyManager = historyManager;
    this.authorizationHelper = authorizationHelper;
  }

  @GET
  @Path("/task/{taskId}")
  @Operation(
      summary = "Get the current state of a task by taskId whether it is active, or inactive",
      responses = {
          @ApiResponse(responseCode = "404", description = "Task with this id does not exist")
      }
  )
  public Optional<SingularityTaskState> getTaskState(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "the task id to search for") @PathParam("taskId") String taskId) {
    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.READ);
    return getTaskStateFromId(SingularityTaskId.valueOf(taskId));
  }

  @GET
  @Path("/run/{requestId}/{runId}")
  @Operation(
      summary = "Get the current state of a task by taskId whether it is pending, active, or inactive",
      responses = {
          @ApiResponse(responseCode = "404", description = "Task with this runId does not exist")
      }
  )
  public Optional<SingularityTaskState> getTaskStateByRunId(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "the request id to search for tasks") @PathParam("requestId") String requestId,
      @Parameter(required = true, description = "the run id to search for") @PathParam("runId") String runId) {
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

    for (SingularityPendingRequest pendingRequest : requestManager.getPendingRequests()) {
      if (pendingRequest.getRequestId().equals(requestId) && pendingRequest.getRunId().isPresent() && pendingRequest.getRunId().get().equals(runId)) {
        return Optional.of(new SingularityTaskState(
            Optional.absent(),
            Optional.absent(),
            pendingRequest.getRunId(),
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
