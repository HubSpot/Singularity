package com.hubspot.singularity.proxy;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityDisabledAction;
import com.hubspot.singularity.SingularityDisasterType;
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityTaskCredits;
import com.hubspot.singularity.api.SingularityDisabledActionRequest;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.DISASTERS_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class DisastersResource extends ProxyResource {

  @Inject
  public DisastersResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    super(configuration, httpClient, objectMapper);
  }

  @GET
  @Path("/stats")
  public SingularityDisastersData disasterStats() {
    throw new RuntimeException("not implemented");
  }

  @GET
  @Path("/active")
  public List<SingularityDisasterType> activeDisasters() {
    throw new RuntimeException("not implemented");
  }

  @POST
  @Path("/disable")
  public void disableAutomatedDisasterCreation() {
    throw new RuntimeException("not implemented");
  }

  @POST
  @Path("/enable")
  public void enableAutomatedDisasterCreation() {
    throw new RuntimeException("not implemented");
  }

  @DELETE
  @Path("/active/{type}")
  public void removeDisaster(@PathParam("type") SingularityDisasterType type) {
    throw new RuntimeException("not implemented");
  }

  @POST
  @Path("/active/{type}")
  public void newDisaster(@PathParam("type") SingularityDisasterType type) {
    throw new RuntimeException("not implemented");
  }

  @GET
  @Path("/disabled-actions")
  public List<SingularityDisabledAction> disabledActions() {
    throw new RuntimeException("not implemented");
  }

  @POST
  @Path("/disabled-actions/{action}")
  public void disableAction(@PathParam("action") SingularityAction action, SingularityDisabledActionRequest disabledActionRequest) {
    throw new RuntimeException("not implemented");
  }

  @DELETE
  @Path("/disabled-actions/{action}")
  public void enableAction(@PathParam("action") SingularityAction action) {
    throw new RuntimeException("not implemented");
  }

  @POST
  @Path("/task-credits")
  public void addTaskCredits(@QueryParam("credits") Optional<Integer> credits) throws Exception {
    throw new RuntimeException("not implemented");
  }

  @DELETE
  @Path("/task-credits")
  public void disableTaskCredits(@Context HttpServletRequest request) throws Exception {
    throw new RuntimeException("not implemented");
  }

  @GET
  @Path("/task-credits")
  public SingularityTaskCredits getTaskCreditData() throws Exception {
    throw new RuntimeException("not implemented");
  }
}
