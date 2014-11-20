package com.hubspot.singularity.resources;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.TaskStatus;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.SingularityLeaderController;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityDriver;

@Path(TestResource.PATH)
@Api(description="Misc testing endpoints.", value=TestResource.PATH)
public class TestResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/test";

  private final SingularityAbort abort;
  private final SingularityLeaderController managed;
  private final SingularityConfiguration configuration;
  private final SingularityDriver driver;

  @Inject
  public TestResource(SingularityConfiguration configuration, SingularityLeaderController managed, SingularityAbort abort, final SingularityDriver driver) {
    this.configuration = configuration;
    this.managed = managed;
    this.abort = abort;
    this.driver = driver;
  }

  private void checkAllowed() {
    if (!configuration.allowTestResourceCalls()) {
      throw WebExceptions.webException(403, "Test resource calls are disabled (set allowTestResourceCalls to true in configuration)");
    }
  }

  @POST
  @Path("/scheduler/statusUpdate/{taskId}/{taskState}")
  @ApiOperation("Force an update for a specific task.")
  public void statusUpdate(@PathParam("taskId") String taskId, @PathParam("taskState") String taskState) {
    checkAllowed();

    driver.getScheduler().statusUpdate(null, TaskStatus.newBuilder()
        .setTaskId(TaskID.newBuilder().setValue(taskId))
        .setState(TaskState.valueOf(taskState))
        .build());
  }

  @POST
  @Path("/leader")
  @ApiOperation("Make this instance of Singularity believe it's elected leader.")
  public void setLeader() {
    checkAllowed();

    managed.isLeader();
  }

  @POST
  @Path("/notleader")
  @ApiOperation("Make this instanceo of Singularity believe it's lost leadership.")
  public void setNotLeader() {
    checkAllowed();

    managed.notLeader();
  }

  @POST
  @Path("/stop")
  @ApiOperation("Stop the Mesos scheduler driver.")
  public void stop() throws Exception {
    checkAllowed();

    managed.stop();
  }

  @POST
  @Path("/abort")
  @ApiOperation("Abort the Mesos scheduler driver.")
  public void abort() {
    checkAllowed();

    abort.abort(AbortReason.TEST_ABORT);
  }

  @POST
  @Path("/start")
  @ApiOperation("Start the Mesos scheduler driver.")
  public void start() throws Exception {
    checkAllowed();

    managed.start();
  }

}
