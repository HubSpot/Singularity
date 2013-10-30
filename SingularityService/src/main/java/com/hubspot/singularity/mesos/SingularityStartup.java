package com.hubspot.singularity.mesos;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.apache.mesos.Protos.MasterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.net.InetAddresses;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosStateObject;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class SingularityStartup {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityStartup.class);
  
  private final static String MASTER_STATE_FORMAT = "http://%s:%s/master/state.json";
  
  private final AsyncHttpClient asyncHttpClient;
  private final ObjectMapper objectMapper;
  private final SingularityRackManager rackManager;
  
  @Inject
  public SingularityStartup(AsyncHttpClient asyncHttpClient, ObjectMapper objectMapper, SingularityRackManager rackManager) {
    this.asyncHttpClient = asyncHttpClient;
    this.objectMapper = objectMapper;
    this.rackManager = rackManager;
  }
  
  private String getStateUri(MasterInfo masterInfo) {
    byte[] fromIp = ByteBuffer.allocate(4).putInt(masterInfo.getIp()).array();
   
    try {
      return String.format(MASTER_STATE_FORMAT, InetAddresses.fromLittleEndianByteArray(fromIp).getHostAddress(), masterInfo.getPort());
    } catch (UnknownHostException e) {
      throw Throwables.propagate(e);
    }
  }
  
  public void startup(MasterInfo masterInfo) {
    final String uri = getStateUri(masterInfo);
    
    LOG.info("Fetching state data from: " + uri);
    
    try {
      Response response = asyncHttpClient.prepareGet(uri).execute().get();
      
      Preconditions.checkState(response.getStatusCode() >= 200 && response.getStatusCode() < 300, "Invalid response from master %s : %s", uri, response.getStatusCode());

      MesosStateObject state = objectMapper.readValue(response.getResponseBodyAsStream(), MesosStateObject.class);
        
      rackManager.loadRacksFromMaster(state);
      
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
}
