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
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.SingularityWebhookSummary;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.WEBHOOK_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class WebhookResource extends ProxyResource {
  // TODO - better routing here, route to all?

  @Inject
  public WebhookResource() {}

  @GET
  public List<SingularityWebhook> getActiveWebhooks(@Context HttpServletRequest request) {
    return routeToDefaultDataCenter(request, TypeRefs.WEBHOOK_LIST_REF);
  }

  @GET
  @Path("/summary")
  public List<SingularityWebhookSummary> getWebhooksWithQueueSize(@Context HttpServletRequest request) {
    return routeToDefaultDataCenter(request, TypeRefs.WEBHOOK_SUMMARY_LIST_REF);
  }

  @POST
  public Response addWebhook(@Context HttpServletRequest request, SingularityWebhook webhook) {
    return routeToDefaultDataCenter(request, TypeRefs.RESPONSE_REF);
  }

  @DELETE
  @Deprecated
  @Path("/{webhookId}")
  public Response deleteWebhookDeprecated(@Context HttpServletRequest request, @PathParam("webhookId") String webhookId) {
    return routeToDefaultDataCenter(request, TypeRefs.RESPONSE_REF);
  }

  @GET
  @Deprecated
  @Path("/deploy/{webhookId}")
  public List<SingularityDeployUpdate> getQueuedDeployUpdatesDeprecated(@Context HttpServletRequest request, @PathParam("webhookId") String webhookId) {
    return routeToDefaultDataCenter(request, TypeRefs.DEPLOY_UPDATE_LIST_REF);
  }

  @GET
  @Deprecated
  @Path("/request/{webhookId}")
  public List<SingularityRequestHistory> getQueuedRequestUpdatesDeprecated(@Context HttpServletRequest request, @PathParam("webhookId") String webhookId) {
    return routeToDefaultDataCenter(request, TypeRefs.REQUEST_HISTORY_LIST_REF);
  }

  @GET
  @Deprecated
  @Path("/task/{webhookId}")
  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdatesDeprecated(@Context HttpServletRequest request, @PathParam("webhookId") String webhookId) {
    return routeToDefaultDataCenter(request, TypeRefs.TASK_HISTORY_UPDATE_LIST_REF);
  }

  @DELETE
  public Response deleteWebhook(@Context HttpServletRequest request, @QueryParam("webhookId") String webhookId) {
    return routeToDefaultDataCenter(request, TypeRefs.RESPONSE_REF);
  }

  @GET
  @Path("/deploy")
  public List<SingularityDeployUpdate> getQueuedDeployUpdates(@Context HttpServletRequest request, @QueryParam("webhookId") String webhookId) {
    return routeToDefaultDataCenter(request, TypeRefs.DEPLOY_UPDATE_LIST_REF);
  }

  @GET
  @Path("/request")
  public List<SingularityRequestHistory> getQueuedRequestUpdates(@Context HttpServletRequest request, @QueryParam("webhookId") String webhookId) {
    return routeToDefaultDataCenter(request, TypeRefs.REQUEST_HISTORY_LIST_REF);
  }

  @GET
  @Path("/task")
  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdates(@Context HttpServletRequest request, @QueryParam("webhookId") String webhookId) {
    return routeToDefaultDataCenter(request, TypeRefs.TASK_HISTORY_UPDATE_LIST_REF);
  }

}
