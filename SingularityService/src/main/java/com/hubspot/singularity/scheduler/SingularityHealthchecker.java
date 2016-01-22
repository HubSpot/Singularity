package com.hubspot.singularity.scheduler;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.HealthcheckProtocol;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.PerRequestConfig;
import com.ning.http.client.RequestBuilder;

@SuppressWarnings("deprecation")
@Singleton
public class SingularityHealthchecker {
  private static final HealthcheckProtocol DEFAULT_HEALTH_CHECK_SCHEME = HealthcheckProtocol.HTTP;

  private static final Logger LOG = LoggerFactory.getLogger(SingularityHealthchecker.class);

  private final AsyncHttpClient http;
  private final SingularityConfiguration configuration;
  private final TaskManager taskManager;
  private final SingularityAbort abort;
  private final SingularityNewTaskChecker newTaskChecker;

  private final Map<String, ScheduledFuture<?>> taskIdToHealthcheck;

  private final ScheduledExecutorService executorService;

  private final SingularityExceptionNotifier exceptionNotifier;

  @Inject
  public SingularityHealthchecker(@Named(SingularityMainModule.HEALTHCHECK_THREADPOOL_NAME) ScheduledExecutorService executorService,
      AsyncHttpClient http, SingularityConfiguration configuration, SingularityNewTaskChecker newTaskChecker,
      TaskManager taskManager, SingularityAbort abort, SingularityExceptionNotifier exceptionNotifier) {
    this.http = http;
    this.configuration = configuration;
    this.newTaskChecker = newTaskChecker;
    this.taskManager = taskManager;
    this.abort = abort;
    this.exceptionNotifier = exceptionNotifier;

    this.taskIdToHealthcheck = Maps.newConcurrentMap();

    this.executorService = executorService;
  }

  public void enqueueHealthcheck(SingularityTask task, boolean ignoreExisting) {
    final Optional<Integer> healthcheckMaxRetries = task.getTaskRequest().getDeploy().getHealthcheckMaxRetries().or(configuration.getHealthcheckMaxRetries());

    if (healthcheckMaxRetries.isPresent() && taskManager.getNumHealthchecks(task.getTaskId()) > healthcheckMaxRetries.get()) {
      LOG.info("Not enqueuing new healthcheck for {}, it has already attempted {} times", task.getTaskId(), healthcheckMaxRetries.get());
      return;
    }

    ScheduledFuture<?> future = enqueueHealthcheckWithDelay(task, task.getTaskRequest().getDeploy().getHealthcheckIntervalSeconds().or(configuration.getHealthcheckIntervalSeconds()));

    ScheduledFuture<?> existing = taskIdToHealthcheck.put(task.getTaskId().getId(), future);

    if (existing != null) {
      boolean canceledExisting = existing.cancel(false);
      if (!ignoreExisting) {
        LOG.warn("Found existing overlapping healthcheck for task {} - cancel success: {}", task.getTaskId(), canceledExisting);
      }
    }
  }

  @Timed
  public boolean enqueueHealthcheck(SingularityTask task, Optional<SingularityPendingDeploy> pendingDeploy, Optional<SingularityRequestWithState> request) {
    if (!shouldHealthcheck(task, request, pendingDeploy)) {
      return false;
    }

    enqueueHealthcheck(task, true);

    return true;
  }

  public void checkHealthcheck(SingularityTask task) {
    if (!taskIdToHealthcheck.containsKey(task.getTaskId().getId())) {
      LOG.info("Enqueueing expected healthcheck for task {}", task.getTaskId());
      enqueueHealthcheck(task, false);
    }
  }

  @VisibleForTesting
  Collection<ScheduledFuture<?>> getHealthCheckFutures() {
    return taskIdToHealthcheck.values();
  }

  public void markHealthcheckFinished(String taskId) {
    taskIdToHealthcheck.remove(taskId);
  }

  public boolean cancelHealthcheck(String taskId) {
    ScheduledFuture<?> future = taskIdToHealthcheck.remove(taskId);

    if (future == null) {
      return false;
    }

    boolean canceled = future.cancel(false);

    LOG.trace("Canceling healthcheck ({}) for task {}", canceled, taskId);

    return canceled;
  }

  private ScheduledFuture<?> enqueueHealthcheckWithDelay(final SingularityTask task, long delaySeconds) {
    LOG.trace("Enqueuing a healthcheck for task {} with delay {}", task.getTaskId(), DurationFormatUtils.formatDurationHMS(TimeUnit.SECONDS.toMillis(delaySeconds)));

    return executorService.schedule(new Runnable() {

      @Override
      public void run() {
        try {
          asyncHealthcheck(task);
        } catch (Throwable t) {
          LOG.error("Uncaught throwable in async healthcheck", t);
          exceptionNotifier.notify(t, ImmutableMap.of("taskId", task.getTaskId().toString()));

          reEnqueueOrAbort(task);
        }
      }

    }, delaySeconds, TimeUnit.SECONDS);
  }

  public void reEnqueueOrAbort(SingularityTask task) {
    try {
      enqueueHealthcheck(task, true);
    } catch (Throwable t) {
      LOG.error("Caught throwable while re-enqueuing health check for {}, aborting", task.getTaskId(), t);
      exceptionNotifier.notify(t, ImmutableMap.of("taskId", task.getTaskId().toString()));

      abort.abort(SingularityAbort.AbortReason.UNRECOVERABLE_ERROR, Optional.of(t));
    }
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

    HealthcheckProtocol protocol = task.getTaskRequest().getDeploy().getHealthcheckProtocol().or(DEFAULT_HEALTH_CHECK_SCHEME);

    return Optional.of(String.format("%s://%s:%d/%s", protocol.getProtocol(), hostname, firstPort.get(), uri));
  }

  private void saveFailure(SingularityHealthcheckAsyncHandler handler, String message) {
    handler.saveResult(Optional.<Integer> absent(), Optional.<String> absent(), Optional.of(message));
  }

  private boolean shouldHealthcheck(final SingularityTask task, final Optional<SingularityRequestWithState> request, Optional<SingularityPendingDeploy> pendingDeploy) {
    if (!task.getTaskRequest().getRequest().isLongRunning() || !task.getTaskRequest().getDeploy().getHealthcheckUri().isPresent()) {
      return false;
    }

    if (task.getTaskRequest().getPendingTask().getSkipHealthchecks().or(false)) {
      return false;
    }

    if (pendingDeploy.isPresent() && pendingDeploy.get().getDeployMarker().getDeployId().equals(task.getTaskId().getDeployId()) && task.getTaskRequest().getDeploy().getSkipHealthchecksOnDeploy().or(false)) {
      return false;
    }

    if (request.isPresent() && request.get().getRequest().getSkipHealthchecks().or(false)) {
      return false;
    }

    Optional<SingularityTaskHealthcheckResult> lastHealthcheck = taskManager.getLastHealthcheck(task.getTaskId());

    if (lastHealthcheck.isPresent() && !lastHealthcheck.get().isFailed()) {
      LOG.debug("Not submitting a new healthcheck for {} because it already passed a healthcheck", task.getTaskId());
      return false;
    }

    return true;
  }

  private void asyncHealthcheck(final SingularityTask task) {
    final SingularityHealthcheckAsyncHandler handler = new SingularityHealthcheckAsyncHandler(exceptionNotifier, configuration, this, newTaskChecker, taskManager, task);
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
      exceptionNotifier.notify(t, ImmutableMap.of("taskId", task.getTaskId().toString()));
      saveFailure(handler, String.format("Healthcheck failed due to exception: %s", t.getMessage()));
    }
  }

}
