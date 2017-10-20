package com.hubspot.singularity.resources;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.ui.SingularityDashboard;
import com.hubspot.singularity.ui.SingularityRequestDetail;
import com.hubspot.singularity.ui.SingularityTaskDetail;
import com.wordnik.swagger.annotations.Api;

@Path(ApiPaths.UI_DATA_RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Api(description = "Aggregations of data specific to UI pages", value=ApiPaths.UI_DATA_RESOURCE_PATH)
public class UIDataResource {
  private final Optional<SingularityUser> user;

  @Inject
  public UIDataResource(Optional<SingularityUser> user) {
    this.user = user;
  }

  @Path("/dashboard")
  public SingularityDashboard getDashboardData() {
    return null;
  }

  @Path("/request/{requestId}")
  public SingularityRequestDetail getRequestDetailData(@PathParam("requestId") String requestId) {
    return null;
  }

  @Path("/task/{taskId}")
  public SingularityTaskDetail getTaskDetailData(@PathParam("taskId") String taskId) {
    SingularityTaskId singularityTaskId = SingularityTaskId.valueOf(taskId);
    return null;
  }
}
