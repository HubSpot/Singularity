package com.hubspot.singularity.proxy;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosTaskStatisticsObject;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityShellCommand;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityTaskShellCommandRequest;
import com.hubspot.singularity.api.SingularityKillTaskRequest;
import com.hubspot.singularity.api.SingularityTaskMetadataRequest;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.TEST_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class TaskResource extends ProxyResource {

  @Inject
  public TaskResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    super(configuration, httpClient, objectMapper);
  }

  @GET
  @Path("/scheduled")
  public List<SingularityTaskRequest> getScheduledTasks(@QueryParam("useWebCache") Boolean useWebCache) {

  }

  @GET
  @Path("/scheduled/ids")
  public Iterable<SingularityPendingTaskId> getScheduledTaskIds(@QueryParam("useWebCache") Boolean useWebCache) {

  }

  @GET
  @Path("/scheduled/task/{pendingTaskId}")
  public SingularityTaskRequest getPendingTask(@PathParam("pendingTaskId") String pendingTaskIdStr) {

  }

  @GET
  @Path("/scheduled/request/{requestId}")
  public List<SingularityTaskRequest> getScheduledTasksForRequest(@PathParam("requestId") String requestId, @QueryParam("useWebCache") Boolean useWebCache) {

  }

  @GET
  @Path("/active/slave/{slaveId}")
  public Iterable<SingularityTask> getTasksForSlave(@PathParam("slaveId") String slaveId, @QueryParam("useWebCache") Boolean useWebCache) {

  }

  @GET
  @Path("/active")
  public Iterable<SingularityTask> getActiveTasks(@QueryParam("useWebCache") Boolean useWebCache) {

  }

  @GET
  @Path("/cleaning")
  public Iterable<SingularityTaskCleanup> getCleaningTasks(@QueryParam("useWebCache") Boolean useWebCache) {

  }

  @GET
  @Path("/killed")
  public Iterable<SingularityKilledTaskIdRecord> getKilledTasks() {

  }

  @GET
  @Path("/lbcleanup")
  public Iterable<SingularityTaskId> getLbCleanupTasks() {

  }

  @GET
  @Path("/task/{taskId}")
  public SingularityTask getActiveTask(@PathParam("taskId") String taskId) {

  }

  @GET
  @Path("/task/{taskId}/statistics")
  public MesosTaskStatisticsObject getTaskStatistics(@PathParam("taskId") String taskId) {

  }

  @GET
  @Path("/task/{taskId}/cleanup")
  public Optional<SingularityTaskCleanup> getTaskCleanup(@PathParam("taskId") String taskId) {

  }

  @DELETE
  @Path("/task/{taskId}")
  public SingularityTaskCleanup killTask(@PathParam("taskId") String taskId, @Context HttpServletRequest requestContext) {

  }

  @DELETE
  @Path("/task/{taskId}")
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityTaskCleanup killTask(@PathParam("taskId") String taskId,
                                         @Context HttpServletRequest requestContext,
                                         SingularityKillTaskRequest killTaskRequest) {

  }

  @Path("/commands/queued")
  public List<SingularityTaskShellCommandRequest> getQueuedShellCommands() {

  }

  @POST
  @Path("/task/{taskId}/metadata")
  @Consumes({ MediaType.APPLICATION_JSON })
  public void postTaskMetadata(@PathParam("taskId") String taskId, final SingularityTaskMetadataRequest taskMetadataRequest) {

  }

  @POST
  @Path("/task/{taskId}/command")
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityTaskShellCommandRequest runShellCommand(@PathParam("taskId") String taskId, final SingularityShellCommand shellCommand) {

  }
}
