package com.hubspot.singularity.proxy;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.INACTIVE_SLAVES_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class InactiveSlaveResource extends ProxyResource {

  @Inject
  public InactiveSlaveResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper, DataCenterLocator dataCenterLocator) {
    super(configuration, httpClient, objectMapper, dataCenterLocator);
  }

  @GET
  public List<String> getInactiveSlaves(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.LIST_STRING_REF);
  }

  @POST
  public Response deactivateSlave(@Context HttpServletRequest request, @QueryParam("host") String host) {
    return routeByHostname(request, host, TypeRefs.RESPONSE_REF);
  }

  @DELETE
  public Response reactivateSlave(@Context HttpServletRequest request, @QueryParam("host") String host) {
    return routeByHostname(request, host, TypeRefs.RESPONSE_REF);
  }
}
