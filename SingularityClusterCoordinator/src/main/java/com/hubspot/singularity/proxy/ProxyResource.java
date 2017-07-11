package com.hubspot.singularity.proxy;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.horizon.AsyncHttpClient;
import com.hubspot.horizon.Header;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpRequest.Method;
import com.hubspot.horizon.HttpResponse;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.hubspot.singularity.config.DataCenter;

import io.dropwizard.server.SimpleServerFactory;

public class ProxyResource {
  private static final Logger LOG = LoggerFactory.getLogger(ProxyResource.class);

  private AsyncHttpClient httpClient;
  private ClusterCoordinatorConfiguration configuration;
  private ObjectMapper objectMapper;
  protected DataCenterLocator dataCenterLocator;
  private String contextPath;

  public ProxyResource() {}

  @Inject
  void injectProxyDeps(ClusterCoordinatorConfiguration configuration,
                       @Named(SingularityClusterCoodinatorResourcesModule.ASYNC_HTTP_CLIENT) AsyncHttpClient httpClient,
                       ObjectMapper objectMapper,
                       DataCenterLocator dataCenterLocator) {
    this.configuration = configuration;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.dataCenterLocator = dataCenterLocator;
    String baseContextPath = ((SimpleServerFactory) configuration.getServerFactory()).getApplicationContextPath();
    if (baseContextPath.startsWith("/")) {
      baseContextPath = baseContextPath.substring(1, baseContextPath.length());
    }
    if (baseContextPath.endsWith("/")) {
      baseContextPath = baseContextPath.substring(0, baseContextPath.length() -1);
    }
    this.contextPath = baseContextPath + ApiPaths.API_BASE_PATH;
  }

  /*
   * For items where the dataCenter is part of the object, or where a full list is desired.
   * Collect and merge results from each configured dataCenter
   */
  public <T> Response getMergedListResult(HttpServletRequest request) {
    return getMergedListResult(request, null);
  }

  public <T> Response getMergedListResult(HttpServletRequest request, T body) {
    Map<String, String> headers = getHeaders(request);
    Map<String, String> params = getParams(request);

    // TODO - parallelize
    List<Object> combined = Lists.newArrayList();
    ResponseBuilder builder = null;

    for (DataCenter dataCenter : configuration.getDataCenters()) {
      try {
        HttpResponse response = proxyAndGetResponse(dataCenter, request, body, headers, params);
        if (builder == null) {
          builder = Response.status(response.getStatusCode());
          for (Header header : response.getHeaders()) {
            builder.header(header.getName(), header.getValue());
          }
        }

        List<Object> content = response.getAs(new TypeReference<List<Object>>() {});
        LOG.trace("Data center {} had response {}", dataCenter.getName(), content);

        combined.addAll(content);
      } catch (RuntimeException re) {
        LOG.error("Could not get data from dataCenter {}, omitting", dataCenter.getName(), re);
      }
    }

    if (builder != null) {
      return builder.entity(combined).build();
    } else {
      throw new WebApplicationException("Got no results", 500);
    }
  }

  /*
   * Route a request to a particular dataCenter using the requestId to locate the correct Singularity cluster
   */
  public Response routeByRequestId(HttpServletRequest request, String requestId) {
    return routeByRequestId(request, requestId, null);
  }

  public <T> Response routeByRequestId(HttpServletRequest request, String requestId, T body) {
    Map<String, String> headers = getHeaders(request);
    Map<String, String> params = getParams(request);

    DataCenter dataCenter = getDataCenterForRequest(requestId);

    return toResponse(proxyAndGetResponse(dataCenter, request, body, headers, params));
  }

  /*
   * Route a request to a particular dataCenter using the request group Id to locate the correct Singularity cluster
   */
  Response routeByRequestGroupId(HttpServletRequest request, String requestGroupId) {
    Map<String, String> headers = getHeaders(request);
    Map<String, String> params = getParams(request);

    DataCenter dataCenter = getDataCenterForRequestGroup(requestGroupId);

    return toResponse(proxyAndGetResponse(dataCenter, request, null, headers, params));
  }

  /*
   * Route a request to a particular dataCenter using the slaveId/hostname to locate the correct Singularity cluster
   */
  Response routeBySlaveId(HttpServletRequest request, String slaveId) {
    Map<String, String> headers = getHeaders(request);
    Map<String, String> params = getParams(request);

    DataCenter dataCenter = getDataCenterForSlaveId(slaveId);

    return toResponse(proxyAndGetResponse(dataCenter, request, null, headers, params));
  }

  Response routeByHostname(HttpServletRequest request, String hostname) {
    Map<String, String> headers = getHeaders(request);
    Map<String, String> params = getParams(request);

    DataCenter dataCenter = getDataCenterForSlaveHostname(hostname);

    return toResponse(proxyAndGetResponse(dataCenter, request, null, headers, params));
  }

  /*
   * Route a request to a particular dataCenter using the rack ID to locate the correct Singularity cluster
   */
  Response routeByRackId(HttpServletRequest request, String rackId) {
    Map<String, String> headers = getHeaders(request);
    Map<String, String> params = getParams(request);

    DataCenter dataCenter = getDataCenterForRackId(rackId);

    return toResponse(proxyAndGetResponse(dataCenter, request, null, headers, params));
  }

