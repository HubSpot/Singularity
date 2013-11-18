package com.hubspot.singularity.mesos;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import org.apache.mesos.Protos.MasterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosFrameworkObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosTaskObject;
import com.hubspot.singularity.SingularityPendingRequestId;
import com.hubspot.singularity.SingularityPendingRequestId.PendingType;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class SingularityStartup {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityStartup.class);
  
  private final static String MASTER_STATE_FORMAT = "http://%s:%s/master/state.json";
  
  private final AsyncHttpClient asyncHttpClient;
  private final ObjectMapper objectMapper;
  private final TaskManager taskManager;
  private final SingularityRackManager rackManager;
  private final RequestManager requestManager;
  
  @Inject
  public SingularityStartup(AsyncHttpClient asyncHttpClient, ObjectMapper objectMapper, SingularityRackManager rackManager, TaskManager taskManager, RequestManager requestManager) {
    this.asyncHttpClient = asyncHttpClient;
    this.objectMapper = objectMapper;
    this.rackManager = rackManager;
    this.taskManager = taskManager;
    this.requestManager = requestManager;
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
    
    final long start = System.currentTimeMillis();
    
    LOG.info("Starting up... fetching state data from: " + uri);
    
    try {
      Response response = asyncHttpClient.prepareGet(uri).execute().get();
      
      Preconditions.checkState(response.getStatusCode() >= 200 && response.getStatusCode() < 300, "Invalid response from master %s : %s", uri, response.getStatusCode());

      MesosMasterStateObject state = objectMapper.readValue(response.getResponseBodyAsStream(), MesosMasterStateObject.class);
        
      rackManager.loadRacksFromMaster(state);
      
      // two things need to happen: 
      // 1- we need to look for active tasks that are no longer active (assume that there is no such thing as a missing active task.)
      // 2- we need to reschedule the world.
      
      checkForMissingActiveTasks(state);
      rescheduleTheWorld();
      
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
    
    LOG.info(String.format("Finished startup after %sms", System.currentTimeMillis() - start));
  }
  
  private void checkForMissingActiveTasks(MesosMasterStateObject state) {
    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
    final Set<String> strTaskIds = Sets.newHashSetWithExpectedSize(activeTaskIds.size());
    for (SingularityTaskId taskId : activeTaskIds) {
      strTaskIds.add(taskId.toString());
    }
    
    for (MesosFrameworkObject framework : state.getFrameworks()) {
      for (MesosTaskObject taskObject : framework.getTasks()) {
        strTaskIds.remove(taskObject.getId());
      }
    }
    
    // these are no longer running.
    for (String strTaskId : strTaskIds) {
      // TODO record history?
      taskManager.deleteActiveTask(strTaskId);
    }
    
    LOG.info(String.format("Finished reconciling active tasks: %s active tasks, %s were deleted", activeTaskIds.size(), strTaskIds.size()));
  }
  
  private void rescheduleTheWorld() {
    final List<String> requests = requestManager.getRequestIds();
    
    for (String requestId : requests) {
      requestManager.addToPendingQueue(new SingularityPendingRequestId(requestId));
    }
    
    LOG.info(String.format("Put %s requests in pending queue", requests.size()));
  }
  
}
