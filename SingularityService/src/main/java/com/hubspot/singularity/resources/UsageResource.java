package com.hubspot.singularity.resources;

import com.google.inject.Inject;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityAgentUsageWithId;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityClusterUtilization;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.usage.UsageManager;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path(ApiPaths.USAGE_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Schema(title = "Retrieve usage data about slaves and tasks")
@Tags({ @Tag(name = "Resource Usage") })
public class UsageResource {
  private final UsageManager usageManager;
  private final SingularityAuthorizer authorizationHelper;

  @Inject
  public UsageResource(
    UsageManager usageManager,
    SingularityAuthorizer authorizationHelper
  ) {
    this.usageManager = usageManager;
    this.authorizationHelper = authorizationHelper;
  }

  @GET
  @Path("/slaves")
  @Operation(summary = "Retrieve a list of agent resource usage models with agent ids")
  @Deprecated
  public Collection<SingularityAgentUsageWithId> getAgentsWithUsageDeprecated(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    return getAgentsWithUsage(user);
  }

  @GET
  @Path("/agents")
  @Operation(summary = "Retrieve a list of agent resource usage models with agent ids")
  public Collection<SingularityAgentUsageWithId> getAgentsWithUsage(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return usageManager.getAllCurrentAgentUsage().values();
  }

  @GET
  @Path("/tasks/{taskId}/history")
  @Operation(
    summary = "Retrieve the usage history for a particular task",
    description = "Empty if the task usage has not been collected or has been cleaned up"
  )
  public List<SingularityTaskUsage> getTaskUsageHistory(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      required = true,
      description = "The id of the task to retrieve usage history for"
    ) @PathParam("taskId") String taskId
  ) {
    authorizationHelper.checkForAuthorizationByTaskId(
      taskId,
      user,
      SingularityAuthorizationScope.READ
    );
    return usageManager.getTaskUsage(SingularityTaskId.valueOf(taskId));
  }

  @GET
  @Path("/cluster/utilization")
  @Operation(
    summary = "GET a summary of utilization for all slaves and requests in the mesos cluster"
  )
  public SingularityClusterUtilization getClusterUtilization(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    //authorizationHelper.checkAdminAuthorization(user); Needed for ui pages outside single request
    WebExceptions.checkNotFound(
      usageManager.getClusterUtilization().isPresent(),
      "No cluster utilization has been saved yet"
    );

    return usageManager.getClusterUtilization().get();
  }

  @GET
  @Path("/requests")
  public List<RequestUtilization> getRequestUtilizations(
    @Auth SingularityUser user,
    @QueryParam("useWebCache") Boolean useWebCache
  ) {
    return new ArrayList<>(
      usageManager.getRequestUtilizations(useWebCache != null && useWebCache).values()
    );
  }

  @GET
  @Path("/requests/request/{requestId}")
  public Optional<RequestUtilization> getRequestUtilization(
    @Auth SingularityUser user,
    @PathParam("requestId") String requestId,
    @QueryParam("useWebCache") Boolean useWebCache
  ) {
    authorizationHelper.checkForAuthorizationByRequestId(
      requestId,
      user,
      SingularityAuthorizationScope.READ
    );
    return usageManager.getRequestUtilization(
      requestId,
      useWebCache != null && useWebCache
    );
  }
}
