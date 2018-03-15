package com.hubspot.singularity.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.api.auth.SingularityAuthorizationScope;
import com.hubspot.singularity.api.auth.SingularityUser;
import com.hubspot.singularity.api.machines.SingularityClusterUtilization;
import com.hubspot.singularity.api.machines.SingularitySlave;
import com.hubspot.singularity.api.machines.SingularitySlaveUsage;
import com.hubspot.singularity.api.machines.SingularitySlaveUsageWithId;
import com.hubspot.singularity.api.task.SingularityTask;
import com.hubspot.singularity.api.task.SingularityTaskCurrentUsageWithId;
import com.hubspot.singularity.api.task.SingularityTaskId;
import com.hubspot.singularity.api.task.SingularityTaskUsage;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.UsageManager;

import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Path(ApiPaths.USAGE_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Schema(title = "Retrieve usage data about slaves and tasks")
@Tags({@Tag(name = "Resource Usage")})
public class UsageResource {
  private final UsageManager usageManager;
  private final TaskManager taskManager;
  private final SlaveManager slaveManager;
  private final SingularityAuthorizationHelper authorizationHelper;

  @Inject
  public UsageResource(UsageManager usageManager, TaskManager taskManager, SlaveManager slaveManager, SingularityAuthorizationHelper authorizationHelper) {
    this.usageManager = usageManager;
    this.taskManager = taskManager;
    this.slaveManager = slaveManager;
    this.authorizationHelper = authorizationHelper;
  }

  @GET
  @Path("/slaves")
  @Operation(summary = "Retrieve a list of slave resource usage models with slave ids")
  public List<SingularitySlaveUsageWithId> getSlavesWithUsage(@Parameter(hidden = true) @Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    return usageManager.getAllCurrentSlaveUsage();
  }

  @GET
  @Path("/slaves/{slaveId}/tasks/current")
  @Operation(summary = "Retrieve a list of resource usages for active tasks on a particular slave")
  public List<SingularityTaskCurrentUsageWithId> getSlaveCurrentTaskUsage(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The slave to retrieve task usages for") @PathParam("slaveId") String slaveId) {
    authorizationHelper.checkAdminAuthorization(user);
    Optional<SingularitySlave> slave = slaveManager.getObject(slaveId);

    WebExceptions.checkNotFound(slave.isPresent(), "No slave found with id %s", slaveId);

    List<SingularityTask> tasksOnSlave = taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slave.get());

    List<SingularityTaskId> taskIds = new ArrayList<>(tasksOnSlave.size());
    for (SingularityTask task : tasksOnSlave) {
      taskIds.add(task.getTaskId());
    }

    return usageManager.getTaskCurrentUsages(taskIds);
  }

  @GET
  @Path("/slaves/{slaveId}/history")
  @Operation(summary = "Retrieve the usage history for a particular slave")
  public List<SingularitySlaveUsage> getSlaveUsageHistory(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The slave to retrieve usage history for") @PathParam("slaveId") String slaveId) {
    authorizationHelper.checkAdminAuthorization(user);
    return usageManager.getSlaveUsage(slaveId);
  }

  @GET
  @Path("/tasks/{taskId}/history")
  @Operation(
      summary = "Retrieve the usage history for a particular task",
      description = "Empty if the task usage has not been collected or has been cleaned up"
  )
  public List<SingularityTaskUsage> getTaskUsageHistory(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The id of the task to retrieve usage history for") @PathParam("taskId") String taskId) {
    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.READ);
    return usageManager.getTaskUsage(taskId);
  }

  @GET
  @Path("/cluster/utilization")
  @Operation(summary = "GET a summary of utilization for all slaves and requests in the mesos cluster")
  public SingularityClusterUtilization getClusterUtilization(@Parameter(hidden = true) @Auth SingularityUser user) {
    //authorizationHelper.checkAdminAuthorization(user); Needed for ui pages outside single request
    WebExceptions.checkNotFound(usageManager.getClusterUtilization().isPresent(), "No cluster utilization has been saved yet");

    return usageManager.getClusterUtilization().get();
  }
}
