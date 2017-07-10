package com.hubspot.singularity.proxy;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.hubspot.singularity.config.DataCenter;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

public class ProxyResource {
  private static final Logger LOG = LoggerFactory.getLogger(ProxyResource.class);

  private final AsyncHttpClient httpClient;
  private final ClusterCoordinatorConfiguration configuration;
  private final ObjectMapper objectMapper;
  private final DataCenterLocator dataCenterLocator;

  @Inject
  public ProxyResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper, DataCenterLocator dataCenterLocator) {
    this.configuration = configuration;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.dataCenterLocator = dataCenterLocator;
  }

  /*
   * For items where the dataCenter is part of the object, or where a full list is desired.
   * Collect and merge results from each configured dataCenter
   */
  public <T> List<T> getMergedListResult(HttpServletRequest request, TypeReference<List<T>> clazz) {
    return getMergedListResult(request, null, clazz);
  }

  public <T, Q> List<T> getMergedListResult(HttpServletRequest request, Q body, TypeReference<List<T>> clazz) {
    Map<String, String> headers = getHeaders(request);
    Map<String, String> params = getParams(request);

    return configuration.getDataCenters().parallelStream()
        .map((dc) -> proxyRequest(dc, request, body, clazz, headers, params))
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  /*
   * Route a request to a particular dataCenter using the requestId to locate the correct Singularity cluster
   */
  public <T> T routeByRequestId(HttpServletRequest request, String requestId, TypeReference<T> clazz) {
    return routeByRequestId(request, requestId, null, clazz);
  }

  public <T, Q> T routeByRequestId(HttpServletRequest request, String requestId, Q body, TypeReference<T> clazz) {
    Map<String, String> headers = getHeaders(request);
    Map<String, String> params = getParams(request);

    DataCenter dataCenter = getDataCenterForRequest(requestId);

    return proxyRequest(dataCenter, request, body, clazz, headers, params);
  }

  /*
   * Route a request to a particular dataCenter using the request group Id to locate the correct Singularity cluster
   */
  <T> T routeByRequestGroupId(HttpServletRequest request, String requestGroupId, TypeReference<T> clazz) {
    Map<String, String> headers = getHeaders(request);
    Map<String, String> params = getParams(request);

    DataCenter dataCenter = getDataCenterForRequestGroup(requestGroupId);

    return proxyRequest(dataCenter, request, null, clazz, headers, params);
  }

  /*
   * Route a request to a particular dataCenter using the slaveId/hostname to locate the correct Singularity cluster
   */
  <T> T routeBySlaveId(HttpServletRequest request, String slaveId, TypeReference<T> clazz) {
    Map<String, String> headers = getHeaders(request);
    Map<String, String> params = getParams(request);

    DataCenter dataCenter = getDataCenterForSlaveId(slaveId);

    return proxyRequest(dataCenter, request, null, clazz, headers, params);
  }

  <T> T routeByHostname(HttpServletRequest request, String hostname, TypeReference<T> clazz) {
    Map<String, String> headers = getHeaders(request);
    Map<String, String> params = getParams(request);

    DataCenter dataCenter = getDataCenterForSlaveHostname(hostname);

    return proxyRequest(dataCenter, request, null, clazz, headers, params);
  }

  /*
   * Route a request to a particular dataCenter using the rack ID to locate the correct Singularity cluster
   */
  <T> T routeByRackId(HttpServletRequest request, String rackId, TypeReference<T> clazz) {
    Map<String, String> headers = getHeaders(request);
    Map<String, String> params = getParams(request);

    DataCenter dataCenter = getDataCenterForRackId(rackId);

    return proxyRequest(dataCenter, request, null, clazz, headers, params);
  }

  /*
   * Route a request to a particular dataCenter by name, failing if it is not present
   */
  <T, Q> T routeByDataCenter(HttpServletRequest request, String dataCenterName, Q body, TypeReference<T> clazz) {
    Map<String, String> headers = getHeaders(request);
    Map<String, String> params = getParams(request);

    DataCenter dataCenter = getDataCenter(dataCenterName);

    return proxyRequest(dataCenter, request, body, clazz, headers, params);
  }

  /*
   * Route to the default Singularity cluster
   */
  <T> T routeToDefaultDataCenter(HttpServletRequest request, TypeReference<T> clazz) {
    return routeToDefaultDataCenter(request, null, clazz);
  }

  <T, Q> T routeToDefaultDataCenter(HttpServletRequest request, Q body, TypeReference<T> clazz) {
    Map<String, String> headers = getHeaders(request);
    Map<String, String> params = getParams(request);

    DataCenter dataCenter = configuration.getDataCenters().get(0);

    return proxyRequest(dataCenter, request, body, clazz, headers, params);
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
  private <T, Q> T proxyRequest(DataCenter dc, HttpServletRequest request, Q body, TypeReference<T> clazz, Map<String, String> headers, Map<String, String> params) {
    String url = String.format("%s://%s%s%s", dc.getScheme(), getHost(dc), request.getContextPath(), request.getPathInfo());
    BoundRequestBuilder requestBuilder = startRequestBuilder(request.getMethod(), url);
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
    params.forEach(requestBuilder::addQueryParameter);

    Response response;
    try {
      response = requestBuilder.execute().get();
    } catch (IOException|InterruptedException|ExecutionException ioe) {
      LOG.error("Exception while processing request to {}", url, ioe);
      return null;
    }

    try {

      if (response.getStatusCode() > 399) {
        LOG.error("Request to {} failed ({}:{})", url, response.getStatusCode(), response.getResponseBody(Charsets.UTF_8.toString()));
        return null;
      } else {
        return objectMapper.readValue(response.getResponseBodyAsStream(), clazz);
      }
    } catch (IOException ioe) {
      try {
        LOG.warn("Bad response body: {}", response.getResponseBody(Charsets.UTF_8.toString()));
      } catch (IOException io) {
        LOG.error("Could not print response", io);
      }
      LOG.error("Request succeeded with status {}, but could not interpret response ({})", response.getStatusCode(), ioe);
    }

    return null;
  }

  private BoundRequestBuilder startRequestBuilder(String method, String url) {
    switch (method.toUpperCase()) {
      case "GET":
        return httpClient.prepareGet(url);
      case "POST":
        return httpClient.preparePost(url);
      case "PUT":
        return httpClient.preparePut(url);
      case "DELETE":
        return httpClient.prepareDelete(url);
      default:
        throw new WebApplicationException(String.format("Not meant to proxy request of method %s", method), 400);
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
    return headers;
  }

  private Map<String, String> getParams(HttpServletRequest request) {
    Map<String, String> params = new HashMap<>();
    Enumeration<String> parameterNames = request.getParameterNames();
    if (parameterNames != null) {
      while (parameterNames.hasMoreElements()) {
        String parameterName = parameterNames.nextElement();
        params.put(parameterName, request.getParameter(parameterName));
        LOG.trace("Copied query param {}={}", parameterName, request.getParameter(parameterName));
      }
    }
    return params;
  }
}
