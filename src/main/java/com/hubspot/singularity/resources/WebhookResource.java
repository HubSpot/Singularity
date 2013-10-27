package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.singularity.hooks.WebhookManager;

@Path("/webhooks")
@Produces({ MediaType.APPLICATION_JSON })
public class WebhookResource {

  private final WebhookManager webhookManager;
  
  @Inject
  public WebhookResource(WebhookManager webhookManager) {
    this.webhookManager = webhookManager;
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  public void addHooks(List<String> hooks) {
    for (String hook : hooks) {
      webhookManager.addHook(hook);
    }
  }

  @GET
  public List<String> getWebhooks() {
    return webhookManager.getWebhooks();
  }
 
  @DELETE
  @Path("/{hook}")
  public void getHistoryForTask(@PathParam("hook") String hook) {
    webhookManager.removeHook(hook);
  }

}
