package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.data.WebhookManager;

@Path(SingularityService.API_BASE_PATH + "/webhooks")
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
    return webhookManager.deleteWebhook(webhookId);
  }

//  @GET
//  @Path("/deploy/{webhookId}")
//  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdates(@PathParam("webhookId") String webhookId) {
//    return webhookManager.getQueuedTaskUpdatesForHook(webhookId);
//  }
//
//  @GET
//  @Path("/request/{webhookId}")
//  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdates(@PathParam("webhookId") String webhookId) {
//    return webhookManager.getQueuedTaskUpdatesForHook(webhookId);
//  }
  
  @GET
  @Path("/task/{webhookId}")
  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdates(@PathParam("webhookId") String webhookId) {
    return webhookManager.getQueuedTaskUpdatesForHook(webhookId);
  }

}
