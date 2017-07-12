package com.hubspot.singularity.proxy;

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
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.WEBHOOK_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class WebhookResource extends ProxyResource {
  // TODO - better routing here, route to all?

  @Inject
  public WebhookResource() {}

  @GET
  public Response getActiveWebhooks(@Context HttpServletRequest request) {
    return routeToDefaultDataCenter(request);
  }

  @GET
  @Path("/summary")
  public Response getWebhooksWithQueueSize(@Context HttpServletRequest request) {
    return routeToDefaultDataCenter(request);
  }

  @POST
  public Response addWebhook(@Context HttpServletRequest request, SingularityWebhook webhook) {
    return routeToDefaultDataCenter(request, webhook);
  }

  @DELETE
  @Deprecated
  @Path("/{webhookId}")
  public Response deleteWebhookDeprecated(@Context HttpServletRequest request, @PathParam("webhookId") String webhookId) {
    return routeToDefaultDataCenter(request);
  }

  @GET
  @Deprecated
  @Path("/deploy/{webhookId}")
  public Response getQueuedDeployUpdatesDeprecated(@Context HttpServletRequest request, @PathParam("webhookId") String webhookId) {
    return routeToDefaultDataCenter(request);
  }

  @GET
  @Deprecated
  @Path("/request/{webhookId}")
  public Response getQueuedRequestUpdatesDeprecated(@Context HttpServletRequest request, @PathParam("webhookId") String webhookId) {
    return routeToDefaultDataCenter(request);
  }

  @GET
  @Deprecated
  @Path("/task/{webhookId}")
  public Response getQueuedTaskUpdatesDeprecated(@Context HttpServletRequest request, @PathParam("webhookId") String webhookId) {
    return routeToDefaultDataCenter(request);
  }

  @DELETE
  public Response deleteWebhook(@Context HttpServletRequest request, @QueryParam("webhookId") String webhookId) {
    return routeToDefaultDataCenter(request);
  }

  @GET
  @Path("/deploy")
  public Response getQueuedDeployUpdates(@Context HttpServletRequest request, @QueryParam("webhookId") String webhookId) {
    return routeToDefaultDataCenter(request);
  }

  @GET
  @Path("/request")
  public Response getQueuedRequestUpdates(@Context HttpServletRequest request, @QueryParam("webhookId") String webhookId) {
    return routeToDefaultDataCenter(request);
  }

  @GET
  @Path("/task")
  public Response getQueuedTaskUpdates(@Context HttpServletRequest request, @QueryParam("webhookId") String webhookId) {
    return routeToDefaultDataCenter(request);
  }

}
