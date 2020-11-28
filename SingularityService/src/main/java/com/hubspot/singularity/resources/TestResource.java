package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkForbidden;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.SingularityLeaderController;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.SingularityDeployHistoryPersister;
import com.hubspot.singularity.data.history.SingularityHistoryPurger;
import com.hubspot.singularity.data.history.SingularityRequestHistoryPersister;
import com.hubspot.singularity.data.history.SingularityTaskHistoryPersister;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;
import com.hubspot.singularity.scheduler.SingularityTaskReconciliation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskState;
import org.apache.mesos.v1.Protos.TaskStatus;

@Path(ApiPaths.TEST_RESOURCE_PATH)
@Schema(title = "Misc testing endpoints")
@Tags({ @Tag(name = "Test") })
public class TestResource {
  private final SingularityAbort abort;
  private final SingularityLeaderController managed;
  private final SingularityConfiguration configuration;
  private final SingularityMesosScheduler scheduler;
  private final SingularityTaskReconciliation taskReconciliation;
  private final SingularityHistoryPurger historyPurger;
  private final HistoryManager historyManager;
  private final SingularityTaskHistoryPersister taskHistoryPersister;
  private final SingularityDeployHistoryPersister deployHistoryPersister;
  private final SingularityRequestHistoryPersister requestHistoryPersister;

  @Inject
  public TestResource(
    SingularityConfiguration configuration,
    SingularityLeaderController managed,
    SingularityAbort abort,
    final SingularityMesosScheduler scheduler,
    SingularityTaskReconciliation taskReconciliation,
    SingularityHistoryPurger historyPurger,
    HistoryManager historyManager,
    SingularityTaskHistoryPersister taskHistoryPersister,
    SingularityDeployHistoryPersister deployHistoryPersister,
    SingularityRequestHistoryPersister requestHistoryPersister
  ) {
    this.configuration = configuration;
    this.managed = managed;
    this.abort = abort;
    this.scheduler = scheduler;
    this.taskReconciliation = taskReconciliation;
    this.historyPurger = historyPurger;
    this.historyManager = historyManager;
    this.taskHistoryPersister = taskHistoryPersister;
    this.deployHistoryPersister = deployHistoryPersister;
    this.requestHistoryPersister = requestHistoryPersister;
  }

  @POST
  @Path("/scheduler/statusUpdate/{taskId}/{taskState}")
  @Operation(
    summary = "Force an update for a specific task",
    responses = {
      @ApiResponse(
        responseCode = "403",
        description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable"
      )
    }
  )
  public void statusUpdate(
    @PathParam("taskId") String taskId,
    @PathParam("taskState") String taskState
  ) {
    checkForbidden(
      configuration.isAllowTestResourceCalls(),
      "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)"
    );

    scheduler
      .statusUpdate(
        TaskStatus
          .newBuilder()
          .setTaskId(TaskID.newBuilder().setValue(taskId))
          .setState(TaskState.valueOf(taskState))
          .build()
      )
      .join();
  }

  @POST
  @Path("/leader")
  @Operation(
    summary = "Make this instance of Singularity believe it's elected leader",
    responses = {
      @ApiResponse(
        responseCode = "403",
        description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable"
      )
    }
  )
  public void setLeader() {
    checkForbidden(
      configuration.isAllowTestResourceCalls(),
      "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)"
    );

    managed.isLeader();
  }

  @POST
  @Path("/notleader")
  @Operation(
    summary = "Make this instanceo of Singularity believe it's lost leadership",
    responses = {
      @ApiResponse(
        responseCode = "403",
        description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable"
      )
    }
  )
  public void setNotLeader() {
    checkForbidden(
      configuration.isAllowTestResourceCalls(),
      "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)"
    );

    managed.notLeader();
  }

  @POST
  @Path("/stop")
  @Operation(
    summary = "Stop the Mesos scheduler subscriber",
    responses = {
      @ApiResponse(
        responseCode = "403",
        description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable"
      )
    }
  )
  public void stop() throws Exception {
    checkForbidden(
      configuration.isAllowTestResourceCalls(),
      "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)"
    );

    managed.stop();
  }

  @POST
  @Path("/abort")
  @Operation(
    summary = "Abort the Mesos scheduler",
    responses = {
      @ApiResponse(
        responseCode = "403",
        description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable"
      )
    }
  )
  public void abort() {
    checkForbidden(
      configuration.isAllowTestResourceCalls(),
      "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)"
    );

    abort.abort(
      AbortReason.TEST_ABORT,
      Optional.of(new RuntimeException("for a stack trace"))
    );
  }

