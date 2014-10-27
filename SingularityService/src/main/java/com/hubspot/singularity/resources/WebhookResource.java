package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeployWebhook;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.data.WebhookManager;

@Path(SingularityService.API_BASE_PATH + "/webhooks")
@Produces({ MediaType.APPLICATION_JSON })
public class WebhookResource {

  private final WebhookManager webhookManager;

  @Inject
  public WebhookResource(WebhookManager webhookManager) {
    this.webhookManager = webhookManager;
  }

  @GET
  public List<SingularityWebhook> getActiveWebhooks() {
    return webhookManager.getActiveWebhooks();
  }

  @POST
  public SingularityCreateResult addWebhook(SingularityWebhook webhook) {
    return webhookManager.addWebhook(webhook);
  }

  @DELETE
  @Path("/{webhookId}")
  public SingularityDeleteResult deleteWebhook(@PathParam("webhookId") String webhookId) {
    return webhookManager.deleteWebhook(JavaUtils.urlEncode(webhookId));
  }

  @GET
  @Path("/deploy/{webhookId}")
  public List<SingularityDeployWebhook> getQueuedDeployUpdates(@PathParam("webhookId") String webhookId) {
    return webhookManager.getQueuedDeployUpdatesForHook(JavaUtils.urlEncode(webhookId));
  }

  @GET
  @Path("/request/{webhookId}")
  public List<SingularityRequestHistory> getQueuedRequestUpdates(@PathParam("webhookId") String webhookId) {
    return webhookManager.getQueuedRequestHistoryForHook(JavaUtils.urlEncode(webhookId));
  }

  @GET
  @Path("/task/{webhookId}")
  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdates(@PathParam("webhookId") String webhookId) {
    return webhookManager.getQueuedTaskUpdatesForHook(JavaUtils.urlEncode(webhookId));
  }

}
