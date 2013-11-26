package com.hubspot.singularity.mesos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosExecutorObject;
import com.hubspot.mesos.json.MesosSlaveFrameworkObject;
import com.hubspot.mesos.json.MesosSlaveStateObject;
import com.hubspot.mesos.json.MesosTaskObject;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.data.history.HistoryManager;

public class SingularityLogSupport {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityLogSupport.class);

  private final MesosClient mesosClient;
  private final HistoryManager historyManager;
  
  @Inject
  public SingularityLogSupport(MesosClient mesosClient, HistoryManager historyManager) {
    this.mesosClient = mesosClient;
    this.historyManager = historyManager;
  }

  public void notifyRunning(SingularityTask task) {
    final long now = System.currentTimeMillis();
    
    final String slaveUri = mesosClient.getSlaveUri(task.getOffer().getHostname());
    
    LOG.info(String.format("Fetching slave data to find log directory for task %s from uri %s", task.getTaskId(), slaveUri));
  
    MesosSlaveStateObject slaveState = mesosClient.getSlaveState(slaveUri);
    
    String directory = null;
    
    for (MesosSlaveFrameworkObject slaveFramework : slaveState.getFrameworks()) {
      for (MesosExecutorObject executor : slaveFramework.getExecutors()) {
        for (MesosTaskObject executorTask : executor.getTasks()) {
          if (task.getTaskId().getId().equals(executorTask.getId())) {
            directory = executor.getDirectory();
            break;
          }
        }
      }
    }
    
    if (directory == null) {
      LOG.warn(String.format("Couldn't find matching executor for task %s", task.getTaskId()));
      return;
    }
    
    LOG.debug(String.format("Found a directory %s for task %s", directory, task.getTaskId().getId()));
      
    historyManager.updateTaskDirectory(task.getTaskId().getId(), directory);
  
    LOG.trace(String.format("Updated task directory in %sms", System.currentTimeMillis() - now));
  }
  
}
