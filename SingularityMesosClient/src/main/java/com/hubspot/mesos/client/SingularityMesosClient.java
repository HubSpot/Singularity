package com.hubspot.mesos.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpRequest.Options;
import com.hubspot.horizon.HttpResponse;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.json.MesosAgentMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosAgentStateObject;
import com.hubspot.mesos.json.MesosMasterMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosSlaveMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosSlaveStateObject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityMesosClient implements MesosClient {

  public static final String DEFAULT_HTTP_CLIENT_NAME = "mesos.http.client";
  public static final String SHORT_TIMEOUT_HTTP_CLIENT_NAME =
    "mesos.http.client.short.timeout";
  public static final String MESOS_CREDENTIALS = "mesos.http.client.credentials";

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosClient.class);

  private static final String MASTER_STATE_FORMAT = "http://%s/master/state";
  private static final String MESOS_AGENT_JSON_URL = "http://%s:5051/slave(1)/state";
  private static final String MESOS_AGENT_STATISTICS_URL =
    "http://%s:5051/monitor/statistics";
  private static final String MESOS_MASTER_METRICS_SNAPSHOT_URL =
    "http://%s/metrics/snapshot";
  private static final String MESOS_AGENT_METRICS_SNAPSHOT_URL =
    "http://%s:5051/metrics/snapshot";

  private static final TypeReference<List<MesosTaskMonitorObject>> TASK_MONITOR_TYPE_REFERENCE = new TypeReference<List<MesosTaskMonitorObject>>() {};

  private final HttpClient httpClient;
  private final HttpClient shortTimeoutHttpClient;
  private final UserAndPassword credentials;

  @Inject
  public SingularityMesosClient(
    @Named(DEFAULT_HTTP_CLIENT_NAME) HttpClient httpClient,
    @Named(SHORT_TIMEOUT_HTTP_CLIENT_NAME) HttpClient shortTimeoutHttpClient,
    @Named(MESOS_CREDENTIALS) UserAndPassword credentials
  ) {
    this.httpClient = httpClient;
    this.shortTimeoutHttpClient = shortTimeoutHttpClient;
    this.credentials = credentials;
  }

  @Override
  public String getMasterUri(String hostnameAndPort) {
    return String.format(MASTER_STATE_FORMAT, hostnameAndPort);
  }

  @Override
  public String getMasterMetricsSnapshotUri(String hostnameAndPort) {
    return String.format(MESOS_MASTER_METRICS_SNAPSHOT_URL, hostnameAndPort);
  }

  private HttpResponse getFromMesos(String uri, boolean useShortTimeout) {
    HttpClient currentHttpClient = useShortTimeout ? shortTimeoutHttpClient : httpClient;
    HttpResponse response = null;

    final long start = System.currentTimeMillis();

    LOG.debug("Fetching {} from mesos", uri);

    try {
      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().setUrl(uri);
      if (credentials.hasCredentials()) {
        requestBuilder.addBasicAuth(credentials.getUser(), credentials.getPassword());
      }
      response = currentHttpClient.execute(requestBuilder.build(), new Options());

      LOG.debug(
        "Response {} - {} after {}",
        response.getStatusCode(),
        uri,
        JavaUtils.duration(start)
      );
    } catch (Exception e) {
      throw new MesosClientException(
        String.format("Exception fetching %s after %s", uri, JavaUtils.duration(start)),
        e
      );
    }

    if (!response.isSuccess()) {
      throw new MesosClientException(
        String.format("Invalid response code from %s : %s", uri, response.getStatusCode())
      );
    }

    return response;
  }

  private <T> T getFromMesos(String uri, Class<T> clazz) {
    return getFromMesos(uri, clazz, false);
  }

  private <T> T getFromMesos(String uri, Class<T> clazz, boolean useShortTimeout) {
    HttpResponse response = getFromMesos(uri, useShortTimeout);

    try {
      return response.getAs(clazz);
    } catch (Exception e) {
      throw new MesosClientException(
        String.format("Couldn't deserialize %s from %s", clazz.getSimpleName(), uri),
        e
      );
    }
  }

  @Override
  public MesosMasterStateObject getMasterState(String uri) {
    return getFromMesos(uri, MesosMasterStateObject.class);
  }

  @Override
  public MesosMasterMetricsSnapshotObject getMasterMetricsSnapshot(String uri) {
    return getFromMesos(uri, MesosMasterMetricsSnapshotObject.class);
  }

  @Override
  @Deprecated
  public MesosSlaveMetricsSnapshotObject getSlaveMetricsSnapshot(
    String hostname,
    boolean useShortTimeout
  ) {
    return getFromMesos(
      String.format(MESOS_AGENT_METRICS_SNAPSHOT_URL, hostname),
      MesosSlaveMetricsSnapshotObject.class,
      useShortTimeout
    );
  }

  @Override
  public MesosAgentMetricsSnapshotObject getAgentMetricsSnapshot(
    String hostname,
    boolean useShortTimeout
  ) {
    return getFromMesos(
      String.format(MESOS_AGENT_METRICS_SNAPSHOT_URL, hostname),
      MesosAgentMetricsSnapshotObject.class,
      useShortTimeout
    );
  }

  @Override
  @Deprecated
  public String getSlaveUri(String hostname) {
    return String.format(MESOS_AGENT_JSON_URL, hostname);
  }

  @Override
  public String getAgentUri(String hostname) {
    return String.format(MESOS_AGENT_JSON_URL, hostname);
  }

  @Override
  @Deprecated
  public MesosSlaveStateObject getSlaveState(String uri) {
    return getFromMesos(uri, MesosSlaveStateObject.class);
  }

  @Override
  public MesosAgentStateObject getAgentState(String uri) {
    return getFromMesos(uri, MesosAgentStateObject.class);
  }

  @Override
  public List<MesosTaskMonitorObject> getAgentResourceUsage(
    String hostname,
    boolean useShortTimeout
  ) {
    return getSlaveResourceUsage(hostname, useShortTimeout);
  }

  @Override
  @Deprecated
  public List<MesosTaskMonitorObject> getSlaveResourceUsage(
    String hostname,
    boolean useShortTimeout
  ) {
    final String uri = String.format(MESOS_AGENT_STATISTICS_URL, hostname);

    HttpResponse response = getFromMesos(uri, useShortTimeout);

    try {
      return response.getAs(TASK_MONITOR_TYPE_REFERENCE);
    } catch (Exception e) {
      throw new MesosClientException(
        String.format("Unable to deserialize task monitor object from %s", uri),
        e
      );
    }
  }
}
