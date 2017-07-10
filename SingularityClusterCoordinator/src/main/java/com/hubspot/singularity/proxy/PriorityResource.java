package com.hubspot.singularity.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPriorityFreezeParent;
import com.hubspot.singularity.api.SingularityPriorityFreeze;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.exceptions.NotImplemenedException;

@Path(ApiPaths.PRIORITY_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class PriorityResource extends ProxyResource {

  @Inject
  public PriorityResource() {}

  @GET
  @Path("/freeze")
  public Optional<SingularityPriorityFreezeParent> getActivePriorityFreeze(@Context HttpServletRequest request) {
    throw new NotImplemenedException();
  }

  @DELETE
  @Path("/freeze")
  public void deleteActivePriorityFreeze(@Context HttpServletRequest request) {
    throw new NotImplemenedException();
  }

  @POST
  @Path("/freeze")
  public SingularityPriorityFreezeParent createPriorityFreeze(@Context HttpServletRequest request, SingularityPriorityFreeze priorityFreezeRequest) {
    throw new NotImplemenedException();
  }
}
