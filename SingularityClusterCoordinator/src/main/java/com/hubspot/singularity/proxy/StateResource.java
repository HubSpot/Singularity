package com.hubspot.singularity.proxy;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.SingularityTaskReconciliationStatistics;
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
  public List<String> getUnderProvisionedRequestIds(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.LIST_STRING_REF);
  }

  @GET
  @Path("/requests/over-provisioned")
  public List<String> getOverProvisionedRequestIds(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.LIST_STRING_REF);
  }

  @GET
  @Path("/task-reconciliation")
  public Optional<SingularityTaskReconciliationStatistics> getTaskReconciliationStatistics(@Context HttpServletRequest request) {
    // TODO - merge this result?
    throw new NotImplemenedException();
  }
}
