package com.hubspot.singularity.proxy;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityTaskCurrentUsageWithId;
import com.hubspot.singularity.SingularityTaskUsage;

@Path("/api/usage")
public class UsageResource {

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