  @POST
  @Path("/start")
  @Operation(
    summary = "Start the Mesos scheduler driver",
    responses = {
      @ApiResponse(
        responseCode = "403",
        description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable"
      )
    }
  )
  public void start() throws Exception {
    checkForbidden(
      configuration.isAllowTestResourceCalls(),
      "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)"
    );

    managed.start();
  }

  @POST
  @Path("/exception")
  @Operation(
    summary = "Trigger an exception",
    responses = {
      @ApiResponse(
        responseCode = "403",
        description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable"
      )
    }
  )
  public void throwException(
    @Parameter(description = "Exception message") @QueryParam("message") @DefaultValue(
      "test exception"
    ) String message
  ) {
    throw new RuntimeException(message);
  }

  @POST
  @Path("/reconcile")
  @Operation(
    summary = "Start task reconciliation",
    responses = {
      @ApiResponse(
        responseCode = "403",
        description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable"
      )
    }
  )
  public void startTaskReconciliation() throws Exception {
    checkForbidden(
      configuration.isAllowTestResourceCalls(),
      "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)"
    );
    taskReconciliation.startReconciliation();
  }

  @POST
  @Path("/purge-history")
  @Operation(
    summary = "Run a history purge",
    responses = {
      @ApiResponse(
        responseCode = "403",
        description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable"
      )
    }
  )
  public void runHistoryPurge() throws Exception {
    checkForbidden(
      configuration.isAllowTestResourceCalls(),
      "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)"
    );
    historyPurger.runActionOnPoll();
  }

  @POST
  @Path("/purge-history/request")
  @Operation(
    summary = "Run a request history purge",
    responses = {
      @ApiResponse(
        responseCode = "403",
        description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable"
      )
    }
  )
  public void runRequestHistoryPurge() throws Exception {
    checkForbidden(
      configuration.isAllowTestResourceCalls(),
      "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)"
    );
    historyManager.purgeRequestHistory();
  }

  @POST
  @Path("/purge-history/deploy")
  @Operation(
    summary = "Run a request history purge",
    responses = {
      @ApiResponse(
        responseCode = "403",
        description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable"
      )
    }
  )
  public void runDeployHistoryPurge() throws Exception {
    checkForbidden(
      configuration.isAllowTestResourceCalls(),
      "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)"
    );
    historyManager.purgeDeployHistory();
  }

  @POST
  @Path("/reconnect-mesos")
  @Operation(
    summary = "Trigger a reconnect to the mesos master",
    responses = {
      @ApiResponse(
        responseCode = "403",
        description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable"
      )
    }
  )
  public void reconnectMesos() {
    checkForbidden(
      configuration.isAllowTestResourceCalls(),
      "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)"
    );
    scheduler.reconnectMesos();
  }

  @POST
  @Path("/persist-task-history")
  @Operation(
    summary = "Trigger a task history persister run",
    responses = {
      @ApiResponse(
        responseCode = "403",
        description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable"
      )
    }
  )
  public void persistTaskHistory() {
    checkForbidden(
      configuration.isAllowTestResourceCalls(),
      "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)"
    );
    CompletableFuture.runAsync(taskHistoryPersister::runActionOnPoll);
  }

  @POST
  @Path("/persist-deploy-history")
  @Operation(
    summary = "Trigger a deploy history persister run",
    responses = {
      @ApiResponse(
        responseCode = "403",
        description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable"
      )
    }
  )
  public void persistDeployHistory() {
    checkForbidden(
      configuration.isAllowTestResourceCalls(),
      "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)"
    );
    CompletableFuture.runAsync(deployHistoryPersister::runActionOnPoll);
  }

  @POST
  @Path("/persist-request-history")
  @Operation(
    summary = "Trigger a request history persister run",
    responses = {
      @ApiResponse(
        responseCode = "403",
        description = "Test resource calls are currently not enabled, set `allowTestResourceCalls` to `true` in config yaml to enable"
      )
    }
  )
  public void persistRequestHistory() {
    checkForbidden(
      configuration.isAllowTestResourceCalls(),
      "Test resource calls are disabled (set isAllowTestResourceCalls to true in configuration)"
    );
    CompletableFuture.runAsync(requestHistoryPersister::runActionOnPoll);
  }
}
