package com.hubspot.singularity.hooks;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityLoadBalancerRequest;
import com.hubspot.singularity.SingularityPendingDeploy.LoadBalancerState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.Utils;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

public class LoadBalancerClient {

  private final static Logger LOG = LoggerFactory.getLogger(LoadBalancerClient.class);

  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final String HEADER_CONTENT_TYPE = "Content-Type";
  
  private final String loadBalancerUri;
  private final boolean hasValidUri;
  private final long loadBalancerTimeoutMillis;
  
  private final AsyncHttpClient httpClient;
  private final ObjectMapper objectMapper;
  
  private final String OPERATION_URI = "%s/%s";
  
  @Inject
  public LoadBalancerClient(SingularityConfiguration configuration, ObjectMapper objectMapper, AsyncHttpClient httpClient) {
    this.loadBalancerUri = configuration.getLoadBalancerUri();
    this.hasValidUri = loadBalancerUri != null;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.loadBalancerTimeoutMillis = configuration.getLoadBalancerRequestTimeoutMillis();
  }
  
  public boolean hasValidUri() {
    return hasValidUri;
  }
  
  private String getLoadBalancerUri(String loadBalanceRequestId) {
    return String.format(OPERATION_URI, loadBalancerUri, loadBalanceRequestId);
  }

  
  public Optional<LoadBalancerState> enqueue(String loadBalancerRequestId, List<SingularityTask> add, List<SingularityTask> remove) {
    final String uri = getLoadBalancerUri(loadBalancerRequestId);
    final SingularityLoadBalancerRequest loadBalancerRequest = new SingularityLoadBalancerRequest(loadBalancerRequestId, add, remove);
    
    Optional<LoadBalancerState> returnState = Optional.absent();
    final long start = System.currentTimeMillis();
    
    try {
      LOG.trace("Sending LB enqueue for {} to {}", loadBalancerRequestId, uri);
      
      ListenableFuture<Response> future = httpClient.preparePost(uri)
          .addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
          .setBody(loadBalancerRequest.getAsBytes(objectMapper))
          .execute();

      Response response = future.get(loadBalancerTimeoutMillis, TimeUnit.MILLISECONDS);
      
      LOG.trace("LB enqueue request {} returned with code {}", loadBalancerRequestId, response.getStatusCode());
      
      if (isSuccess(response)) {
        returnState = Optional.of(LoadBalancerState.WAITING);
      } else {
        returnState = Optional.of(LoadBalancerState.FAILED);
      }
      
    } catch (TimeoutException te) {
      LOG.trace("LB enqueue request {} timed out after waiting {}ms", loadBalancerRequestId, loadBalancerTimeoutMillis);
    } catch (Throwable t) {
      LOG.error("While posting request {} to {}", loadBalancerRequest, uri, t);
      returnState = Optional.of(LoadBalancerState.FAILED);
    } finally {
      LOG.trace("LB enqueue request {} had result {} after {}", loadBalancerRequestId, returnState, Utils.duration(start));
    }
    
    return returnState;
  }
  
  private boolean isSuccess(Response response) {
    return response.getStatusCode() > 199 && response.getStatusCode() < 300;
  }
  
  public void cancel() {
    
  }
  
  
  
}
