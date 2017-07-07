package com.hubspot.singularity.proxy;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.SingularityTaskReconciliationStatistics;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.STATE_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class StateResource extends ProxyResource {

  @Inject
  public StateResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    super(configuration, httpClient, objectMapper);
  }

  @GET
  public SingularityState getState(@QueryParam("skipCache") boolean skipCache, @QueryParam("includeRequestIds") boolean includeRequestIds) {
    throw new NotImplemenedException();
  }

  @GET
  @Path("/requests/under-provisioned")
  public List<String> getUnderProvisionedRequestIds(@QueryParam("skipCache") boolean skipCache) {
    throw new NotImplemenedException();
  }

  @GET
  @Path("/requests/over-provisioned")
  public List<String> getOverProvisionedRequestIds(@QueryParam("skipCache") boolean skipCache) {
    throw new NotImplemenedException();
  }

  @GET
  @Path("/task-reconciliation")
  public Optional<SingularityTaskReconciliationStatistics> getTaskReconciliationStatistics() {
    throw new NotImplemenedException();
  }
}
