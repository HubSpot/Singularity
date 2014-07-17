package com.hubspot.singularity.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
  public SingularityState getState() {
    return stateManager.getState();
  }
    
}
