package com.hubspot.singularity.hooks;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.protos.MesosParameter;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityLoadBalancerUpdate.LoadBalancerMethod;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import com.hubspot.singularity.helpers.MesosUtils;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

public class LoadBalancerClientImpl implements LoadBalancerClient {

  private static final Logger LOG = LoggerFactory.getLogger(LoadBalancerClient.class);

  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final String HEADER_CONTENT_TYPE = "Content-Type";

  private final String loadBalancerUri;
  private final Optional<Map<String, String>> loadBalancerQueryParams;
  private final long loadBalancerTimeoutMillis;

  private final AsyncHttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final Optional<String> taskLabelForLoadBalancerUpstreamGroup;
  private final MesosProtosUtils mesosProtosUtils;

  private static final String OPERATION_URI = "%s/%s";

  @Inject
  public LoadBalancerClientImpl(SingularityConfiguration configuration, ObjectMapper objectMapper, AsyncHttpClient httpClient, MesosProtosUtils mesosProtosUtils) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.loadBalancerUri = configuration.getLoadBalancerUri();
    this.loadBalancerTimeoutMillis = configuration.getLoadBalancerRequestTimeoutMillis();
    this.loadBalancerQueryParams = configuration.getLoadBalancerQueryParams();
    this.taskLabelForLoadBalancerUpstreamGroup = configuration.getTaskLabelForLoadBalancerUpstreamGroup();
    this.mesosProtosUtils = mesosProtosUtils;
  }

  private String getLoadBalancerUri(LoadBalancerRequestId loadBalancerRequestId) {
    return String.format(OPERATION_URI, loadBalancerUri, loadBalancerRequestId);
  }

  private void addAllQueryParams(BoundRequestBuilder boundRequestBuilder, Map<String, String> queryParams) {
    for (Map.Entry<String, String> entry : queryParams.entrySet()) {
      boundRequestBuilder.addQueryParameter(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public SingularityLoadBalancerUpdate getState(LoadBalancerRequestId loadBalancerRequestId) {
    final String uri = getLoadBalancerUri(loadBalancerRequestId);

    final BoundRequestBuilder requestBuilder = httpClient.prepareGet(uri);

    if (loadBalancerQueryParams.isPresent()) {
      addAllQueryParams(requestBuilder, loadBalancerQueryParams.get());
    }

    return sendRequestWrapper(loadBalancerRequestId, LoadBalancerMethod.CHECK_STATE, requestBuilder.build(), BaragonRequestState.UNKNOWN);
  }

  private BaragonResponse readResponse(Response response) {
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

  private SingularityLoadBalancerUpdate sendBaragonRequest(LoadBalancerRequestId loadBalancerRequestId, BaragonRequest loadBalancerRequest, LoadBalancerMethod method) {
    try {
      LOG.trace("Preparing to send request {}", loadBalancerRequest);

      final BoundRequestBuilder requestBuilder = httpClient.preparePost(loadBalancerUri)
        .addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
        .setBody(objectMapper.writeValueAsBytes(loadBalancerRequest));

      if (loadBalancerQueryParams.isPresent()) {
        addAllQueryParams(requestBuilder, loadBalancerQueryParams.get());
      }

      return sendRequestWrapper(loadBalancerRequestId, method, requestBuilder.build(), BaragonRequestState.FAILED);
    } catch (IOException e) {
      return new SingularityLoadBalancerUpdate(BaragonRequestState.UNKNOWN, loadBalancerRequestId, Optional.of(e.getMessage()), System.currentTimeMillis(), method, Optional.of(loadBalancerUri));
    }
  }

  private LoadBalancerUpdateHolder sendRequest(LoadBalancerRequestId loadBalancerRequestId, Request request, BaragonRequestState onFailure) {
    try {
      LOG.trace("Sending LB {} request for {} to {}", request.getMethod(), loadBalancerRequestId, request.getUrl());

      ListenableFuture<Response> future = httpClient.executeRequest(request);

      Response response = future.get(loadBalancerTimeoutMillis, TimeUnit.MILLISECONDS);

      LOG.trace("LB {} request {} returned with code {}", request.getMethod(), loadBalancerRequestId, response.getStatusCode());

      if (response.getStatusCode() == 504) {
        return new LoadBalancerUpdateHolder(BaragonRequestState.UNKNOWN, Optional.of(String.format("LB %s request %s timed out", request.getMethod(), loadBalancerRequestId)));
      } else if (!JavaUtils.isHttpSuccess(response.getStatusCode())) {
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

  @Override
  public SingularityLoadBalancerUpdate enqueue(LoadBalancerRequestId loadBalancerRequestId, SingularityRequest request, SingularityDeploy deploy, List<SingularityTask> add,
      List<SingularityTask> remove) {
    final List<String> serviceOwners = request.getOwners().or(Collections.<String> emptyList());
    final Set<String> loadBalancerGroups = deploy.getLoadBalancerGroups().or(Collections.<String>emptySet());
    final BaragonService lbService = new BaragonService(deploy.getLoadBalancerServiceIdOverride().or(request.getId()), serviceOwners, deploy.getServiceBasePath().get(),
      deploy.getLoadBalancerAdditionalRoutes().or(Collections.<String>emptyList()), loadBalancerGroups, deploy.getLoadBalancerOptions().orNull(),
      deploy.getLoadBalancerTemplate(), deploy.getLoadBalancerDomains().or(Collections.<String>emptySet()));

    final List<UpstreamInfo> addUpstreams = tasksToUpstreams(add, loadBalancerRequestId.toString(), deploy.getLoadBalancerUpstreamGroup());
    final List<UpstreamInfo> removeUpstreams = tasksToUpstreams(remove, loadBalancerRequestId.toString(), deploy.getLoadBalancerUpstreamGroup());

    final BaragonRequest loadBalancerRequest = new BaragonRequest(loadBalancerRequestId.toString(), lbService, addUpstreams, removeUpstreams);

    return sendBaragonRequest(loadBalancerRequestId, loadBalancerRequest, LoadBalancerMethod.ENQUEUE);
  }

  private List<UpstreamInfo> tasksToUpstreams(List<SingularityTask> tasks, String requestId, Optional<String> loadBalancerUpstreamGroup) {
    final List<UpstreamInfo> upstreams = Lists.newArrayListWithCapacity(tasks.size());

    for (SingularityTask task : tasks) {
      final Optional<Long> maybeLoadBalancerPort = MesosUtils.getPortByIndex(mesosProtosUtils.toResourceList(task.getMesosTask().getResources()), task.getTaskRequest().getDeploy().getLoadBalancerPortIndex().or(0));

      if (maybeLoadBalancerPort.isPresent()) {
        String upstream = String.format("%s:%d", task.getHostname(), maybeLoadBalancerPort.get());
        Optional<String> group = loadBalancerUpstreamGroup;

        if (taskLabelForLoadBalancerUpstreamGroup.isPresent()) {
          for (MesosParameter label : task.getMesosTask().getLabels().getLabels()) {
            if (label.hasKey() && label.getKey().equals(taskLabelForLoadBalancerUpstreamGroup.get()) && label.hasValue()) {
              group = Optional.of(label.getValue());
              break;
            }
          }
        }

        upstreams.add(new UpstreamInfo(upstream, Optional.of(requestId), task.getRackId(), Optional.<String>absent(), group));
      } else {
        LOG.warn("Task {} is missing port but is being passed to LB  ({})", task.getTaskId(), task);
      }
    }

    return upstreams;
  }

  @Override
  public SingularityLoadBalancerUpdate cancel(LoadBalancerRequestId loadBalancerRequestId) {
    final String uri = getLoadBalancerUri(loadBalancerRequestId);

    final BoundRequestBuilder requestBuilder = httpClient.prepareDelete(uri);

    if (loadBalancerQueryParams.isPresent()) {
      addAllQueryParams(requestBuilder, loadBalancerQueryParams.get());
    }

    return sendRequestWrapper(loadBalancerRequestId, LoadBalancerMethod.CANCEL, requestBuilder.build(), BaragonRequestState.UNKNOWN);
  }

  @Override
  public SingularityLoadBalancerUpdate delete(LoadBalancerRequestId loadBalancerRequestId, String requestId, Set<String> loadBalancerGroups, String serviceBasePath) {
    final BaragonService lbService = new BaragonService(requestId, Collections.<String> emptyList(), serviceBasePath, loadBalancerGroups, Collections.<String, Object>emptyMap());
    final BaragonRequest loadBalancerRequest = new BaragonRequest(loadBalancerRequestId.toString(), lbService, Collections.<UpstreamInfo>emptyList(), Collections.<UpstreamInfo>emptyList(), Collections.<UpstreamInfo>emptyList(), Optional.<String>absent(), Optional.of(RequestAction.DELETE));

    return sendBaragonRequest(loadBalancerRequestId, loadBalancerRequest, LoadBalancerMethod.DELETE);
  }
}
