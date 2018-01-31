package com.hubspot.singularity.resources.ui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.resources.SingularityServiceUIModule;

@Path("/")
@Produces(MediaType.TEXT_HTML)
public class IndexResource {
  private final String singularityUriBase;

  @Inject
  public IndexResource(@Named(SingularityServiceUIModule.SINGULARITY_URI_BASE) String singularityUriBase) {
    this.singularityUriBase = singularityUriBase;
  }

  @GET
  @Path("/")
  public Response getIndex(@Context UriInfo info) {
    return Response.status(Status.MOVED_PERMANENTLY).location(UriBuilder.fromPath(singularityUriBase).path(UiResource.UI_RESOURCE_LOCATION).build()).build();
  }
}
