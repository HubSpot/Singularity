package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.SingularityTaskReconciliationStatistics;
import com.hubspot.singularity.data.StateManager;
import com.hubspot.singularity.views.IndexView;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path(StateResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Provides information about the current state of Singularity.", value=StateResource.PATH)
public class StateResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/state";

  private final StateManager stateManager;
  private final IndexView indexView;

  @Inject
  public StateResource(StateManager stateManager, IndexView indexView) {
    this.stateManager = stateManager;
    this.indexView = indexView;
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

  @GET
  @Path("/ui-configuration")
  @ApiOperation("Retrieve information about the deployed UI configuration")
  public IndexView getUIConfiguration() {
    return this.indexView;
  }
}
