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
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityDisabledAction;
import com.hubspot.singularity.SingularityDisasterType;
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityTaskCredits;
import com.hubspot.singularity.api.SingularityDisabledActionRequest;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.exceptions.NotImplemenedException;

@Path(ApiPaths.DISASTERS_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class DisastersResource extends ProxyResource {

  @Inject
  public DisastersResource() {}

  @GET
  @Path("/stats")
  public SingularityDisastersData disasterStats(@Context HttpServletRequest request) {
    throw new NotImplemenedException();
  }

  @GET
  @Path("/active")
  public List<SingularityDisasterType> activeDisasters(@Context HttpServletRequest request) {
    throw new NotImplemenedException();
  }

  @POST
  @Path("/disable")
  public void disableAutomatedDisasterCreation(@Context HttpServletRequest request) {
    throw new NotImplemenedException();
  }

  @POST
  @Path("/enable")
  public void enableAutomatedDisasterCreation(@Context HttpServletRequest request) {
    throw new NotImplemenedException();
  }

  @DELETE
  @Path("/active/{type}")
  public void removeDisaster(@Context HttpServletRequest request, @PathParam("type") SingularityDisasterType type) {
    throw new NotImplemenedException();
  }

  @POST
  @Path("/active/{type}")
  public void newDisaster(@Context HttpServletRequest request, @PathParam("type") SingularityDisasterType type) {
    throw new NotImplemenedException();
  }

  @GET
  @Path("/disabled-actions")
  public List<SingularityDisabledAction> disabledActions(@Context HttpServletRequest request) {
    throw new NotImplemenedException();
  }

  @POST
  @Path("/disabled-actions/{action}")
  public void disableAction(@Context HttpServletRequest request, @PathParam("action") SingularityAction action, SingularityDisabledActionRequest disabledActionRequest) {
    throw new NotImplemenedException();
  }

  @DELETE
  @Path("/disabled-actions/{action}")
  public void enableAction(@Context HttpServletRequest request, @PathParam("action") SingularityAction action) {
    throw new NotImplemenedException();
  }

  @POST
  @Path("/task-credits")
  public void addTaskCredits(@Context HttpServletRequest request, @QueryParam("credits") Optional<Integer> credits) throws Exception {
    throw new NotImplemenedException();
  }

  @DELETE
  @Path("/task-credits")
  public void disableTaskCredits(@Context HttpServletRequest request) throws Exception {
    throw new NotImplemenedException();
  }

  @GET
  @Path("/task-credits")
  public SingularityTaskCredits getTaskCreditData(@Context HttpServletRequest request) throws Exception {
    throw new NotImplemenedException();
  }
}
