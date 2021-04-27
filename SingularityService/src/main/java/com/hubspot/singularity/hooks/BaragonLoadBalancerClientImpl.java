package com.hubspot.singularity.hooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.RequestAction;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.LoadBalancerRequestState;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.LoadBalancerUpstream;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityLoadBalancerUpdate.LoadBalancerMethod;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaragonLoadBalancerClientImpl extends LoadBalancerClient {
  private static final Logger LOG = LoggerFactory.getLogger(LoadBalancerClient.class);

  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final String HEADER_CONTENT_TYPE = "Content-Type";
  private static final String ALREADY_ENQUEUED_ERROR =
    "is already enqueued with different parameters";

  private final String loadBalancerUri;
  private final Optional<Map<String, String>> loadBalancerQueryParams;
  private final long loadBalancerTimeoutMillis;

  private final AsyncHttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final boolean preResolveUpstreamDNS;
  private final Set<String> skipDNSPreResolutionForRequests;

  private static final String OPERATION_URI = "%s/%s";

  @Inject
  public BaragonLoadBalancerClientImpl(
    SingularityConfiguration configuration,
    @Singularity ObjectMapper objectMapper,
    AsyncHttpClient httpClient,
    MesosProtosUtils mesosProtosUtils
  ) {
    super(configuration, mesosProtosUtils);
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.loadBalancerUri = configuration.getLoadBalancerUri();
    this.loadBalancerTimeoutMillis = configuration.getLoadBalancerRequestTimeoutMillis();
    this.loadBalancerQueryParams = configuration.getLoadBalancerQueryParams();
    this.skipDNSPreResolutionForRequests =
      configuration.getSkipDNSPreResolutionForRequests();
    this.preResolveUpstreamDNS = configuration.isPreResolveUpstreamDNS();
  }

  public boolean isEnabled() {
    return true;
  }

  private String getStateUriFromRequestUri() {
    return loadBalancerUri.replace("request", "state");
  }

  private String getLoadBalancerStateUri(String singularityRequestId) {
    return String.format(
      OPERATION_URI,
      getStateUriFromRequestUri(),
      singularityRequestId
    );
  }

  public List<LoadBalancerUpstream> getUpstreamsForRequest(String singularityRequestId)
    throws IOException, InterruptedException, ExecutionException, TimeoutException {
    final String loadBalancerStateUri = getLoadBalancerStateUri(singularityRequestId);
    final BoundRequestBuilder requestBuilder = httpClient.prepareGet(
      loadBalancerStateUri
    );
    final Request request = requestBuilder.build();
    LOG.debug(
      "Sending load balancer {} request for {} to {}",
      request.getMethod(),
      singularityRequestId,
      request.getUrl()
    );
    ListenableFuture<Response> future = httpClient.executeRequest(request);
    Response response = future.get(loadBalancerTimeoutMillis, TimeUnit.MILLISECONDS);
    LOG.debug(
      "Load balancer {} request {} returned with code {}",
      request.getMethod(),
      singularityRequestId,
      response.getStatusCode()
    );
    Optional<BaragonServiceState> maybeBaragonServiceState = Optional.ofNullable(
      objectMapper.readValue(response.getResponseBodyAsBytes(), BaragonServiceState.class)
    );
    return maybeBaragonServiceState
      .map(BaragonServiceState::getUpstreams)
      .orElse(Collections.emptyList())
      .stream()
      .map(LoadBalancerUpstream::fromBaragonUpstream)
      .collect(Collectors.toList());
  }

  private String getLoadBalancerUri(LoadBalancerRequestId loadBalancerRequestId) {
    return String.format(OPERATION_URI, loadBalancerUri, loadBalancerRequestId);
  }

  private void addAllQueryParams(
    BoundRequestBuilder boundRequestBuilder,
    Map<String, String> queryParams
  ) {
    for (Map.Entry<String, String> entry : queryParams.entrySet()) {
      boundRequestBuilder.addQueryParam(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public SingularityLoadBalancerUpdate getState(
    LoadBalancerRequestId loadBalancerRequestId
  ) {
    final String uri = getLoadBalancerUri(loadBalancerRequestId);

    final BoundRequestBuilder requestBuilder = httpClient.prepareGet(uri);

    loadBalancerQueryParams.ifPresent(
      stringStringMap -> addAllQueryParams(requestBuilder, stringStringMap)
    );

    return sendRequestWrapper(
      loadBalancerRequestId,
      LoadBalancerMethod.CHECK_STATE,
      requestBuilder.build(),
      BaragonRequestState.UNKNOWN
    );
  }

  private BaragonResponse readResponse(Response response) {
    try {
      return objectMapper.readValue(
        response.getResponseBodyAsBytes(),
        BaragonResponse.class
      );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private SingularityLoadBalancerUpdate sendRequestWrapper(
    LoadBalancerRequestId loadBalancerRequestId,
    LoadBalancerMethod method,
    Request request,
    BaragonRequestState onFailure
  ) {
    final long start = System.currentTimeMillis();
    final LoadBalancerUpdateHolder result = sendRequest(
      loadBalancerRequestId,
      request,
      onFailure
    );

    if (
      (
        method != LoadBalancerMethod.CHECK_STATE &&
        method != LoadBalancerMethod.PRE_ENQUEUE
      ) &&
      result.state == BaragonRequestState.FAILED &&
      result.message.orElse("").contains(ALREADY_ENQUEUED_ERROR)
    ) {
      LOG.info(
        "Baragon request {} already in the queue, will fetch current state instead",
        loadBalancerRequestId
      );
      return sendRequestWrapper(
        loadBalancerRequestId,
        LoadBalancerMethod.CHECK_STATE,
        request,
        onFailure
      );
    }

    LOG.debug(
      "LB {} request {} had result {} after {}",
      request.getMethod(),
      loadBalancerRequestId,
      result,
      JavaUtils.duration(start)
    );
    return new SingularityLoadBalancerUpdate(
      LoadBalancerRequestState.fromBaragonRequestState(result.state),
      loadBalancerRequestId,
      result.message,
      start,
      method,
      Optional.of(request.getUrl())
    );
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

  private SingularityLoadBalancerUpdate sendLoadBalancerRequest(
    LoadBalancerRequestId loadBalancerRequestId,
    BaragonRequest loadBalancerRequest,
    LoadBalancerMethod method
  ) {
    try {
      LOG.trace("Preparing to send request {}", loadBalancerRequest);

      final BoundRequestBuilder requestBuilder = httpClient
        .preparePost(loadBalancerUri)
        .addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
        .setBody(objectMapper.writeValueAsBytes(loadBalancerRequest));

      loadBalancerQueryParams.ifPresent(
        stringStringMap -> addAllQueryParams(requestBuilder, stringStringMap)
      );

      return sendRequestWrapper(
        loadBalancerRequestId,
        method,
        requestBuilder.build(),
        BaragonRequestState.FAILED
      );
    } catch (IOException e) {
      return new SingularityLoadBalancerUpdate(
        LoadBalancerRequestState.UNKNOWN,
        loadBalancerRequestId,
        Optional.of(e.getMessage()),
        System.currentTimeMillis(),
        method,
        Optional.of(loadBalancerUri)
      );
    }
  }

  private LoadBalancerUpdateHolder sendRequest(
    LoadBalancerRequestId loadBalancerRequestId,
    Request request,
    BaragonRequestState onFailure
  ) {
    try {
      LOG.trace(
        "Sending LB {} request for {} to {}",
        request.getMethod(),
        loadBalancerRequestId,
        request.getUrl()
      );

      ListenableFuture<Response> future = httpClient.executeRequest(request);

      Response response = future.get(loadBalancerTimeoutMillis, TimeUnit.MILLISECONDS);

      LOG.trace(
        "LB {} request {} returned with code {}",
        request.getMethod(),
        loadBalancerRequestId,
        response.getStatusCode()
      );

      if (response.getStatusCode() == 504) {
        return new LoadBalancerUpdateHolder(
          BaragonRequestState.UNKNOWN,
          Optional.of(
            String.format(
              "LB %s request %s timed out",
              request.getMethod(),
              loadBalancerRequestId
            )
          )
        );
      } else if (!JavaUtils.isHttpSuccess(response.getStatusCode())) {
        String body = response.getResponseBody();
        LOG.info(
          "LB {} request {} failed with code {}: {}",
          request.getMethod(),
          loadBalancerRequestId,
          response.getStatusCode(),
          body
        );
        return new LoadBalancerUpdateHolder(
          onFailure,
          Optional.of(
            String.format("Response status code %s: %s", response.getStatusCode(), body)
          )
        );
      }

      BaragonResponse lbResponse = readResponse(response);

      return new LoadBalancerUpdateHolder(
        lbResponse.getLoadBalancerState(),
        lbResponse.getMessage().toJavaUtil()
      );
    } catch (TimeoutException te) {
      LOG.trace(
        "LB {} request {} timed out after waiting {}",
        request.getMethod(),
        loadBalancerRequestId,
        JavaUtils.durationFromMillis(loadBalancerTimeoutMillis)
      );
      return new LoadBalancerUpdateHolder(
        BaragonRequestState.UNKNOWN,
        Optional.of(
          String.format(
            "Timed out after %s",
            JavaUtils.durationFromMillis(loadBalancerTimeoutMillis)
          )
        )
      );
    } catch (Throwable t) {
      LOG.error(
        "LB {} request {} to {} threw error",
        request.getMethod(),
        loadBalancerRequestId,
        request.getUrl(),
        t
      );
      return new LoadBalancerUpdateHolder(
        BaragonRequestState.UNKNOWN,
        Optional.of(
          String.format("Exception %s - %s", t.getClass().getSimpleName(), t.getMessage())
        )
      );
    }
  }

  @Override
  public SingularityLoadBalancerUpdate makeAndSendLoadBalancerRequest(
    LoadBalancerRequestId loadBalancerRequestId,
    List<LoadBalancerUpstream> addUpstreams,
    List<LoadBalancerUpstream> removeUpstreams,
    SingularityDeploy deploy,
    SingularityRequest request
  ) {
    final List<String> serviceOwners = request
      .getOwners()
      .orElse(Collections.emptyList());
    final Set<String> loadBalancerGroups = deploy
      .getLoadBalancerGroups()
      .orElse(Collections.emptySet());

    boolean enableDNSPreResolution;
    if (skipDNSPreResolutionForRequests.contains(request.getId())) {
      enableDNSPreResolution = false;
    } else {
      enableDNSPreResolution = preResolveUpstreamDNS;
    }

    final BaragonService lbService = new BaragonService(
      deploy.getLoadBalancerServiceIdOverride().orElse(request.getId()),
      serviceOwners,
      deploy.getServiceBasePath().get(),
      deploy.getLoadBalancerAdditionalRoutes().orElse(Collections.<String>emptyList()),
      loadBalancerGroups,
      deploy.getLoadBalancerOptions().orElse(null),
      com.google.common.base.Optional.fromJavaUtil(deploy.getLoadBalancerTemplate()),
      deploy.getLoadBalancerDomains().orElse(Collections.emptySet()),
      com.google.common.base.Optional.absent(),
      Collections.emptySet(),
      enableDNSPreResolution
    );
    final BaragonRequest loadBalancerRequest = new BaragonRequest(
      loadBalancerRequestId.toString(),
      lbService,
      addUpstreams
        .stream()
        .map(u -> singularityToBaragonUpstream(u, loadBalancerRequestId.toString()))
        .collect(Collectors.toList()),
      removeUpstreams
        .stream()
        .map(u -> singularityToBaragonUpstream(u, loadBalancerRequestId.toString()))
        .collect(Collectors.toList()),
      Collections.emptyList(),
      com.google.common.base.Optional.absent(),
      com.google.common.base.Optional.of(RequestAction.UPDATE),
      false,
      false,
      false,
      true
    );
    return sendLoadBalancerRequest(
      loadBalancerRequestId,
      loadBalancerRequest,
      LoadBalancerMethod.ENQUEUE
    );
  }

  private UpstreamInfo singularityToBaragonUpstream(
    LoadBalancerUpstream loadBalancerUpstream,
    String loadBalancerRequestId
  ) {
    return new UpstreamInfo(
      loadBalancerUpstream.getUpstream(),
      com.google.common.base.Optional.of(loadBalancerRequestId),
      com.google.common.base.Optional.fromJavaUtil(loadBalancerUpstream.getRackId()),
      com.google.common.base.Optional.absent(),
      com.google.common.base.Optional.of(loadBalancerUpstream.getGroup())
    );
  }

  @Override
  public SingularityLoadBalancerUpdate cancel(
    LoadBalancerRequestId loadBalancerRequestId
  ) {
    final String uri = getLoadBalancerUri(loadBalancerRequestId);

    final BoundRequestBuilder requestBuilder = httpClient.prepareDelete(uri);

    loadBalancerQueryParams.ifPresent(
      stringStringMap -> addAllQueryParams(requestBuilder, stringStringMap)
    );

    return sendRequestWrapper(
      loadBalancerRequestId,
      LoadBalancerMethod.CANCEL,
      requestBuilder.build(),
      BaragonRequestState.UNKNOWN
    );
  }

  @Override
  public SingularityLoadBalancerUpdate delete(
    LoadBalancerRequestId loadBalancerRequestId,
    String requestId,
    Set<String> loadBalancerGroups,
    String serviceBasePath
  ) {
    final BaragonService lbService = new BaragonService(
      requestId,
      Collections.emptyList(),
      serviceBasePath,
      loadBalancerGroups,
      Collections.emptyMap()
    );
    final BaragonRequest loadBalancerRequest = new BaragonRequest(
      loadBalancerRequestId.toString(),
      lbService,
      Collections.emptyList(),
      Collections.emptyList(),
      Collections.emptyList(),
      com.google.common.base.Optional.absent(),
      com.google.common.base.Optional.of(RequestAction.DELETE)
    );

    return sendLoadBalancerRequest(
      loadBalancerRequestId,
      loadBalancerRequest,
      LoadBalancerMethod.DELETE
    );
  }
}
