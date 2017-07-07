package com.hubspot.singularity.proxy;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityUserHolder;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.AUTH_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class AuthResource extends ProxyResource {

  @Inject
  public AuthResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    super(configuration, httpClient, objectMapper);
  }

  @GET
  @Path("/user")
  public SingularityUserHolder getUser() {
    throw new RuntimeException("not implemented");
  }

  @GET
  @Path("/{requestId}/auth-check/{userId}")
  public Response checkReadOnlyAuth(@PathParam("requestId") String requestId, @PathParam("userId") String userId, @QueryParam("scope") Optional<SingularityAuthorizationScope> scope) {
    throw new RuntimeException("not implemented");
  }
}
