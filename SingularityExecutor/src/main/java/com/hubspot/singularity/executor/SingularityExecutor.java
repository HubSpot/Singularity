package com.hubspot.singularity.executor;

import java.util.Map;
import java.util.concurrent.Executors;

import org.apache.mesos.Executor;
import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.config.SingularityExecutorModule;
import com.hubspot.singularity.executor.utils.ExecutorUtils;

public class SingularityExecutor implements Executor {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityExecutor.class);

  private final ObjectMapper yamlObjectMapper;
  private final ObjectMapper jsonObjectMapper;
  
  private final ArtifactManager artifactManager;
  private final TemplateManager templateManager;
  private final String deployEnv;

  private final Map<String, SingularityExecutorTask> tasks;
  private final ListeningExecutorService taskRunner;
  
  @Inject
  public SingularityExecutor(@Named(SingularityExecutorModule.YAML_MAPPER) ObjectMapper yamlObjectMapper, @Named(SingularityExecutorModule.JSON_MAPPER) ObjectMapper jsonObjectMapper, 
      ArtifactManager artifactManager, TemplateManager templateManager, @Named(SingularityExecutorModule.DEPLOY_ENV) String deployEnv) {
    this.yamlObjectMapper = yamlObjectMapper;
    this.jsonObjectMapper = jsonObjectMapper;
    this.templateManager = templateManager;
    this.artifactManager = artifactManager;
    this.deployEnv = deployEnv;
    
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

      ExecutorUtils.sendStatusUpdate(executorDriver, taskInfo, Protos.TaskState.TASK_LOST, "Executor service was shut down");
      
      return;
    }
    
    try {
      final SingularityExecutorTask executorTask = new SingularityExecutorTask(executorDriver, deployEnv, artifactManager, templateManager, jsonObjectMapper, yamlObjectMapper, taskId, taskInfo);
    
      ListenableFuture<Integer> taskResult = taskRunner.submit(executorTask);
   
      Futures.addCallback(taskResult, new FutureCallback<Integer>() {
        
        public void onSuccess(Integer exitCode) {
          if (exitCode == 0) {
            ExecutorUtils.sendStatusUpdate(executorDriver, taskInfo, Protos.TaskState.TASK_FINISHED, "");
          } else {
            ExecutorUtils.sendStatusUpdate(executorDriver, taskInfo, Protos.TaskState.TASK_FAILED, "Exit code: " + exitCode);
          }
          
          onFinish(executorTask);
        }
        
        public void onFailure(Throwable t) {
          LOG.error("Task {} failed with", executorTask, t);
          
          ExecutorUtils.sendStatusUpdate(executorDriver, taskInfo, Protos.TaskState.TASK_LOST, "While running task: " + t.getMessage());
          
          onFinish(executorTask);
        }
      });
      
      tasks.put(taskId, executorTask);
        
    } catch (Throwable t) {
      LOG.error("Couldn't launch task {} because of", taskId, t);
    
      ExecutorUtils.sendStatusUpdate(executorDriver, taskInfo, Protos.TaskState.TASK_LOST, "While launching task: " + t.getMessage());
    }
  }
  
  private void onFinish(SingularityExecutorTask task) {
    tasks.remove(task.getTaskId());
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

    SingularityExecutorTask task = tasks.get(taskId);
    
    if (task == null) {
      LOG.warn("Asked to kill unknown task {}, ignoring...", taskId);
      return;
    }
    
    task.kill();
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
    
    for (SingularityExecutorTask task : tasks.values()) {
      task.kill();
    }
    
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
