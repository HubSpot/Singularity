package com.hubspot.singularity.mesos;

import org.apache.mesos.Protos.MasterInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosSlaveStateObject;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class MesosClient {
  
  private final static String MASTER_STATE_FORMAT = "http://%s/master/state.json";
  private final static String MESOS_SLAVE_JSON_URL = "http://%s:5051/slave(1)/state.json";
  
  private final AsyncHttpClient asyncHttpClient;
  private final ObjectMapper objectMapper;
  
  @Inject
  public MesosClient(AsyncHttpClient asyncHttpClient, ObjectMapper objectMapper) {
    this.asyncHttpClient = asyncHttpClient;
    this.objectMapper = objectMapper;
  }

  public String getMasterUri(MasterInfo masterInfo) {
    return String.format(MASTER_STATE_FORMAT, MesosUtils.getMasterHostAndPort(masterInfo));
  }
  
  @SuppressWarnings("serial")
  public static class MesosClientException extends RuntimeException {

    public MesosClientException(String message) {
      super(message);
    }

    public MesosClientException(String message, Throwable cause) {
      super(message, cause);
    }
    
  }
  
  private Response getResponse(String uri) {
    Response response = null;
    
    try {
      response = asyncHttpClient.prepareGet(uri).execute().get();
    } catch (Exception e) {
      throw new MesosClientException("While fetching: " + uri, e);
    }
    
    if (response.getStatusCode() < 200 || response.getStatusCode() > 299) {
      throw new MesosClientException(String.format("Invalid response code from %s : %s", uri, response.getStatusCode()));
    }
    
    return response;
  }
  
  public MesosMasterStateObject getMasterState(String uri) {
    Response response = getResponse(uri);
    
    try {
      return objectMapper.readValue(response.getResponseBodyAsStream(), MesosMasterStateObject.class);
    } catch (Exception e) {
      throw new MesosClientException("Invalid response from uri: " + uri, e);
    }
  }
  
  public String getSlaveUri(String hostname) {
    return String.format(MESOS_SLAVE_JSON_URL, hostname);
  }
  
  public MesosSlaveStateObject getSlaveState(String uri) {
    Response response = getResponse(uri);
    
    try {
      return objectMapper.readValue(response.getResponseBodyAsStream(), MesosSlaveStateObject.class);
    } catch (Exception e) {
      throw new MesosClientException("Invalid response from uri: " + uri, e);
    }
  }
  
}
