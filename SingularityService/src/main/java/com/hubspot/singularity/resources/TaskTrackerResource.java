package com.hubspot.singularity.resources;

import java.util.Collections;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskState;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path(TaskTrackerResource.PATH)
@Produces({MediaType.APPLICATION_JSON})
public class TaskTrackerResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/track";
  private static final Logger LOG = LoggerFactory.getLogger(TaskTrackerResource.class);

  private final TaskManager taskManager;
  private final HistoryManager historyManager;

  @Inject
  public TaskTrackerResource(TaskManager taskManager, HistoryManager historyManager) {
    this.taskManager = taskManager;
    this.historyManager = historyManager;
  }

  @GET
  @Path("/task/{taskId}")
  @ApiOperation(value="Get the current state of a task by taskId whether it is active, or inactive")
  @ApiResponses({
      @ApiResponse(code=404, message="Task with this id does not exist")
  })
  public Optional<SingularityTaskState> getTaskState(@PathParam("taskId") String taskId) {
    return getTaskStateFromId(SingularityTaskId.valueOf(taskId));
  }

  @GET
  @Path("/run/{requestId}/{runId}")
  @ApiOperation(value="Get the current state of a task by taskId whether it is pending, active, or inactive")
  @ApiResponses({
      @ApiResponse(code=404, message="Task with this runId does not exist")
  })
  public Optional<SingularityTaskState> getTaskStateByRunId(@PathParam("requestId") String requestId, @PathParam("runId") String runId) {
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
