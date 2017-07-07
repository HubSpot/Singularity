package com.hubspot.singularity.proxy;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPriorityFreezeParent;
import com.hubspot.singularity.api.SingularityPriorityFreeze;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.PRIORITY_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class PriorityResource extends ProxyResource {

  @Inject
  public PriorityResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    super(configuration, httpClient, objectMapper);
  }

  @GET
  @Path("/freeze")
  public Optional<SingularityPriorityFreezeParent> getActivePriorityFreeze() {

  }

  @DELETE
  @Path("/freeze")
  public void deleteActivePriorityFreeze() {

  }

  @POST
  @Path("/freeze")
  public SingularityPriorityFreezeParent createPriorityFreeze(SingularityPriorityFreeze priorityFreezeRequest) {

  }
}
