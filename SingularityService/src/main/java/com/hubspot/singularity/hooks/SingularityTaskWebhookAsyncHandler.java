package com.hubspot.singularity.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.data.WebhookManager;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

public class SingularityTaskWebhookAsyncHandler extends AsyncCompletionHandler<Response>  {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityTaskWebhookAsyncHandler.class);
  
  private final WebhookManager webhookManager;
  private final SingularityWebhook webhook;
  private final SingularityTaskHistoryUpdate taskUpdate;
  private final long start;
  
  public SingularityTaskWebhookAsyncHandler(WebhookManager webhookManager, SingularityWebhook webhook, SingularityTaskHistoryUpdate taskUpdate) {
    this.webhookManager = webhookManager;
    this.webhook = webhook;
    this.taskUpdate = taskUpdate;
  
    this.start = System.currentTimeMillis();
  }

  @Override
  public Response onCompleted(Response response) throws Exception {
    LOG.trace("Webhook {} for {} completed with {} after {}", webhook.getUri(), taskUpdate.getTaskId(), response.getStatusCode(), JavaUtils.duration(start));
    
    if (JavaUtils.isHttpSuccess(response.getStatusCode())) {
      webhookManager.deleteTaskUpdate(webhook, taskUpdate);
    }
    
    return response;
  }  
  
}
