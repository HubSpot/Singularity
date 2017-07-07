package com.hubspot.singularity.proxy;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityPriorityFreezeParent;
import com.hubspot.singularity.api.SingularityPriorityFreeze;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.PRIORITY_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class PriorityResource {

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
