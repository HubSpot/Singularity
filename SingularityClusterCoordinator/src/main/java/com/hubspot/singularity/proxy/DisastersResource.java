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

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityDisabledAction;
import com.hubspot.singularity.SingularityDisasterType;
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityTaskCredits;
import com.hubspot.singularity.api.SingularityDisabledActionRequest;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.DISASTERS_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class DisastersResource {

  @GET
  @Path("/stats")
  public SingularityDisastersData disasterStats() {

  }

  @GET
  @Path("/active")
  public List<SingularityDisasterType> activeDisasters() {

  }

  @POST
  @Path("/disable")
  public void disableAutomatedDisasterCreation() {

  }

  @POST
  @Path("/enable")
  public void enableAutomatedDisasterCreation() {

  }

  @DELETE
  @Path("/active/{type}")
  public void removeDisaster(@PathParam("type") SingularityDisasterType type) {

  }

  @POST
  @Path("/active/{type}")
  public void newDisaster(@PathParam("type") SingularityDisasterType type) {

  }

  @GET
  @Path("/disabled-actions")
  public List<SingularityDisabledAction> disabledActions() {

  }

  @POST
  @Path("/disabled-actions/{action}")
  public void disableAction(@PathParam("action") SingularityAction action, SingularityDisabledActionRequest disabledActionRequest) {

  }

  @DELETE
  @Path("/disabled-actions/{action}")
  public void enableAction(@PathParam("action") SingularityAction action) {

  }

  @POST
  @Path("/task-credits")
  public void addTaskCredits(@QueryParam("credits") Optional<Integer> credits) throws Exception {

  }

  @DELETE
  @Path("/task-credits")
  public void disableTaskCredits(@Context HttpServletRequest request) throws Exception {

  }

  @GET
  @Path("/task-credits")
  public SingularityTaskCredits getTaskCreditData() throws Exception {

  }
}
