package com.hubspot.singularity.proxy;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
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
  protected ClusterCoordinatorConfiguration configuration;
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
    List<Param> headers = getHeaders(request);
    List<Param> params = getParams(request);

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
        LOG.trace("Data center {} had response {}", dataCenter.getName(), response.getStatusCode());

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
    List<Param> headers = getHeaders(request);
    List<Param> params = getParams(request);

    DataCenter dataCenter = getDataCenterForRequest(requestId, request.getMethod().toUpperCase().equals("GET"));

    return toResponse(proxyAndGetResponse(dataCenter, request, body, headers, params));
  }

  /*
   * Route a request to a particular dataCenter using the request group Id to locate the correct Singularity cluster
   */
  Response routeByRequestGroupId(HttpServletRequest request, String requestGroupId) {
    List<Param> headers = getHeaders(request);
    List<Param> params = getParams(request);

    DataCenter dataCenter = getDataCenterForRequestGroup(requestGroupId, request.getMethod().toUpperCase().equals("GET"));

    return toResponse(proxyAndGetResponse(dataCenter, request, null, headers, params));
  }

  /*
   * Route a request to a particular dataCenter using the slaveId/hostname to locate the correct Singularity cluster
   */
  Response routeBySlaveId(HttpServletRequest request, String slaveId) {
    return routeBySlaveId(request, slaveId, null);
  }

  <T> Response routeBySlaveId(HttpServletRequest request, String slaveId, T body) {
    List<Param> headers = getHeaders(request);
    List<Param> params = getParams(request);

    DataCenter dataCenter = getDataCenterForSlaveId(slaveId, request.getMethod().toUpperCase().equals("GET"));

    return toResponse(proxyAndGetResponse(dataCenter, request, body, headers, params));
  }

  Response routeByHostname(HttpServletRequest request, String hostname) {
    List<Param> headers = getHeaders(request);
    List<Param> params = getParams(request);

    DataCenter dataCenter = getDataCenterForSlaveHostname(hostname, request.getMethod().toUpperCase().equals("GET"));

    return toResponse(proxyAndGetResponse(dataCenter, request, null, headers, params));
  }

  /*
   * Route a request to a particular dataCenter using the rack ID to locate the correct Singularity cluster
   */
  Response routeByRackId(HttpServletRequest request, String rackId) {
    return routeByRackId(request, rackId, null);
  }

  <T> Response routeByRackId(HttpServletRequest request, String rackId, T body) {
    List<Param> headers = getHeaders(request);
    List<Param> params = getParams(request);

    DataCenter dataCenter = getDataCenterForRackId(rackId, request.getMethod().toUpperCase().equals("GET"));

    return toResponse(proxyAndGetResponse(dataCenter, request, body, headers, params));
  }

  /*
   * Route a request to a particular dataCenter by name, failing if it is not present
   */
  <T> Response routeByDataCenter(HttpServletRequest request, String dataCenterName, T body) {
    List<Param> headers = getHeaders(request);
    List<Param> params = getParams(request);

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
    List<Param> headers = getHeaders(request);
    List<Param> params = getParams(request);

    DataCenter dataCenter = configuration.getDataCenters().get(0);

    return toResponse(proxyAndGetResponse(dataCenter, request, body, headers, params));
  }

  /*
   * Working with data centers
   */
  private String getHost(DataCenter dataCenter) {
    return dataCenterLocator.getHost(dataCenter);
  }

  private DataCenter getDataCenterForRequest(String requestId, boolean isGetRequest) {
    return dataCenterLocator.getDataCenterForRequest(requestId, isGetRequest);
  }

  private DataCenter getDataCenterForRequestGroup(String requestGroupId, boolean isGetRequest) {
    return dataCenterLocator.getDataCenterForRequestGroup(requestGroupId, isGetRequest);
  }

  private DataCenter getDataCenterForSlaveId(String slaveId, boolean isGetRequest) {
    return dataCenterLocator.getDataCenterForSlaveId(slaveId, isGetRequest);
  }

  private DataCenter getDataCenterForSlaveHostname(String hostname, boolean isGetRequest) {
    return dataCenterLocator.getDataCenterForSlaveHostname(hostname, isGetRequest);
  }

  private DataCenter getDataCenterForRackId(String rackId, boolean isGetRequest) {
    return dataCenterLocator.getDataCenterForRackId(rackId, isGetRequest);
  }

  private DataCenter getDataCenter(String name) {
    return dataCenterLocator.getDataCenter(name);
  }

  /*
   * Generic methods for proxying requests
   */
  private <T> HttpResponse proxyAndGetResponse(DataCenter dc, HttpServletRequest request, T body, List<Param> headers, List<Param> params) {
    String fullPath = request.getContextPath() + request.getPathInfo();
    String url = String.format("%s://%s%s", dc.getScheme(), getHost(dc), fullPath.replace(contextPath, dc.getContextPath()));

    LOG.debug("Proxying {} {} to: ({}) {}", request.getMethod(), fullPath, dc.getName(), url);
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .setMethod(Method.valueOf(request.getMethod()))
        .setUrl(url);
    if (body != null) {
      requestBuilder.setBody(body);
      LOG.trace("Added body {} to request", body);
    } else if (!request.getMethod().equals("GET")){
      requestBuilder.setBody(new byte[]{});
    }
    headers.forEach((h) -> requestBuilder.addHeader(h.getKey(), h.getValue()));
    params.forEach((h) -> requestBuilder.setQueryParam(h.getKey()).to(h.getValue()));

    try {
      return httpClient.execute(requestBuilder.build()).get();
    } catch (InterruptedException|ExecutionException ioe) {
      if (Throwables.getRootCause(ioe) instanceof TimeoutException) {
        throw new WebApplicationException(ioe, 503);
      } else {
        throw new WebApplicationException(ioe);
      }
    }
  }

  private List<Param> getHeaders(HttpServletRequest request) {
    List<Param> headers = new ArrayList<>();
    Enumeration<String> headerNames = request.getHeaderNames();
    if (headerNames != null) {
      while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();
        headers.add(new Param(headerName, request.getHeader(headerName)));
      }
    }
    LOG.trace("Found headers: {}", headers);
    return headers;
  }

  private List<Param> getParams(HttpServletRequest request) {
    List<Param> params = new ArrayList<>();
    for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
      for (String value : entry.getValue()) {
        params.add(new Param(entry.getKey(), value));
      }
    }
    LOG.trace("Found params {}", params);
    return params;
  }

  private Response toResponse(HttpResponse original) {
    LOG.trace("Got response {}", original.getStatusCode());
    ResponseBuilder builder = Response.status(original.getStatusCode())
        .entity(original.getAsString());
    original.getHeaders().forEach((h) -> builder.header(h.getName(), h.getValue()));
    return builder.build();
  }

  private static class Param {
    private final String key;
    private final String value;

    Param(String key, String value) {
      this.key = key;
      this.value = value;
    }

    String getKey() {
      return key;
    }

    String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return "Param{" +
          "key='" + key + '\'' +
          ", value='" + value + '\'' +
          '}';
    }
  }
}
