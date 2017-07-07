package com.hubspot.singularity.proxy;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityTaskCurrentUsageWithId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path("/api/usage")
public class UsageResource extends ProxyResource {

  @Inject
  public UsageResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    super(configuration, httpClient, objectMapper);
  }

  @GET
  @Path("/slaves")
  public List<SingularitySlaveUsageWithId> getSlavesWithUsage() {

  }

  @GET
  @Path("/slaves/{slaveId}/tasks/current")
  public List<SingularityTaskCurrentUsageWithId> getSlaveCurrentTaskUsage(@PathParam("slaveId") String slaveId) {

  }

  @GET
  @Path("/slaves/{slaveId}/history")
  public List<SingularitySlaveUsage> getSlaveUsageHistory(@PathParam("slaveId") String slaveId) {

  }

  @GET
  @Path("/tasks/{taskId}/history")
  public List<SingularityTaskUsage> getTaskUsageHistory(@PathParam("taskId") String taskId) {

  }

}
