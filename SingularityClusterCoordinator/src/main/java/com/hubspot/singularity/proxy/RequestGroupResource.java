package com.hubspot.singularity.proxy;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityRequestGroup;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.REQUEST_GROUP_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class RequestGroupResource {

  @GET
  public List<SingularityRequestGroup> getRequestGroupIds(@QueryParam("useWebCache") Boolean useWebCache) {

  }

  @GET
  @Path("/group/{requestGroupId}")
  public Optional<SingularityRequestGroup> getRequestGroup(@PathParam("requestGroupId") String requestGroupId) {

  }

  @DELETE
  @Path("/group/{requestGroupId}")
  public void deleteRequestGroup(@PathParam("requestGroupId") String requestGroupId) {

  }

  @POST
  public SingularityRequestGroup saveRequestGroup(SingularityRequestGroup requestGroup) {

  }
}
