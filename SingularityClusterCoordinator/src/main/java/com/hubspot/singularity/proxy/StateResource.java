package com.hubspot.singularity.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.exceptions.NotImplemenedException;

@Path(ApiPaths.STATE_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class StateResource extends ProxyResource {

  @Inject
  public StateResource() {}

  @GET
  public SingularityState getState(@Context HttpServletRequest request) {
    // TODO - merge this result?
    throw new NotImplemenedException();
  }

  @GET
  @Path("/requests/under-provisioned")
  public Response getUnderProvisionedRequestIds(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/requests/over-provisioned")
  public Response getOverProvisionedRequestIds(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/task-reconciliation")
  public Response getTaskReconciliationStatistics(@Context HttpServletRequest request) {
    // TODO - merge this result?
    throw new NotImplemenedException();
  }
}
