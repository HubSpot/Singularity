package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.data.StateManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path(StateResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Provides information about the current state of Singularity.", value=StateResource.PATH)
public class StateResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/state";

  private final StateManager stateManager;

  @Inject
  public StateResource(StateManager stateManager) {
    this.stateManager = stateManager;
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation("Retrieve information about the current state of Singularity.")
  public SingularityState getState(@QueryParam("skipCache") boolean skipCache, @QueryParam("includeRequestIds") boolean includeRequestIds) {
    return stateManager.getState(skipCache, includeRequestIds);
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/requests/under-provisioned")
  @ApiOperation("Retrieve the list of under-provisioned request IDs.")
  public List<String> getUnderProvisionedRequestIds(@QueryParam("skipCache") boolean skipCache) {
    return stateManager.getState(skipCache, true).getUnderProvisionedRequestIds();
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/requests/over-provisioned")
  @ApiOperation("Retrieve the list of over-provisioned request IDs.")
  public List<String> getOverProvisionedRequestIds(@QueryParam("skipCache") boolean skipCache) {
    return stateManager.getState(skipCache, true).getOverProvisionedRequestIds();
  }

}
