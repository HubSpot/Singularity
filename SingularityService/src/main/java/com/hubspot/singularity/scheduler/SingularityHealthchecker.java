package com.hubspot.singularity.scheduler;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityCloseable;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.PerRequestConfig;
import com.ning.http.client.RequestBuilder;

@SuppressWarnings("deprecation")
public class SingularityHealthchecker implements SingularityCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityHealthchecker.class);

  private final AsyncHttpClient http;
  private final SingularityConfiguration configuration;
  private final TaskManager taskManager;
  private final SingularityAbort abort;
  private final SingularityNewTaskChecker newTaskChecker;

  private final Map<String, ScheduledFuture<?>> taskIdToHealthcheck;

  private final ScheduledExecutorService executorService;
  private final SingularityCloser closer;

  private final SingularityExceptionNotifier exceptionNotifier;

  @Inject
  public SingularityHealthchecker(AsyncHttpClient http, SingularityConfiguration configuration, SingularityNewTaskChecker newTaskChecker, TaskManager taskManager, SingularityAbort abort, SingularityCloser closer, SingularityExceptionNotifier exceptionNotifier) {
    this.http = http;
    this.configuration = configuration;
    this.newTaskChecker = newTaskChecker;
    this.taskManager = taskManager;
    this.abort = abort;
    this.closer = closer;
    this.exceptionNotifier = exceptionNotifier;

    this.taskIdToHealthcheck = Maps.newConcurrentMap();

    this.executorService = Executors.newScheduledThreadPool(configuration.getHealthcheckStartThreads(), new ThreadFactoryBuilder().setNameFormat("SingularityHealthchecker-%d").build());
  }

  @Override
  public void close() {
    closer.shutdown(getClass().getName(), executorService, 1);
  }

  public void reEnqueueHealthcheck(SingularityTask task) {
    if (!taskManager.isActiveTask(task.getTaskId().getId())) {
      LOG.trace("Task {} is not active, not reEnqueueing healthcheck", task.getTaskId());
      return;
    }

    privateEnqueueHealthcheck(task);
  }

  private void privateEnqueueHealthcheck(SingularityTask task) {
    ScheduledFuture<?> future = enqueueHealthcheckWithDelay(task, task.getTaskRequest().getDeploy().getHealthcheckIntervalSeconds().or(configuration.getHealthcheckIntervalSeconds()));

    ScheduledFuture<?> existing = taskIdToHealthcheck.put(task.getTaskId().getId(), future);

    if (existing != null) {
      boolean canceledExisting = existing.cancel(false);
      LOG.warn("Found existing overlapping healthcheck for task {} - cancel success: {}", task.getTaskId(), canceledExisting);
    }
  }

  public boolean enqueueHealthcheck(SingularityTask task, Optional<SingularityPendingDeploy> pendingDeploy) {
    if (!shouldHealthcheck(task, pendingDeploy)) {
      return false;
    }

    privateEnqueueHealthcheck(task);

    return true;
  }

  public void cancelHealthcheck(String taskId) {
    ScheduledFuture<?> future = taskIdToHealthcheck.remove(taskId);

    if (future == null) {
      return;
    }

    boolean canceled = future.cancel(false);

    LOG.trace("Canceling healthcheck ({}) for task {}", canceled, taskId);
  }

  private ScheduledFuture<?> enqueueHealthcheckWithDelay(final SingularityTask task, long delaySeconds) {
    LOG.trace("Enqueing a healthcheck for task {} with delay {}", task.getTaskId(), DurationFormatUtils.formatDurationHMS(TimeUnit.SECONDS.toMillis(delaySeconds)));

    return executorService.schedule(new Runnable() {

      @Override
      public void run() {
        taskIdToHealthcheck.remove(task.getTaskId().getId());

        try {
          asyncHealthcheck(task);
        } catch (Throwable t) {
          LOG.error("Uncaught throwable in async healthcheck", t);
          exceptionNotifier.notify(t);
        }
      }

    }, delaySeconds, TimeUnit.SECONDS);
  }

  private Optional<String> getHealthcheckUri(SingularityTask task) {
    if (task.getTaskRequest().getDeploy().getHealthcheckUri() == null) {
      return Optional.absent();
    }

    final String hostname = task.getOffer().getHostname();

    Optional<Long> firstPort = task.getFirstPort();

    if (!firstPort.isPresent() || firstPort.get() < 1L) {
      LOG.warn("Couldn't find a port for health check for task {}", task);
      return Optional.absent();
    }

    String uri = task.getTaskRequest().getDeploy().getHealthcheckUri().get();

    if (uri.startsWith("/")) {
      uri = uri.substring(1);
    }

    return Optional.of(String.format("http://%s:%d/%s", hostname, firstPort.get(), uri));
  }

  private void saveFailure(SingularityHealthcheckAsyncHandler handler, String message) {
    handler.saveResult(Optional.<Integer> absent(), Optional.<String> absent(), Optional.of(message));
  }

  private boolean shouldHealthcheck(final SingularityTask task, Optional<SingularityPendingDeploy> pendingDeploy) {
    if (!task.getTaskRequest().getRequest().isLongRunning() || !task.getTaskRequest().getDeploy().getHealthcheckUri().isPresent()) {
      return false;
    }

    if (pendingDeploy.isPresent() && pendingDeploy.get().getDeployMarker().getDeployId().equals(task.getTaskId().getDeployId()) && task.getTaskRequest().getDeploy().getSkipHealthchecksOnDeploy().or(false)) {
      return false;
    }

    return true;
  }

  private void asyncHealthcheck(final SingularityTask task) {
    final SingularityHealthcheckAsyncHandler handler = new SingularityHealthcheckAsyncHandler(exceptionNotifier, configuration, this, newTaskChecker, taskManager, abort, task);
    final Optional<String> uri = getHealthcheckUri(task);

    if (!uri.isPresent()) {
      saveFailure(handler, "Invalid healthcheck uri or ports not present");
      return;
    }

    final Long timeoutSeconds = task.getTaskRequest().getDeploy().getHealthcheckTimeoutSeconds().or(configuration.getHealthcheckTimeoutSeconds());

    try {
      PerRequestConfig prc = new PerRequestConfig();
      prc.setRequestTimeoutInMs((int) TimeUnit.SECONDS.toMillis(timeoutSeconds));

      RequestBuilder builder = new RequestBuilder("GET");
      builder.setFollowRedirects(true);
      builder.setUrl(uri.get());
      builder.setPerRequestConfig(prc);

      LOG.trace("Issuing a healthcheck ({}) for task {} with timeout {}s", uri.get(), task.getTaskId(), timeoutSeconds);

      http.prepareRequest(builder.build()).execute(handler);
    } catch (Throwable t) {
      LOG.debug("Exception while preparing healthcheck ({}) for task ({})", uri, task.getTaskId(), t);
      exceptionNotifier.notify(t);
      saveFailure(handler, String.format("Healthcheck failed due to exception: %s", t.getMessage()));
    }
  }

}
