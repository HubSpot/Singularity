package com.hubspot.singularity.resources;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityClusterUtilization;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCurrentUsageWithId;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.UsageManager;
import com.wordnik.swagger.annotations.Api;

import io.dropwizard.auth.Auth;

@Path(ApiPaths.USAGE_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Provides usage data about slaves and tasks", value=ApiPaths.USAGE_RESOURCE_PATH)
public class UsageResource {

  public static final String PATH = SingularityService.API_BASE_PATH + "/usage";

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
  public List<SingularitySlaveUsageWithId> getSlavesWithUsage(@Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    return usageManager.getAllCurrentSlaveUsage();
  }

  @GET
  @Path("/slaves/{slaveId}/tasks/current")
  public List<SingularityTaskCurrentUsageWithId> getSlaveCurrentTaskUsage(@Auth SingularityUser user, @PathParam("slaveId") String slaveId) {
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
  public List<SingularitySlaveUsage> getSlaveUsageHistory(@Auth SingularityUser user, @PathParam("slaveId") String slaveId) {
    authorizationHelper.checkAdminAuthorization(user);
    return usageManager.getSlaveUsage(slaveId);
  }

  @GET
  @Path("/tasks/{taskId}/history")
  public List<SingularityTaskUsage> getTaskUsageHistory(@Auth SingularityUser user, @PathParam("taskId") String taskId) {
    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.READ);
    return usageManager.getTaskUsage(taskId);
  }

  @GET
  @Path("/cluster/utilization")
  public SingularityClusterUtilization getClusterUtilization(@Auth SingularityUser user) {
    //authorizationHelper.checkAdminAuthorization(user); Needed for ui pages outside single request
    WebExceptions.checkNotFound(usageManager.getClusterUtilization().isPresent(), "No cluster utilization has been saved yet");

    return usageManager.getClusterUtilization().get();
  }
}
