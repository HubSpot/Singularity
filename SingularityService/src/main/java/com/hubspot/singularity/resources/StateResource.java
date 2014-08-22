package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.data.StateManager;

@Path(SingularityService.API_BASE_PATH + "/state")
@Produces({ MediaType.APPLICATION_JSON })
public class StateResource {

  private final StateManager stateManager;

  @Inject
  public StateResource(StateManager stateManager) {
    this.stateManager = stateManager;
  }

  @GET
  public SingularityState getState(@QueryParam("skipCache") boolean skipCache, @QueryParam("includeRequestIds") boolean includeRequestIds) {
    return stateManager.getState(skipCache, includeRequestIds);
  }

  @GET
  @Path("/requests/under-provisioned")
  public List<String> getUnderProvisionedTaskIds(@QueryParam("skipCache") boolean skipCache) {
    return stateManager.getState(skipCache, true).getUnderProvisionedRequestIds();
  }

  @GET
  @Path("/requests/over-provisioned")
  public List<String> getOverProvisionedTaskIds(@QueryParam("skipCache") boolean skipCache) {
    return stateManager.getState(skipCache, true).getOverProvisionedRequestIds();
  }

}
