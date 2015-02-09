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
  private final boolean shouldDeleteUpdateOnFailure;

  public AbstractSingularityWebhookAsyncHandler(SingularityWebhook webhook, T update, boolean shouldDeleteUpdateOnFailure) {
    this.webhook = webhook;
    this.update = update;
    this.shouldDeleteUpdateOnFailure = shouldDeleteUpdateOnFailure;

    this.start = System.currentTimeMillis();
  }

  @Override
  public void onThrowable(Throwable t) {
    LOG.trace("Webhook {} for {} failed after {}", webhook.getUri(), update, JavaUtils.duration(start), t);

    if (shouldDeleteUpdateOnFailure) {
      deleteWebhookUpdate();
    }
  }

  public boolean shouldDeleteUpdateDueToQueueAboveCapacity() {
    return shouldDeleteUpdateOnFailure;
  }

  @Override
  public Response onCompleted(Response response) throws Exception {
    LOG.trace("Webhook {} for {} completed with {} after {}", webhook.getUri(), update, response.getStatusCode(), JavaUtils.duration(start));

    if (response.hasResponseBody()) {
      LOG.trace("Webhook response message is: '{}'", response.getResponseBody());
    }

    if (JavaUtils.isHttpSuccess(response.getStatusCode()) || shouldDeleteUpdateOnFailure) {
      deleteWebhookUpdate();
    }

    return response;
  }

  public abstract void deleteWebhookUpdate();

}
