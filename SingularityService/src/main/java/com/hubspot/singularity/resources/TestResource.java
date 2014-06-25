package com.hubspot.singularity.resources;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.TaskStatus;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityDriverManager;
import com.hubspot.singularity.SingularityLeaderController;
import com.hubspot.singularity.SingularityService;

@Path(SingularityService.API_BASE_PATH + "/test")
public class TestResource extends BaseResource {

  private final SingularityAbort abort;
  private final SingularityLeaderController managed;
  private final SingularityDriverManager driverManager;
  
  @Inject
  public TestResource(SingularityLeaderController managed, SingularityAbort abort, SingularityDriverManager driverManager) {
    this.managed = managed;
    this.abort = abort;
    this.driverManager = driverManager;
  }
  
  @POST
  @Path("/scheduler/statusUpdate/{taskId}/{taskState}")
  public void statusUpdate(@PathParam("taskId") String taskId, @PathParam("taskState") String taskState) {
    driverManager.getDriver().getScheduler().statusUpdate(null, TaskStatus.newBuilder()
        .setTaskId(TaskID.newBuilder().setValue(taskId))
        .setState(TaskState.valueOf(taskState))
        .build());
  }
  
  @POST
  @Path("/leader")
  public void setLeader() {
    managed.isLeader();
  }
  
  @POST
  @Path("/notleader")
  public void setNotLeader() {
    managed.notLeader();
  }
 
  @POST
  @Path("/stop")
  public void stop() throws Exception {
    managed.stop();
  }
  
  @POST
  @Path("/abort")
  public void abort() {
    abort.abort();
  }
  
  @POST
  @Path("/start")
  public void start() throws Exception {
    managed.start();
  }
  
}
