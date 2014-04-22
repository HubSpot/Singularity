package com.hubspot.singularity.executor;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.TaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.config.SingularityExecutorLogging;
import com.hubspot.singularity.executor.task.SingularityExecutorTask;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskProcessCallable;
import com.hubspot.singularity.executor.utils.ExecutorUtils;

public class SingularityExecutorMonitor {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityExecutorMonitor.class);

  private final ListeningExecutorService processBuilderPool;
  private final ListeningExecutorService runningProcessPool;
  private final ScheduledExecutorService exitChecker;
  
  private final Lock exitLock;
  
  @SuppressWarnings("rawtypes")
  private volatile Optional<Future> exitCheckerFuture;
  private volatile RunState runState;
  
  private final SingularityExecutorConfiguration configuration;
  private final SingularityExecutorLogging logging;
  private final ExecutorUtils executorUtils;
  private final SingularityExecutorProcessKiller processKiller;
  
  private final Map<String, SingularityExecutorTask> tasks;
  private final Map<String, ListenableFuture<ProcessBuilder>> processBuildingTasks;
  private final Map<String, SingularityExecutorTaskProcessCallable> processRunningTasks;
  
  @Inject
  public SingularityExecutorMonitor(SingularityExecutorLogging logging, ExecutorUtils executorUtils, SingularityExecutorProcessKiller processKiller, SingularityExecutorConfiguration configuration) {
    this.logging = logging;
    this.configuration = configuration;
    this.executorUtils = executorUtils;
    this.processKiller = processKiller;
    this.exitChecker = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("SingularityExecutorExitChecker-%d").build());
    
    this.tasks = Maps.newConcurrentMap();
    this.processBuildingTasks = Maps.newConcurrentMap();
    this.processRunningTasks = Maps.newConcurrentMap();
    
    this.processBuilderPool = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("SingularityExecutorProcessBuilder-%d").build()));
    this.runningProcessPool = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("SingularityExecutorProcessRunner-%d").build()));
  
    this.runState = RunState.RUNNING;
    this.exitLock = new ReentrantLock();  
    
    this.exitCheckerFuture = Optional.of(startExitChecker(Optional.<ExecutorDriver> absent()));
  }
  
  public enum RunState {
    RUNNING, SHUTDOWN;
  }
  
  public enum SubmitState {
    SUBMITTED, REJECTED, TASK_ALREADY_EXISTED;
  }
    
  public void shutdown(Optional<ExecutorDriver> driver) {
    LOG.info("Shutdown requested with driver {}", driver);
    
    processBuilderPool.shutdown();

    runningProcessPool.shutdown();
    
    for (SingularityExecutorTask task : tasks.values()) {
      task.getLog().info("Executor shutting down - requested task kill with state: {}", requestKill(task.getTaskId()));
    }
    
    processKiller.getExecutorService().shutdown();

    exitChecker.shutdown();
    
    CountDownLatch latch = new CountDownLatch(3);
    
    JavaUtils.awaitTerminationWithLatch(latch, "processBuilder", processBuilderPool, configuration.getShutdownTimeoutWaitMillis());
    JavaUtils.awaitTerminationWithLatch(latch, "runningProcess", runningProcessPool, configuration.getShutdownTimeoutWaitMillis());
    JavaUtils.awaitTerminationWithLatch(latch, "processKiller", processKiller.getExecutorService(), configuration.getShutdownTimeoutWaitMillis());
    
    LOG.info("Awaiting shutdown of all executor services for a max of {}ms", configuration.getShutdownTimeoutWaitMillis());
    
    try {
      latch.await();
    } catch (InterruptedException e) {
      LOG.warn("While awaiting shutdown of executor services", e);
    }
    
    LOG.info("Waiting {}ms before exiting driver...", configuration.getStopDriverAfterMillis());
    
    try {
      Thread.sleep(configuration.getStopDriverAfterMillis());
    } catch (Throwable t) {
      LOG.warn("While sleeping waiting to stop driver", t);
    }
    
    if (driver.isPresent()) {
      LOG.info("Stopping driver {}", driver.get());
      driver.get().stop();
      
    } else {
      LOG.warn("No driver present on shutdown, using System.exit(1)");
      System.exit(1);
    }
  }
  
  private void checkForExit(final Optional<ExecutorDriver> driver) {
    try {
      exitLock.lockInterruptibly();
    } catch (InterruptedException e) {
      LOG.warn("Interrupted acquiring exit lock", e);
      return;
    }
    
    boolean shuttingDown = false;
    
    try {
      if (tasks.isEmpty()) {
        runState = RunState.SHUTDOWN;
        shuttingDown = true;
      }
    } finally {
      exitLock.unlock();
    }
    
    if (shuttingDown) {
      shutdown(driver);
    } else if (runState == RunState.SHUTDOWN) {
      LOG.info("Already shutting down...");
    } else {
      LOG.info("Tasks wasn't empty, exit checker doing nothing...");
    }
  }
  
  @SuppressWarnings("rawtypes")
  private Future startExitChecker(final Optional<ExecutorDriver> driver) {
    LOG.info("Starting an exit checker that will run in {}ms", configuration.getIdleExecutorShutdownWaitMillis());
    
    return exitChecker.schedule(new Runnable() {
      
      @Override
      public void run() {
        LOG.info("Exit checker running...");
        
        try {
          checkForExit(driver);
        } catch (Throwable t) {
          LOG.error("Caught while shutting down", t);
          System.exit(2);
        }
      }
    }, configuration.getIdleExecutorShutdownWaitMillis(), TimeUnit.MILLISECONDS);
  }
  
  private void clearExitCheckerUnsafe() {
    if (exitCheckerFuture.isPresent()) {
      LOG.info("Canceling an exit checker");
      exitCheckerFuture.get().cancel(true);
      exitCheckerFuture = Optional.absent();
    }
  }    
  
  public SubmitState submit(final SingularityExecutorTask task) {
    exitLock.lock();
    task.getLock().lock();
    
    try {
      if (runState == RunState.SHUTDOWN) {
        return SubmitState.REJECTED;
      }
      
      if (tasks.containsKey(task.getTaskId())) {
        return SubmitState.TASK_ALREADY_EXISTED;
      }
      tasks.put(task.getTaskId(), task);

      clearExitCheckerUnsafe();
    } finally {
      task.getLock().unlock();
      exitLock.unlock();
    }
    
    try {
      final ListenableFuture<ProcessBuilder> processBuildFuture = processBuilderPool.submit(task.getProcessBuilder());
      
      watchProcessBuilder(task, processBuildFuture);
    } catch (Throwable t) {
      task.getLog().error("Couldn't start task", t);
      
      sendStatusUpdate(task, TaskState.TASK_LOST, "Task couldn't start due to: " + t.getMessage());
      
      onFinish(task);
      
      return SubmitState.REJECTED;
    }
    
    return SubmitState.SUBMITTED;
  }
  
  private void watchProcessBuilder(final SingularityExecutorTask task, final ListenableFuture<ProcessBuilder> processBuildFuture) {
    Futures.addCallback(processBuildFuture, new FutureCallback<ProcessBuilder>() {
      
      // these code blocks must not throw exceptions since they are executed inside an executor. (or must be caught)
      public void onSuccess(ProcessBuilder processBuilder) {
        task.getLog().debug("Process builder finished succesfully... ");
        
        boolean wasKilled = false;
        
        task.getLock().lock();
        
        try {
          processBuildingTasks.remove(task.getTaskId());
          
          wasKilled = task.wasKilled();
          
          if (!wasKilled) {
            try {
              processRunningTasks.put(task.getTaskId(), submitProcessMonitor(task, processBuilder));
            } catch (Throwable t) {
              task.getLog().error("While submitting process task", t);
              
              sendStatusUpdate(task, Protos.TaskState.TASK_LOST, String.format("Task lost while transitioning due to: %s", t.getClass().getSimpleName()));
              
              onFinish(task);
            }
          }
        } finally {
          task.getLock().unlock();
        }
          
        if (wasKilled) {
          sendStatusUpdate(task, Protos.TaskState.TASK_KILLED, "Task killed before service process started");
          
          onFinish(task);
        }
      }
      
      public void onFailure(Throwable t) {
        if (task.wasKilled()) {
          sendStatusUpdate(task, Protos.TaskState.TASK_KILLED, String.format("Task killed, caught expected %s", t.getClass().getSimpleName()));
        } else {
          task.getLog().error("Task {} failed before starting process", task, t);
          
          sendStatusUpdate(task, Protos.TaskState.TASK_LOST, String.format("%s while initializing task: %s", t.getClass().getSimpleName(), t.getMessage()));
        }

        onFinish(task);
      }
    });
    
  }
  
  private void sendStatusUpdate(SingularityExecutorTask task, Protos.TaskState taskState, String message) {
    executorUtils.sendStatusUpdate(task.getDriver(), task.getTaskInfo(), taskState, message, task.getLog());
  }
  
  private void onFinish(SingularityExecutorTask task) {
    tasks.remove(task.getTaskId());
    processRunningTasks.remove(task.getTaskId());
    processBuildingTasks.remove(task.getTaskId());

    cleanupTaskResources(task);
    
    logging.stopTaskLogger(task.getTaskId(), task.getLog());
    
    checkIdleExecutorShutdown(task.getDriver());
  }
  
  private void checkIdleExecutorShutdown(ExecutorDriver driver) {
    exitLock.lock();
    
    try {
      clearExitCheckerUnsafe();

      if (tasks.isEmpty()) {
        exitCheckerFuture = Optional.of(startExitChecker(Optional.of(driver)));
      }
    } finally {
      exitLock.unlock();
    }
  }
  
  private void cleanupTaskResources(SingularityExecutorTask task) {
    Path taskAppDirectoryPath = configuration.getTaskAppDirectoryPath(task.getTaskId());
    final String pathToDelete = taskAppDirectoryPath.toAbsolutePath().toString();
    
    task.getLog().info("Deleting: {}", pathToDelete);
    
    try {
      Runtime.getRuntime().exec(String.format("rm -rf %s", pathToDelete)).waitFor();
    } catch (Throwable t) {
      task.getLog().error("While deleting directory {}", pathToDelete, t);
    }
  }
  
  public enum KillState {
    DIDNT_EXIST, ALREADY_REQUESTED, INTERRUPTING_PRE_PROCESS, KILLING_PROCESS, INCONSISTENT_STATE;
  }
  
  public KillState requestKill(String taskId) {
    final Optional<SingularityExecutorTask> maybeTask = Optional.fromNullable(tasks.get(taskId));
      
    if (!maybeTask.isPresent()) {
      return KillState.DIDNT_EXIST;
    }
      
    final SingularityExecutorTask task = maybeTask.get();
    
    ListenableFuture<ProcessBuilder> processBuilderFuture = null;
    SingularityExecutorTaskProcessCallable runningProcess = null;
    
    task.getLock().lock();
    
    try {
      if (task.wasKilled()) {
        return KillState.ALREADY_REQUESTED;
      }
      
      task.markKilled();
      
      processBuilderFuture = processBuildingTasks.get(task.getTaskId());
      runningProcess = processRunningTasks.get(task.getTaskId());
    } finally {
      task.getLock().unlock();
    }
    
    if (processBuilderFuture != null) {
      processBuilderFuture.cancel(true);
   
      task.getProcessBuilder().getArtifactManager().markKilled();
      task.getProcessBuilder().getArtifactManager().destroyProcessIfActive();
      
      return KillState.INTERRUPTING_PRE_PROCESS;
    }
    
    if (runningProcess != null) {
      processKiller.submitKillRequest(runningProcess);
    
      return KillState.KILLING_PROCESS;
    }
    
    return KillState.INCONSISTENT_STATE;
  }
  
  private SingularityExecutorTaskProcessCallable buildProcessCallable(final SingularityExecutorTask task, ProcessBuilder processBuilder) {
    return new SingularityExecutorTaskProcessCallable(task, processBuilder, executorUtils);
  }
  
  private SingularityExecutorTaskProcessCallable submitProcessMonitor(final SingularityExecutorTask task, ProcessBuilder processBuilder) {
    SingularityExecutorTaskProcessCallable processCallable = buildProcessCallable(task, processBuilder);
    
    final ListenableFuture<Integer> processExitFuture = runningProcessPool.submit(processCallable);
    
    watchProcessExitFuture(task, processExitFuture);
   
    return processCallable;
  }
  
  private void watchProcessExitFuture(final SingularityExecutorTask task, final ListenableFuture<Integer> processExitFuture) {
    Futures.addCallback(processExitFuture, new FutureCallback<Integer>() {
      
      // these code blocks must not throw exceptions since they are executed inside an executor. (or must be caught)
      public void onSuccess(Integer exitCode) {
        if (task.wasKilled()) {
          sendStatusUpdate(task, Protos.TaskState.TASK_KILLED, "Task killed, but process exited with code: " + exitCode);
        } else if (task.isSuccessExitCode(exitCode)) {
          sendStatusUpdate(task, Protos.TaskState.TASK_FINISHED, "Process exited normally with code: " + exitCode);
        } else {
          sendStatusUpdate(task, Protos.TaskState.TASK_FAILED, "Process failed with exit code: " + exitCode);
        }
        
        onFinish(task);
      }
      
      public void onFailure(Throwable t) {
        task.getLog().error("Task {} failed while running process", task, t);
        
        if (task.wasKilled()) {
          sendStatusUpdate(task, Protos.TaskState.TASK_KILLED, String.format("Task killed, caught %s", t.getClass().getSimpleName()));
        } else {
          sendStatusUpdate(task, Protos.TaskState.TASK_LOST, String.format("%s while running process: %s", t.getClass().getSimpleName(), t.getMessage()));
        }
        
        onFinish(task);
      }
      
    });
  }
  
}
