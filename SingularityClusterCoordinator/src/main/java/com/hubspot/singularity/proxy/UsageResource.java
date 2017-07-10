package com.hubspot.singularity.proxy;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityTaskCurrentUsageWithId;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path("/api/usage")
public class UsageResource extends ProxyResource {

  @Inject
  public UsageResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper, DataCenterLocator dataCenterLocator) {
    super(configuration, httpClient, objectMapper, dataCenterLocator);
  }

  @GET
  @Path("/slaves")
  public List<SingularitySlaveUsageWithId> getSlavesWithUsage(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.SLAVE_USAGE_WITH_ID_LIST_REF);
  }

  @GET
  @Path("/slaves/{slaveId}/tasks/current")
  public List<SingularityTaskCurrentUsageWithId> getSlaveCurrentTaskUsage(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId) {
    return routeBySlaveId(request, slaveId, TypeRefs.SLAVE_TASK_USAGE_WITH_ID_LIST_REF);
  }

  @GET
  @Path("/slaves/{slaveId}/history")
  public List<SingularitySlaveUsage> getSlaveUsageHistory(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId) {
    return routeBySlaveId(request, slaveId, TypeRefs.SLAVE_USAGE_LIST_REF);
  }

  @GET
  @Path("/tasks/{taskId}/history")
  public List<SingularityTaskUsage> getTaskUsageHistory(@Context HttpServletRequest request, @PathParam("taskId") String taskId) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId(), TypeRefs.TASK_USAGE_LIST_REF);
  }

}
