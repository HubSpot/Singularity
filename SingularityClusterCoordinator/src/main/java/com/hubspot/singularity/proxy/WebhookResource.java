package com.hubspot.singularity.proxy;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.SingularityWebhookSummary;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.WEBHOOK_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class WebhookResource extends ProxyResource {

  @Inject
  public WebhookResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    super(configuration, httpClient, objectMapper);
  }

  @GET
  public List<SingularityWebhook> getActiveWebhooks() {

  }

  @GET
  @Path("/summary")
  public List<SingularityWebhookSummary> getWebhooksWithQueueSize() {

  }

  @POST
  public SingularityCreateResult addWebhook(SingularityWebhook webhook) {

  }

  @DELETE
  @Deprecated
  @Path("/{webhookId}")
  public SingularityDeleteResult deleteWebhookDeprecated(@PathParam("webhookId") String webhookId) {

  }

  @GET
  @Deprecated
  @Path("/deploy/{webhookId}")
  public List<SingularityDeployUpdate> getQueuedDeployUpdatesDeprecated(@PathParam("webhookId") String webhookId) {

  }

  @GET
  @Deprecated
  @Path("/request/{webhookId}")
  public List<SingularityRequestHistory> getQueuedRequestUpdatesDeprecated(@PathParam("webhookId") String webhookId) {

  }

  @GET
  @Deprecated
  @Path("/task/{webhookId}")
  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdatesDeprecated(@PathParam("webhookId") String webhookId) {

  }

  @DELETE
  public SingularityDeleteResult deleteWebhook(@QueryParam("webhookId") String webhookId) {

  }

  @GET
  @Path("/deploy")
  public List<SingularityDeployUpdate> getQueuedDeployUpdates(@QueryParam("webhookId") String webhookId) {

  }

  @GET
  @Path("/request")
  public List<SingularityRequestHistory> getQueuedRequestUpdates(@QueryParam("webhookId") String webhookId) {

  }

  @GET
  @Path("/task")
  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdates(@QueryParam("webhookId") String webhookId) {

  }

}
