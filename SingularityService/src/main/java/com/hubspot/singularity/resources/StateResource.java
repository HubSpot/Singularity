package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.SingularityTaskReconciliationStatistics;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.StateManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path(ApiPaths.STATE_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Provides information about the current state of Singularity.", value=ApiPaths.STATE_RESOURCE_PATH)
public class StateResource {
  private final StateManager stateManager;

  @Inject
  public StateResource(StateManager stateManager) {
    this.stateManager = stateManager;
  }

  @GET
  @ApiOperation("Retrieve information about the current state of Singularity.")
  public SingularityState getState(@QueryParam("skipCache") boolean skipCache, @QueryParam("includeRequestIds") boolean includeRequestIds) {
    return stateManager.getState(skipCache, includeRequestIds);
  }

  @GET
  @Path("/requests/under-provisioned")
  @ApiOperation("Retrieve the list of under-provisioned request IDs.")
  public List<String> getUnderProvisionedRequestIds(@QueryParam("skipCache") boolean skipCache) {
    return stateManager.getState(skipCache, true).getUnderProvisionedRequestIds();
  }

  @GET
  @Path("/requests/over-provisioned")
  @ApiOperation("Retrieve the list of over-provisioned request IDs.")
  public List<String> getOverProvisionedRequestIds(@QueryParam("skipCache") boolean skipCache) {
    return stateManager.getState(skipCache, true).getOverProvisionedRequestIds();
  }

  @GET
  @Path("/task-reconciliation")
  @ApiOperation("Retrieve information about the most recent task reconciliation")
  public Optional<SingularityTaskReconciliationStatistics> getTaskReconciliationStatistics() {
    return stateManager.getTaskReconciliationStatistics();
  }
}
