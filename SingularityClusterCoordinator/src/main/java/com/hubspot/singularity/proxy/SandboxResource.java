package com.hubspot.singularity.proxy;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.singularity.SingularitySandbox;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.SANDBOX_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class SandboxResource {

  @GET
  @Path("/{taskId}/browse")
  public SingularitySandbox browse(@PathParam("taskId") String taskId, @QueryParam("path") String path) {

  }

  @GET
  @Path("/{taskId}/read")
  public MesosFileChunkObject read(@PathParam("taskId") String taskId, @QueryParam("path") String path, @QueryParam("grep") Optional<String> grep,
                                   @QueryParam("offset") Optional<Long> offset, @QueryParam("length") Optional<Long> length) {

  }
}
