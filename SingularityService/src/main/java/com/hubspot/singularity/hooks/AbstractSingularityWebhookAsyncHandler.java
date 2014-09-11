package com.hubspot.singularity.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityWebhook;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

public abstract class AbstractSingularityWebhookAsyncHandler<T> extends AsyncCompletionHandler<Response>  {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractSingularityWebhookAsyncHandler.class);

  protected final SingularityWebhook webhook;
  protected final T update;
  private final long start;
  private final boolean shouldDeleteUpdateDueToQueueAboveCapacity;

  public AbstractSingularityWebhookAsyncHandler(SingularityWebhook webhook, T update, boolean shouldDeleteUpdateDueToQueueAboveCapacity) {
    this.webhook = webhook;
    this.update = update;
    this.shouldDeleteUpdateDueToQueueAboveCapacity = shouldDeleteUpdateDueToQueueAboveCapacity;

    this.start = System.currentTimeMillis();
  }

  @Override
  public void onThrowable(Throwable t) {
    LOG.trace("Webhook {} for {} failed after {}", webhook.getUri(), update, JavaUtils.duration(start), t);

    if (shouldDeleteUpdateDueToQueueAboveCapacity) {
      deleteWebhookUpdate();
    }
  }

  public boolean shouldDeleteUpdateDueToQueueAboveCapacity() {
    return shouldDeleteUpdateDueToQueueAboveCapacity;
  }

  @Override
  public Response onCompleted(Response response) throws Exception {
    LOG.trace("Webhook {} for {} completed with {} after {}", webhook.getUri(), update, response.getStatusCode(), JavaUtils.duration(start));

    if (JavaUtils.isHttpSuccess(response.getStatusCode()) || shouldDeleteUpdateDueToQueueAboveCapacity) {
      deleteWebhookUpdate();
    }

    return response;
  }

  public abstract void deleteWebhookUpdate();

}
