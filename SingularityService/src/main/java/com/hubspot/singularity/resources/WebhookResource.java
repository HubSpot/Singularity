package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import com.google.inject.Inject;
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
  private final UriInfo uriInfo;
  
  @Inject
  public WebhookResource(WebhookManager webhookManager, UriInfo uriInfo) {
    this.webhookManager = webhookManager;
    this.uriInfo = uriInfo;
  }

  @GET
  public List<SingularityWebhook> getActiveWebhooks() {
    return webhookManager.getActiveWebhooks();
  }

  @POST
  public SingularityCreateResult addWebhook(SingularityWebhook webhook) {
    return webhookManager.addWebhook(webhook);
  }

  private String getWebhookId() {
    return uriInfo.getPathParameters(false).getFirst("webhookId");
  }
  
  @DELETE
  @Path("/{webhookId}")
  public SingularityDeleteResult deleteWebhook() {
    return webhookManager.deleteWebhook(getWebhookId());
  }

  @GET
  @Path("/deploy/{webhookId}")
  public List<SingularityDeployWebhook> getQueuedDeployUpdates() {
    return webhookManager.getQueuedDeployUpdatesForHook(getWebhookId());
  }
  
  @GET
  @Path("/request/{webhookId}")
  public List<SingularityRequestHistory> getQueuedRequestUpdates() {
    return webhookManager.getQueuedRequestHistoryForHook(getWebhookId());
  }
  
  @GET
  @Path("/task/{webhookId}")
  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdates() {
    return webhookManager.getQueuedTaskUpdatesForHook(getWebhookId());
  }

}
