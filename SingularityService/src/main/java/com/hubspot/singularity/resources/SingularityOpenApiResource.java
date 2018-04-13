package com.hubspot.singularity.resources;

import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.hubspot.singularity.config.ApiPaths;

import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;

@Path(ApiPaths.OPEN_API_RESOURCE_PATH)
public class SingularityOpenApiResource extends BaseOpenApiResource {
  @Context
  ServletConfig config;

  @Context
  Application app;

  private final AtomicReference<Response> cachedApiJson = new AtomicReference<>();

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(hidden = true)
  public Response getOpenApi(@Context HttpHeaders headers,
                             @Context UriInfo uriInfo) throws Exception {
    if (cachedApiJson.get() == null) {
      Response openApi = super.getOpenApi(headers, config, app, uriInfo, "json");
      cachedApiJson.set(openApi);
      return openApi;
    } else {
      return cachedApiJson.get();
    }
  }
}
