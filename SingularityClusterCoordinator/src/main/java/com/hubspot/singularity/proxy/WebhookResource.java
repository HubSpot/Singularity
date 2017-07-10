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

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.SingularityWebhookSummary;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.exceptions.NotImplemenedException;

@Path(ApiPaths.WEBHOOK_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class WebhookResource extends ProxyResource {

  @Inject
  public WebhookResource() {}

  @GET
  public List<SingularityWebhook> getActiveWebhooks(@Context HttpServletRequest request) {
    throw new NotImplemenedException();
  }

  @GET
  @Path("/summary")
  public List<SingularityWebhookSummary> getWebhooksWithQueueSize(@Context HttpServletRequest request) {
    throw new NotImplemenedException();
  }

  @POST
  public SingularityCreateResult addWebhook(@Context HttpServletRequest request, SingularityWebhook webhook) {
    throw new NotImplemenedException();
  }

  @DELETE
  @Deprecated
  @Path("/{webhookId}")
  public SingularityDeleteResult deleteWebhookDeprecated(@Context HttpServletRequest request, @PathParam("webhookId") String webhookId) {
    throw new NotImplemenedException();
  }

  @GET
  @Deprecated
  @Path("/deploy/{webhookId}")
  public List<SingularityDeployUpdate> getQueuedDeployUpdatesDeprecated(@Context HttpServletRequest request, @PathParam("webhookId") String webhookId) {
    throw new NotImplemenedException();
  }

  @GET
  @Deprecated
  @Path("/request/{webhookId}")
  public List<SingularityRequestHistory> getQueuedRequestUpdatesDeprecated(@Context HttpServletRequest request, @PathParam("webhookId") String webhookId) {
    throw new NotImplemenedException();
  }

  @GET
  @Deprecated
  @Path("/task/{webhookId}")
  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdatesDeprecated(@Context HttpServletRequest request, @PathParam("webhookId") String webhookId) {
    throw new NotImplemenedException();
  }

  @DELETE
  public SingularityDeleteResult deleteWebhook(@Context HttpServletRequest request, @QueryParam("webhookId") String webhookId) {
    throw new NotImplemenedException();
  }

  @GET
  @Path("/deploy")
  public List<SingularityDeployUpdate> getQueuedDeployUpdates(@Context HttpServletRequest request, @QueryParam("webhookId") String webhookId) {
    throw new NotImplemenedException();
  }

  @GET
  @Path("/request")
  public List<SingularityRequestHistory> getQueuedRequestUpdates(@Context HttpServletRequest request, @QueryParam("webhookId") String webhookId) {
    throw new NotImplemenedException();
  }

  @GET
  @Path("/task")
  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdates(@Context HttpServletRequest request, @QueryParam("webhookId") String webhookId) {
    throw new NotImplemenedException();
  }

}
