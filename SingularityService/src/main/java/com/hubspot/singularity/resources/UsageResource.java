package com.hubspot.singularity.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityClusterUtilization;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.usage.UsageManager;

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
  private final SingularityAuthorizationHelper authorizationHelper;

  @Inject
  public UsageResource(UsageManager usageManager, SingularityAuthorizationHelper authorizationHelper) {
    this.usageManager = usageManager;
    this.authorizationHelper = authorizationHelper;
  }

  @GET
  @Path("/slaves")
  @Operation(summary = "Retrieve a list of slave resource usage models with slave ids")
  public Collection<SingularitySlaveUsageWithId> getSlavesWithUsage(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Skip the cache and read data directly from zookeeper") @QueryParam("skipCache") @DefaultValue("false") boolean skipCache) {
    authorizationHelper.checkAdminAuthorization(user);
    return usageManager.getAllCurrentSlaveUsage(skipCache).values();
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
    return usageManager.getTaskUsage(SingularityTaskId.valueOf(taskId));
  }

  @GET
  @Path("/cluster/utilization")
  @Operation(summary = "GET a summary of utilization for all slaves and requests in the mesos cluster")
  public SingularityClusterUtilization getClusterUtilization(@Parameter(hidden = true) @Auth SingularityUser user) {
    authorizationHelper.checkReadAuthorization(user);
    WebExceptions.checkNotFound(usageManager.getClusterUtilization().isPresent(), "No cluster utilization has been saved yet");
    return usageManager.getClusterUtilization().get();
  }

  @GET
  @Path("/requests")
  @Operation(
      summary = "Retrieve the usage summaries for all requests"
  )
  public List<RequestUtilization> getRequestUtilizations(
      @Auth SingularityUser user,
      @Parameter(description = "Skip the cache and read data directly from zookeeper") @QueryParam("skipCache") @DefaultValue("false") boolean skipCache) {
    return new ArrayList<>(usageManager.getRequestUtilizations(skipCache).values());
  }

  @GET
  @Path("/requests/request/{requestId}")
  @Operation(
      summary = "Retrieve the usage summary for a single request"
  )
  public Optional<RequestUtilization> getRequestUtilization(
      @Auth SingularityUser user,
      @Parameter(description = "The request to fetch usage data for") @PathParam("requestId") String requestId,
      @Parameter(description = "Skip the cache and read data directly from zookeeper") @QueryParam("skipCache") @DefaultValue("false") boolean skipCache) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, SingularityAuthorizationScope.READ);
    return usageManager.getRequestUtilization(requestId, skipCache);
  }

}
