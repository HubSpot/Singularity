package com.hubspot.singularity.proxy;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequestGroup;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.REQUEST_GROUP_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class RequestGroupResource extends ProxyResource {

  @Inject
  public RequestGroupResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper, DataCenterLocator dataCenterLocator) {
    super(configuration, httpClient, objectMapper, dataCenterLocator);
  }

  @GET
  public List<SingularityRequestGroup> getRequestGroupIds(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.REQUEST_GROUP_LIST_REF);
  }

  @GET
  @Path("/group/{requestGroupId}")
  public Optional<SingularityRequestGroup> getRequestGroup(@Context HttpServletRequest request, @PathParam("requestGroupId") String requestGroupId) {
    return routeByRequestGroupId(request, requestGroupId, TypeRefs.OPTIONAL_REQUEST_GROUP_REF);
  }

  @DELETE
  @Path("/group/{requestGroupId}")
  public Response deleteRequestGroup(@Context HttpServletRequest request, @PathParam("requestGroupId") String requestGroupId) {
    return routeByRequestGroupId(request, requestGroupId, TypeRefs.RESPONSE_REF);
  }

  @POST
  public SingularityRequestGroup saveRequestGroup(@Context HttpServletRequest request, SingularityRequestGroup requestGroup) {
    // TODO - route by more than first request id?
    // TODO - add to internal list of groups?
    // TODO - error if list is empty?
    return routeByRequestId(request, requestGroup.getRequestIds().get(0), TypeRefs.REQUEST_GROUP_REF);
  }
}