  /*
   * Route a request to a particular dataCenter by name, failing if it is not present
   */
  <T> Response routeByDataCenter(HttpServletRequest request, String dataCenterName, T body) {
    Map<String, String> headers = getHeaders(request);
    Map<String, String> params = getParams(request);

    DataCenter dataCenter = getDataCenter(dataCenterName);

    return toResponse(proxyAndGetResponse(dataCenter, request, body, headers, params));
  }

  /*
   * Route to the default Singularity cluster
   */
  Response routeToDefaultDataCenter(HttpServletRequest request) {
    return routeToDefaultDataCenter(request, null);
  }

  <T> Response routeToDefaultDataCenter(HttpServletRequest request, T body) {
    Map<String, String> headers = getHeaders(request);
    Map<String, String> params = getParams(request);

    DataCenter dataCenter = configuration.getDataCenters().get(0);

    return toResponse(proxyAndGetResponse(dataCenter, request, body, headers, params));
  }

  /*
   * Working with data centers
   */
  private String getHost(DataCenter dataCenter) {
    return dataCenterLocator.getHost(dataCenter);
  }

  private DataCenter getDataCenterForRequest(String requestId) {
    return dataCenterLocator.getDataCenterForRequest(requestId);
  }

  private DataCenter getDataCenterForRequestGroup(String requestGroupId) {
    return dataCenterLocator.getDataCenterForRequestGroup(requestGroupId);
  }

  private DataCenter getDataCenterForSlaveId(String slaveId) {
    return dataCenterLocator.getDataCenterForSlaveId(slaveId);
  }

  private DataCenter getDataCenterForSlaveHostname(String hostname) {
    return dataCenterLocator.getDataCenterForSlaveHostname(hostname);
  }

  private DataCenter getDataCenterForRackId(String rackId) {
    return dataCenterLocator.getDataCenterForRackId(rackId);
  }

  private DataCenter getDataCenter(String name) {
    return dataCenterLocator.getDataCenter(name);
  }

  /*
   * Generic methods for proxying requests
   */
  private <T> HttpResponse proxyAndGetResponse(DataCenter dc, HttpServletRequest request, T body, Map<String, String> headers, Map<String, String> params) {
    String fullPath = request.getContextPath() + request.getPathInfo();
    String url = String.format("%s://%s%s", dc.getScheme(), getHost(dc), fullPath.replace(contextPath, dc.getContextPath()));

    LOG.debug("Proxying {} to: {}", fullPath, url);
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .setMethod(Method.valueOf(request.getMethod()))
        .setUrl(url);
    try {
      if (body != null) {
        requestBuilder.setBody(objectMapper.writeValueAsBytes(body));
        LOG.trace("Added body {} to request", body);
      }
    } catch (JsonProcessingException jpe) {
      LOG.error("Could not write body from object {}", body);
      throw new WebApplicationException(jpe, 500);
    }
    headers.forEach(requestBuilder::addHeader);
    params.forEach((k, v) -> requestBuilder.setQueryParam(k).to(v));

    try {
      return httpClient.execute(requestBuilder.build()).get();
    } catch (InterruptedException|ExecutionException ioe) {
      throw new WebApplicationException(ioe);
    }
  }

  private <T, Q> T proxyAndGetResponseAs(DataCenter dc, HttpServletRequest request, Q body, TypeReference<T> clazz, Map<String, String> headers, Map<String, String> params) {
    HttpResponse response = proxyAndGetResponse(dc, request, body, headers, params);
    if (response.getStatusCode() > 399) {
      throw new WebApplicationException(response.getAsString(), response.getStatusCode());
    } else {
      try {
        T object = response.getAs(clazz);
        LOG.trace("Got response: {}", object);
        return object;
      } catch (RuntimeException e) {
        LOG.error("Could not parse response json", e);
        throw new WebApplicationException(e);
      }
    }
  }

  private Map<String, String> getHeaders(HttpServletRequest request) {
    Map<String, String> headers = new HashMap<>();
    Enumeration<String> headerNames = request.getHeaderNames();
    if (headerNames != null) {
      while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();
        headers.put(headerName, request.getHeader(headerName));
      }
    }
    LOG.trace("Found headers: {}", headers);
    return headers;
  }

  private Map<String, String> getParams(HttpServletRequest request) {
    Map<String, String> params = new HashMap<>();
    Enumeration<String> parameterNames = request.getParameterNames();
    if (parameterNames != null) {
      while (parameterNames.hasMoreElements()) {
        String parameterName = parameterNames.nextElement();
        params.put(parameterName, request.getParameter(parameterName));
      }
    }
    LOG.trace("Found query params: {}", params);
    return params;
  }

  private Response toResponse(HttpResponse original) {
    ResponseBuilder builder = Response.status(original.getStatusCode())
        .entity(original.getAsString());
    original.getHeaders().forEach((h) -> builder.header(h.getName(), h.getValue()));
    return builder.build();
  }
}
