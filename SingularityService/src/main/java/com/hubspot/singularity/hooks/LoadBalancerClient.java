package com.hubspot.singularity.hooks;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.LoadBalancerState;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerRequest;
import com.hubspot.singularity.SingularityLoadBalancerResponse;
import com.hubspot.singularity.SingularityLoadBalancerService;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Request;
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

  public SingularityLoadBalancerUpdate getState(String loadBalancerRequestId) {
    final String uri = getLoadBalancerUri(loadBalancerRequestId);
    
    final Request request = httpClient.prepareGet(uri)
      .build();
    
    return request(loadBalancerRequestId, request, LoadBalancerState.UNKNOWN);
  }
  
  private SingularityLoadBalancerResponse readResponse(Response response)  {
    try {
      return SingularityLoadBalancerResponse.fromBytes(response.getResponseBodyAsBytes(), objectMapper);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private SingularityLoadBalancerUpdate request(String loadBalancerRequestId, Request request, LoadBalancerState onFailure) {
    final long start = System.currentTimeMillis();
    final LoadBalancerState result = actualRequest(loadBalancerRequestId, request, onFailure);
    LOG.debug("LB {} request {} had result {} after {}", request.getMethod(), loadBalancerRequestId, result, JavaUtils.duration(start));
    return new SingularityLoadBalancerUpdate(result, loadBalancerRequestId, start);
  }
    
  private LoadBalancerState actualRequest(String loadBalancerRequestId, Request request, LoadBalancerState onFailure) {
    try {
      LOG.trace("Sending LB {} request for {} to {}", request.getMethod(), loadBalancerRequestId, request.getUrl());
      
      ListenableFuture<Response> future = httpClient.executeRequest(request);

      Response response = future.get(loadBalancerTimeoutMillis, TimeUnit.MILLISECONDS);
      
      LOG.trace("LB {} request {} returned with code {}", request.getMethod(), loadBalancerRequestId, response.getStatusCode());
      
      if (!isSuccess(response)) {
        return onFailure;
      }
      
      return readResponse(response).getLoadBalancerState();
    } catch (TimeoutException te) {
      LOG.trace("LB {} request {} timed out after waiting {}", request.getMethod(), loadBalancerRequestId, JavaUtils.durationFromMillis(loadBalancerTimeoutMillis));
      return LoadBalancerState.UNKNOWN;
    } catch (Throwable t) {
      LOG.error("LB {} request {} to {} threw error", request.getMethod(), loadBalancerRequestId, request.getUrl(), t);
      return onFailure;
    }
  }
  
  public SingularityLoadBalancerUpdate enqueue(String loadBalancerRequestId, SingularityRequest request, SingularityDeploy deploy, List<SingularityTask> add, List<SingularityTask> remove) {
    final SingularityLoadBalancerService lbService = SingularityLoadBalancerService.fromRequestAndDeploy(request, deploy);
    
    final List<String> addUpstreams = transformTasksToUpstreams(add);
    final List<String> removeUpstreams = transformTasksToUpstreams(remove);

    final SingularityLoadBalancerRequest loadBalancerRequest = new SingularityLoadBalancerRequest(loadBalancerRequestId, lbService, addUpstreams, removeUpstreams);
    
    final Request httpRequest = httpClient.preparePost(loadBalancerUri)
      .addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
      .setBody(loadBalancerRequest.getAsBytes(objectMapper))
      .build();
    
    return request(loadBalancerRequestId, httpRequest, LoadBalancerState.FAILED);
  }
  
  private boolean isSuccess(Response response) {
    return response.getStatusCode() > 199 && response.getStatusCode() < 300;
  }

  private List<String> transformTasksToUpstreams(List<SingularityTask> tasks) {
    final List<String> upstreams = Lists.newArrayListWithCapacity(tasks.size());

    for (SingularityTask task : tasks) {
      final Optional<Long> maybeFirstPort = MesosUtils.getFirstPort(task.getOffer());

      if (maybeFirstPort.isPresent()) {
        upstreams.add(String.format("%s:%d", task.getOffer().getHostname(), maybeFirstPort.get()));
      }
    }

    return upstreams;
  }
  
  public SingularityLoadBalancerUpdate cancel(String loadBalancerRequestId) {
    final String uri = getLoadBalancerUri(loadBalancerRequestId);
    
    final Request request = httpClient.prepareDelete(uri)
        .build();
    
    return request(uri, request, LoadBalancerState.UNKNOWN);
  }
  
}
