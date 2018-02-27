package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkForbidden;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskState;
import org.apache.mesos.v1.Protos.TaskStatus;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.SingularityLeaderController;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.history.SingularityHistoryPurger;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;
import com.hubspot.singularity.scheduler.SingularityTaskReconciliation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Path(ApiPaths.TEST_RESOURCE_PATH)
@Schema(title = "Misc testing endpoints")
@Tags({@Tag(name = "Test")})
public class TestResource {
  private final SingularityAbort abort;
  private final SingularityLeaderController managed;
  private final SingularityConfiguration configuration;
  private final SingularityMesosScheduler scheduler;
  private final SingularityTaskReconciliation taskReconciliation;
  private final SingularityHistoryPurger historyPurger;

  @Inject
  public TestResource(SingularityConfiguration configuration, SingularityLeaderController managed, SingularityAbort abort, final SingularityMesosScheduler scheduler, SingularityTaskReconciliation taskReconciliation, SingularityHistoryPurger historyPurger) {
    this.configuration = configuration;
    this.managed = managed;
    this.abort = abort;
    this.scheduler = scheduler;
    this.taskReconciliation = taskReconciliation;
    this.historyPurger = historyPurger;
  }

  @POST
  @Path("/scheduler/statusUpdate/{taskId}/{taskState}")
  @Operation(
      summary = "Force an update for a specific task",
      responses = {
          @ApiResponse(responseCode = "403", description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable")
      }
  )
  public void statusUpdate(@PathParam("taskId") String taskId, @PathParam("taskState") String taskState) {
    checkForbidden(configuration.isAllowTestResourceCalls(), "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)");

    scheduler.statusUpdate(TaskStatus.newBuilder()
        .setTaskId(TaskID.newBuilder().setValue(taskId))
        .setState(TaskState.valueOf(taskState))
        .build()).join();
  }

  @POST
  @Path("/leader")
  @Operation(
      summary = "Make this instance of Singularity believe it's elected leader",
      responses = {
          @ApiResponse(responseCode = "403", description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable")
      }
  )
  public void setLeader() {
    checkForbidden(configuration.isAllowTestResourceCalls(), "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)");

    managed.isLeader();
  }

  @POST
  @Path("/notleader")
  @Operation(
      summary = "Make this instanceo of Singularity believe it's lost leadership",
      responses = {
          @ApiResponse(responseCode = "403", description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable")
      }
  )
  public void setNotLeader() {
    checkForbidden(configuration.isAllowTestResourceCalls(), "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)");

    managed.notLeader();
  }

  @POST
  @Path("/stop")
  @Operation(
      summary = "Stop the Mesos scheduler subscriber",
      responses = {
          @ApiResponse(responseCode = "403", description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable")
      }
  )
  public void stop() throws Exception {
    checkForbidden(configuration.isAllowTestResourceCalls(), "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)");

    managed.stop();
  }

  @POST
  @Path("/abort")
  @Operation(
      summary = "Abort the Mesos scheduler",
      responses = {
          @ApiResponse(responseCode = "403", description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable")
      }
  )
  public void abort() {
    checkForbidden(configuration.isAllowTestResourceCalls(), "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)");

    abort.abort(AbortReason.TEST_ABORT, Optional.<Throwable>absent());
  }

  @POST
  @Path("/start")
  @Operation(
      summary = "Start the Mesos scheduler driver",
      responses = {
          @ApiResponse(responseCode = "403", description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable")
      }
  )
  public void start() throws Exception {
    checkForbidden(configuration.isAllowTestResourceCalls(), "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)");

    managed.start();
  }

  @POST
  @Path("/exception")
  @Operation(
      summary = "Trigger an exception",
      responses = {
          @ApiResponse(responseCode = "403", description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable")
      }
  )
  public void throwException(
      @Parameter(description = "Exception message") @QueryParam("message") @DefaultValue("test exception") String message) {
    throw new RuntimeException(message);
  }

  @POST
  @Path("/reconcile")
  @Operation(
      summary = "Start task reconciliation",
      responses = {
          @ApiResponse(responseCode = "403", description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable")
      }
  )
  public void startTaskReconciliation() throws Exception {
    checkForbidden(configuration.isAllowTestResourceCalls(), "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)");
    taskReconciliation.startReconciliation();
  }

  @POST
  @Path("/purge-history")
  @Operation(
      summary = "Run a history purge",
      responses = {
          @ApiResponse(responseCode = "403", description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable")
      }
  )
  public void runHistoryPurge() throws Exception {
    checkForbidden(configuration.isAllowTestResourceCalls(), "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)");
    historyPurger.runActionOnPoll();
  }
}
