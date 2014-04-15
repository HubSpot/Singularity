package com.hubspot.singularity.executor;

import java.util.Map;
import java.util.concurrent.Executors;

import org.apache.mesos.Executor;
import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.config.SingularityExecutorLogging;
import com.hubspot.singularity.executor.config.SingularityTaskBuilder;
import com.hubspot.singularity.executor.utils.ExecutorUtils;

public class SingularityExecutor implements Executor {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityExecutor.class);

  private final Map<String, SingularityExecutorTaskHolder> tasks;
  private final ListeningExecutorService taskRunner;
  
  private final SingularityTaskBuilder taskBuilder;
  private final ExecutorUtils executorUtils;

  private final SingularityExecutorLogging logging;
  private final SingularityExecutorKiller killer;
  
  @Inject
  public SingularityExecutor(ExecutorUtils executorUtils, SingularityTaskBuilder taskBuilder, SingularityExecutorLogging logging, SingularityExecutorKiller killer) {
    this.taskBuilder = taskBuilder;
    this.executorUtils = executorUtils;
    this.logging = logging;
    this.killer = killer;
    
    tasks = Maps.newConcurrentMap();
    
    taskRunner = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("SingularityExecutorProcessRunner-%d").build()));
  }
  
  /**
   * Invoked once the executor driver has been able to successfully
   * connect with Mesos. In particular, a scheduler can pass some
   * data to it's executors through the FrameworkInfo.ExecutorInfo's
   * data field.
   */
  @Override
  public void registered(ExecutorDriver executorDriver, Protos.ExecutorInfo executorInfo, Protos.FrameworkInfo frameworkInfo, Protos.SlaveInfo slaveInfo) {
    LOG.info("Registered {} with Mesos slave {} for framework {}", executorInfo, slaveInfo, frameworkInfo);
  }

  /**
   * Invoked when the executor re-registers with a restarted slave.
   */
  @Override
  public void reregistered(ExecutorDriver executorDriver, Protos.SlaveInfo slaveInfo) {
    LOG.info("Re-registered with Mesos slave {}", slaveInfo);
  }

  /**
   * Invoked when the executor becomes "disconnected" from the slave
   * (e.g., the slave is being restarted due to an upgrade).
   */
  @Override
  public void disconnected(ExecutorDriver executorDriver) {
    LOG.warn("Disconnected from Mesos slave"); // TODO do we need to handle anything here?
  }

  /**
   * Invoked when a task has been launched on this executor (initiated
   * via Scheduler::launchTasks). Note that this task can be realized
   * with a thread, a process, or some simple computation, however, no
   * other callbacks will be invoked on this executor until this
   * callback has returned.
   */
  @Override
  public void launchTask(final ExecutorDriver executorDriver, final Protos.TaskInfo taskInfo) {
    final String taskId = taskInfo.getTaskId().getValue();
    
    LOG.info("Asked to launch task {}", taskId);

    if (taskRunner.isShutdown()) {
      LOG.warn("Can't launch task {}, executor service is shut down!", taskInfo);

      executorUtils.sendStatusUpdate(executorDriver, taskInfo, Protos.TaskState.TASK_LOST, "Executor service was shut down", LOG);
      
      return;
    }
    
    if (tasks.containsKey(taskId)) {
      LOG.error("Can't launch task {}, already had a task with that ID", taskInfo);
    
      return;
    }
     
    try {
      final ch.qos.logback.classic.Logger taskLog = taskBuilder.buildTaskLogger(taskId);
      final SingularityExecutorTask executorTask = taskBuilder.buildTask(taskId, executorDriver, taskInfo, taskLog);
    
      final ListenableFuture<Integer> future = taskRunner.submit(executorTask);
   
      Futures.addCallback(future, new FutureCallback<Integer>() {
        
        // these code blocks must not throw exceptions since they are executed inside an executor. (or must be caught)
        public void onSuccess(Integer exitCode) {
          if (executorTask.wasKilled()) {
            executorUtils.sendStatusUpdate(executorDriver, taskInfo, Protos.TaskState.TASK_KILLED, "Process was killed, but exited normally with code: " + exitCode, taskLog);
          } else if (exitCode == 0) {
            executorUtils.sendStatusUpdate(executorDriver, taskInfo, Protos.TaskState.TASK_FINISHED, "Process exited normally", taskLog);
          } else {
            executorUtils.sendStatusUpdate(executorDriver, taskInfo, Protos.TaskState.TASK_FAILED, "Exit code: " + exitCode, taskLog);
          }
          
          onFinish(executorTask, taskLog);
        }
        
        public void onFailure(Throwable t) {
          if (executorTask.wasKilled()) {
            executorUtils.sendStatusUpdate(executorDriver, taskInfo, Protos.TaskState.TASK_KILLED, "Task killed, caught expected exception: " + t.getMessage(), taskLog);
          } else {
            executorUtils.sendStatusUpdate(executorDriver, taskInfo, Protos.TaskState.TASK_LOST, "Exception running task: " + t.getMessage(), taskLog);
          }

          taskLog.error("Task {} failed with", executorTask, t);
          
          onFinish(executorTask, taskLog);
        }
      });
      
      tasks.put(taskId, new SingularityExecutorTaskHolder(executorTask, future));
        
    } catch (Throwable t) {
      LOG.error("Couldn't launch task {} because of", taskId, t);
    
      executorUtils.sendStatusUpdate(executorDriver, taskInfo, Protos.TaskState.TASK_LOST, "While launching task: " + t.getMessage(), LOG);
    }
  }
  
  private void onFinish(SingularityExecutorTask task, ch.qos.logback.classic.Logger taskLog) {
    tasks.remove(task.getTaskId());
    
    logging.stopTaskLogger(task.getTaskId(), taskLog);
  }
  
  /**
   * Invoked when a task running within this executor has been killed
   * (via SchedulerDriver::killTask). Note that no status update will
   * be sent on behalf of the executor, the executor is responsible
   * for creating a new TaskStatus (i.e., with TASK_KILLED) and
   * invoking ExecutorDriver::sendStatusUpdate.
   */
  @Override
  public void killTask(ExecutorDriver executorDriver, Protos.TaskID taskID) {
    final String taskId = taskID.getValue();
    
    LOG.info("Asked to kill task {}", taskId);

    SingularityExecutorTaskHolder taskHolder = tasks.get(taskId);
    
    if (taskHolder == null) {
      LOG.warn("Asked to kill unknown task {}, ignoring...", taskId);
      return;
    }
    
    killer.kill(taskHolder);
  }
  
  @Override
  public void frameworkMessage(ExecutorDriver executorDriver, byte[] bytes) {
    LOG.info("Received framework message: {}", JavaUtils.toString(bytes));
  }

  /**
   * Invoked when the executor should terminate all of it's currently
   * running tasks. Note that after a Mesos has determined that an
   * executor has terminated any tasks that the executor did not send
   * terminal status updates for (e.g., TASK_KILLED, TASK_FINISHED,
   * TASK_FAILED, etc) a TASK_LOST status update will be created.
   */
  @Override
  public void shutdown(ExecutorDriver executorDriver) {
    LOG.info("Asked to shutdown executor...");

    taskRunner.shutdown();  // dont accept any more tasks
    
    for (SingularityExecutorTaskHolder task : tasks.values()) {
      killer.kill(task);
    }
    
    killer.shutdown();
    
    LOG.info("Stopping driver...");
    executorDriver.stop();
  }

  /**
   * Invoked when a fatal error has occured with the executor and/or
   * executor driver. The driver will be aborted BEFORE invoking this
   * callback.
   */
  @Override
  public void error(ExecutorDriver executorDriver, String s) {
    LOG.error("Executor error: {}", s);
  }
}
