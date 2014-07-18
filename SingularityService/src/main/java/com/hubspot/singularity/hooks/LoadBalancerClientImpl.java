package com.hubspot.singularity.hooks;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityLoadBalancerUpdate.LoadBalancerMethod;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Request;
import com.ning.http.client.Response;


public class LoadBalancerClientImpl implements LoadBalancerClient {
  
  private final static Logger LOG = LoggerFactory.getLogger(LoadBalancerClient.class);

  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final String HEADER_CONTENT_TYPE = "Content-Type";
  
  private final String loadBalancerUri;
  private final long loadBalancerTimeoutMillis;
  
  private final AsyncHttpClient httpClient;
  private final ObjectMapper objectMapper;
  
  private final String OPERATION_URI = "%s/%s";
  
  @Inject
  public LoadBalancerClientImpl(SingularityConfiguration configuration, ObjectMapper objectMapper, AsyncHttpClient httpClient) {
    this.loadBalancerUri = configuration.getLoadBalancerUri();
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.loadBalancerTimeoutMillis = configuration.getLoadBalancerRequestTimeoutMillis();
  }
  
  private String getLoadBalancerUri(LoadBalancerRequestId loadBalancerRequestId) {
    return String.format(OPERATION_URI, loadBalancerUri, loadBalancerRequestId);
  }

  public SingularityLoadBalancerUpdate getState(LoadBalancerRequestId loadBalancerRequestId) {
    final String uri = getLoadBalancerUri(loadBalancerRequestId);
    
    final Request request = httpClient.prepareGet(uri)
      .build();

    return sendRequestWrapper(loadBalancerRequestId, LoadBalancerMethod.CHECK_STATE, request, BaragonRequestState.UNKNOWN);
  }
  
  private BaragonResponse readResponse(Response response)  {
    try {
      return objectMapper.readValue(response.getResponseBodyAsBytes(), BaragonResponse.class);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private SingularityLoadBalancerUpdate sendRequestWrapper(LoadBalancerRequestId loadBalancerRequestId, LoadBalancerMethod method, Request request, BaragonRequestState onFailure) {
    final long start = System.currentTimeMillis();
    final LoadBalancerUpdateHolder result = sendRequest(loadBalancerRequestId, request, onFailure);
    LOG.debug("LB {} request {} had result {} after {}", request.getMethod(), loadBalancerRequestId, result, JavaUtils.duration(start));
    return new SingularityLoadBalancerUpdate(result.state, loadBalancerRequestId, result.message, start, method, Optional.of(request.getUrl()));
  }

  private static class LoadBalancerUpdateHolder {
    
    private final Optional<String> message;
    private final BaragonRequestState state;
    
    public LoadBalancerUpdateHolder(BaragonRequestState state, Optional<String> message) {
      this.message = message;
      this.state = state;
    }

    @Override
    public String toString() {
      return "LoadBalancerUpdateHolder [message=" + message + ", state=" + state + "]";
    }
       
  }

  private LoadBalancerUpdateHolder sendRequest(LoadBalancerRequestId loadBalancerRequestId, Request request, BaragonRequestState onFailure) {
    try {
      LOG.trace("Sending LB {} request for {} to {}", request.getMethod(), loadBalancerRequestId, request.getUrl());
      
      ListenableFuture<Response> future = httpClient.executeRequest(request);

      Response response = future.get(loadBalancerTimeoutMillis, TimeUnit.MILLISECONDS);
      
      LOG.trace("LB {} request {} returned with code {}", request.getMethod(), loadBalancerRequestId, response.getStatusCode());
      
      if (!JavaUtils.isHttpSuccess(response.getStatusCode())) {
        return new LoadBalancerUpdateHolder(onFailure, Optional.of(String.format("Response status code %s", response.getStatusCode())));
      }
      
      BaragonResponse lbResponse = readResponse(response);
      
      return new LoadBalancerUpdateHolder(lbResponse.getLoadBalancerState(), lbResponse.getMessage());
    } catch (TimeoutException te) {
      LOG.trace("LB {} request {} timed out after waiting {}", request.getMethod(), loadBalancerRequestId, JavaUtils.durationFromMillis(loadBalancerTimeoutMillis));
      return new LoadBalancerUpdateHolder(BaragonRequestState.UNKNOWN, Optional.of(String.format("Timed out after %s", JavaUtils.durationFromMillis(loadBalancerTimeoutMillis))));
    } catch (Throwable t) {
      LOG.error("LB {} request {} to {} threw error", request.getMethod(), loadBalancerRequestId, request.getUrl(), t);
      return new LoadBalancerUpdateHolder(BaragonRequestState.UNKNOWN, Optional.of(String.format("Exception %s - %s", t.getClass().getSimpleName(), t.getMessage())));
    }
  }
  
  public SingularityLoadBalancerUpdate enqueue(LoadBalancerRequestId loadBalancerRequestId, SingularityRequest request, SingularityDeploy deploy, List<SingularityTask> add, List<SingularityTask> remove) {
    final List<String> serviceOwners = request.getOwners().or(Collections.<String>emptyList());
    final List<String> loadBalancerGroups = deploy.getLoadBalancerGroups().or(Collections.<String>emptyList());
    final BaragonService lbService = new BaragonService(request.getId(), serviceOwners, deploy.getServiceBasePath().get(), loadBalancerGroups, deploy.getLoadBalancerOptions().orNull());
    
    final List<String> addUpstreams = transformTasksToUpstreams(add);
    final List<String> removeUpstreams = transformTasksToUpstreams(remove);

    final BaragonRequest loadBalancerRequest = new BaragonRequest(loadBalancerRequestId.toString(), lbService, addUpstreams, removeUpstreams);

    try {
      LOG.trace("Deploy {} is preparing to send {}", deploy.getId(), loadBalancerRequest);
      
      final Request httpRequest = httpClient.preparePost(loadBalancerUri)
          .addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
          .setBody(objectMapper.writeValueAsBytes(loadBalancerRequest))
          .build();

      return sendRequestWrapper(loadBalancerRequestId, LoadBalancerMethod.ENQUEUE, httpRequest, BaragonRequestState.FAILED);
    } catch (JsonProcessingException e) {
      throw new SingularityJsonException(e);
    }
  }

  private List<String> transformTasksToUpstreams(List<SingularityTask> tasks) {
    final List<String> upstreams = Lists.newArrayListWithCapacity(tasks.size());

    for (SingularityTask task : tasks) {
      final Optional<Long> maybeFirstPort = task.getFirstPort();

      if (maybeFirstPort.isPresent()) {
        upstreams.add(String.format("%s:%d", task.getOffer().getHostname(), maybeFirstPort.get()));
      } else {
        LOG.warn("Task {} is missing port but is being passed to LB  ({})", task.getTaskId(), task);
      }
    }

    return upstreams;
  }
  
  public SingularityLoadBalancerUpdate cancel(LoadBalancerRequestId loadBalancerRequestId) {
    final String uri = getLoadBalancerUri(loadBalancerRequestId);
    
    final Request request = httpClient.prepareDelete(uri)
        .build();

    return sendRequestWrapper(loadBalancerRequestId, LoadBalancerMethod.CANCEL, request, BaragonRequestState.UNKNOWN);
  }
}
