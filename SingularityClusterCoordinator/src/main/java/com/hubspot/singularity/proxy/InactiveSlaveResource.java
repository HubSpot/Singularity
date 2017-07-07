package com.hubspot.singularity.proxy;

import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.INACTIVE_SLAVES_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class InactiveSlaveResource extends ProxyResource {

  @Inject
  public InactiveSlaveResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    super(configuration, httpClient, objectMapper);
  }

  @GET
  public Set<String> getInactiveSlaves() {

  }

  @POST
  public void deactivateSlave(@QueryParam("host") String host) {


  }

  @DELETE
  public void reactivateSlave(@QueryParam("host") String host) {


  }
}
