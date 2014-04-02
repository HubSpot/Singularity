package com.hubspot.singularity.data.history;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.Utils;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;

public class SingularityTaskHistoryPersister {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityTaskHistoryPersister.class);
  
  private final TaskManager taskManager;
  private final HistoryManager historyManager;
  private final ScheduledExecutorService executorService;
  private final SingularityCloser closer;
  private final SingularityConfiguration configuration;
  
  @Inject
  public SingularityTaskHistoryPersister(TaskManager taskManager, HistoryManager historyManager, SingularityConfiguration configuration, SingularityCloser closer) {
    this.taskManager = taskManager;
    this.historyManager = historyManager;
    this.closer = closer;
    this.configuration = configuration;
    
    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityTaskHistoryPersister-%d").build());
  }
  
  public void stop() {
    closer.shutdown(getClass().getName(), executorService, 1);
  }
  
  public void start() {
    LOG.info("Starting a task history persister with a {} delay", DurationFormatUtils.formatDurationHMS(TimeUnit.SECONDS.toMillis(configuration.getPersistTaskHistoryEverySeconds())));
    
    executorService.scheduleWithFixedDelay(new Runnable() {
      
      @Override
      public void run() {
        try {
          checkInactiveTaskIds();
        } catch (Throwable t) {
          LOG.error("While persisting task history", t);
        }
      }
    }, 
    configuration.getPersistTaskHistoryEverySeconds(), configuration.getPersistTaskHistoryEverySeconds(), TimeUnit.SECONDS);
  }
  
  private void checkInactiveTaskIds() {
    LOG.info("Checking inactive task ids for task history persistance");
    
    final long start = System.currentTimeMillis();
    
    final Set<SingularityTaskId> activeTaskIds = Sets.newHashSet(taskManager.getActiveTaskIds());
    final Set<SingularityTaskId> allTaskIds = Sets.newHashSet(taskManager.getAllTaskIds());
    final Set<SingularityTaskId> lbCleaningTaskIds = Sets.newHashSet(taskManager.getLBCleanupTasks());
    
    int numTotal = 0;
    int numTransferred = 0;
    
    for (SingularityTaskId inactiveTaskId : Sets.difference(allTaskIds, activeTaskIds)) {
      if (lbCleaningTaskIds.contains(inactiveTaskId)) {
        continue;
      }
      if (transferToHistoryDB(inactiveTaskId)) {
        numTransferred++;
      }
      numTotal++;
    }
    
    LOG.info("Transferred {} out of {} inactive task ids in {}", numTransferred, numTotal, Utils.duration(start));
  }
  
  private boolean transferToHistoryDB(SingularityTaskId inactiveTaskId) {
    final long start = System.currentTimeMillis();
    
    final Optional<SingularityTaskHistory> taskHistory = taskManager.getTaskHistory(inactiveTaskId);
    
    if (taskHistory.isPresent()) {
      try {
        historyManager.saveTaskHistory(taskHistory.get());
      } catch (Throwable t) {
        LOG.warn("Failed to persist task into History for task {}", inactiveTaskId, t);
        return false;
      }
    } else {
      LOG.warn("Inactive task {} did not have a task to persist", inactiveTaskId);
    }
    
    taskManager.deleteTaskHistory(inactiveTaskId);
    
    LOG.debug("Moved task history for {} from ZK to History in {}", inactiveTaskId, Utils.duration(start));
  
    return true;
  }

}
