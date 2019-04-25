package com.hubspot.singularity.scheduler;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
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
import com.hubspot.deploy.HealthcheckOptions;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.HealthcheckMethod;
import com.hubspot.singularity.HealthcheckProtocol;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.RequestBuilder;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;

@Singleton
public class SingularityHealthchecker {
  private static final HealthcheckProtocol DEFAULT_HEALTH_CHECK_SCHEME = HealthcheckProtocol.HTTP;

  private static final Logger LOG = LoggerFactory.getLogger(SingularityHealthchecker.class);

  private final AsyncHttpClient http;
  private final OkHttpClient http2;
  private final SingularityConfiguration configuration;
  private final TaskManager taskManager;
  private final SingularityAbort abort;
  private final SingularityNewTaskChecker newTaskChecker;

  private final Map<String, ScheduledFuture<?>> taskIdToHealthcheck;

  private final ScheduledExecutorService executorService;

  private final SingularityExceptionNotifier exceptionNotifier;
  private final DisasterManager disasterManager;
  private final MesosProtosUtils mesosProtosUtils;

  @Inject
  public SingularityHealthchecker(SingularityManagedScheduledExecutorServiceFactory executorServiceFactory,
                                  AsyncHttpClient http, OkHttpClient http2, SingularityConfiguration configuration, SingularityNewTaskChecker newTaskChecker,
                                  TaskManager taskManager, SingularityAbort abort, SingularityExceptionNotifier exceptionNotifier, DisasterManager disasterManager,
                                  MesosProtosUtils mesosProtosUtils) {
    this.http = http;
    this.http2 = http2;
    this.configuration = configuration;
    this.newTaskChecker = newTaskChecker;
    this.taskManager = taskManager;
    this.abort = abort;
    this.exceptionNotifier = exceptionNotifier;

    this.taskIdToHealthcheck = Maps.newConcurrentMap();

    this.executorService = executorServiceFactory.get("health-checker", configuration.getHealthcheckStartThreads());
    this.disasterManager = disasterManager;
    this.mesosProtosUtils = mesosProtosUtils;
  }

  public void enqueueHealthcheck(SingularityTask task, boolean ignoreExisting, boolean inStartup, boolean isFirstCheck) {
    HealthcheckOptions options = task.getTaskRequest().getDeploy().getHealthcheck().get();
    final Optional<Integer> healthcheckMaxRetries = options.getMaxRetries().or(configuration.getHealthcheckMaxRetries());

    Optional<Long> maybeRunningAt = getRunningAt(taskManager.getTaskHistoryUpdates(task.getTaskId()));
    if (maybeRunningAt.isPresent()) {
      final long durationSinceRunning = System.currentTimeMillis() - maybeRunningAt.get();
      final int startupTimeout = options.getStartupTimeoutSeconds().or(configuration.getStartupTimeoutSeconds());
      if (inStartup && durationSinceRunning > TimeUnit.SECONDS.toMillis(startupTimeout)) {
        LOG.debug("{} since running", durationSinceRunning);
        LOG.info("Not enqueuing new healthcheck for {}, has not responded to healthchecks before startup timeout of {}s", task.getTaskId(), startupTimeout);
        return;
      }
    }

    if (healthcheckMaxRetries.isPresent() && taskManager.getNumNonstartupHealthchecks(task.getTaskId()) > healthcheckMaxRetries.get()) {
      LOG.info("Not enqueuing new healthcheck for {}, it has already attempted {} times", task.getTaskId(), healthcheckMaxRetries.get());
      return;
    }

    ScheduledFuture<?> existing = null;
    try {
      ScheduledFuture<?> future = enqueueHealthcheckWithDelay(task, getDelaySeconds(task.getTaskId(), options, inStartup, isFirstCheck), inStartup);
      existing = taskIdToHealthcheck.put(task.getTaskId().getId(), future);
    } catch (RejectedExecutionException ree) {
      LOG.warn("Executor rejected execution, Singularity is shutting down, short circuiting");
    }

    if (existing != null) {
      boolean canceledExisting = existing.cancel(false);
      if (!ignoreExisting) {
        LOG.warn("Found existing overlapping healthcheck for task {} - cancel success: {}", task.getTaskId(), canceledExisting);
      }
    }
  }

  private Optional<Long> getRunningAt(Collection<SingularityTaskHistoryUpdate> updates) {
    for (SingularityTaskHistoryUpdate update : updates) {
      if (update.getTaskState() == ExtendedTaskState.TASK_RUNNING) {
        return Optional.of(update.getTimestamp());
      }
    }
    return Optional.absent();
  }

  private int getDelaySeconds(SingularityTaskId taskId, HealthcheckOptions options, boolean inStartup, boolean isFirstCheck) {
    if (isFirstCheck && options.getStartupDelaySeconds().or(configuration.getStartupDelaySeconds()).isPresent()) {
      int delaySeconds = options.getStartupDelaySeconds().or(configuration.getStartupDelaySeconds()).get();
      LOG.trace("Delaying first healthcheck %s seconds for task {}", delaySeconds, taskId);
      return delaySeconds;
    } else if (inStartup) {
      return options.getStartupIntervalSeconds().or(configuration.getStartupIntervalSeconds());
    } else {
      return options.getIntervalSeconds().or(configuration.getHealthcheckIntervalSeconds());
    }
  }

  @Timed
  public boolean enqueueHealthcheck(SingularityTask task, Optional<SingularityPendingDeploy> pendingDeploy, Optional<SingularityRequestWithState> request) {
    if (!shouldHealthcheck(task, request, pendingDeploy)) {
      return false;
    }

    Optional<SingularityTaskHealthcheckResult> lastHealthcheck = taskManager.getLastHealthcheck(task.getTaskId());

    enqueueHealthcheck(task, true, true, !lastHealthcheck.isPresent());

    return true;
  }

  public void checkHealthcheck(SingularityTask task) {
    if (!taskIdToHealthcheck.containsKey(task.getTaskId().getId())) {
      LOG.info("Enqueueing expected healthcheck for task {}", task.getTaskId());

      Optional<SingularityTaskHealthcheckResult> lastHealthcheck = taskManager.getLastHealthcheck(task.getTaskId());
      enqueueHealthcheck(task, false, true, !lastHealthcheck.isPresent());
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

  private ScheduledFuture<?> enqueueHealthcheckWithDelay(final SingularityTask task, long delaySeconds, final boolean inStartup) {
    LOG.trace("Enqueuing a healthcheck for task {} with delay {}", task.getTaskId(), DurationFormatUtils.formatDurationHMS(TimeUnit.SECONDS.toMillis(delaySeconds)));

    return executorService.schedule(new Runnable() {

      @Override
      public void run() {
        try {
          asyncHealthcheck(task);
        } catch (Throwable t) {
          LOG.error("Uncaught throwable in async healthcheck", t);
          exceptionNotifier.notify(String.format("Uncaught throwable in async healthcheck (%s)", t.getMessage()), t, ImmutableMap.of("taskId", task.getTaskId().toString()));

          reEnqueueOrAbort(task, inStartup);
        }
      }

    }, delaySeconds, TimeUnit.SECONDS);
  }

  public void reEnqueueOrAbort(SingularityTask task, boolean inStartup) {
    try {
      enqueueHealthcheck(task, true, inStartup, false);
    } catch (Throwable t) {
      LOG.error("Caught throwable while re-enqueuing health check for {}, aborting", task.getTaskId(), t);
      exceptionNotifier.notify(String.format("Caught throwable while re-enqueuing health check (%s)", t.getMessage()), t, ImmutableMap.of("taskId", task.getTaskId().toString()));

      abort.abort(SingularityAbort.AbortReason.UNRECOVERABLE_ERROR, Optional.of(t));
    }
  }

  private Optional<String> getHealthcheckUri(SingularityTask task) {
    if (!task.getTaskRequest().getDeploy().getHealthcheck().isPresent()) {
      return Optional.absent();
    }

    HealthcheckOptions options = task.getTaskRequest().getDeploy().getHealthcheck().get();

    final String hostname = task.getHostname();

    Optional<Long> healthcheckPort = options.getPortNumber().or(MesosUtils.getPortByIndex(mesosProtosUtils.toResourceList(task.getMesosTask().getResources()), options.getPortIndex().or(0)));

    if (!healthcheckPort.isPresent() || healthcheckPort.get() < 1L) {
      return Optional.absent();
    }

    if (!task.getTaskRequest().getDeploy().getHealthcheck().get().getUri().isPresent()) {
      return Optional.absent();
    }

    String uri = task.getTaskRequest().getDeploy().getHealthcheck().get().getUri().get();
    if (uri.startsWith("/")) {
      uri = uri.substring(1);
    }

    HealthcheckProtocol protocol = options.getProtocol().or(DEFAULT_HEALTH_CHECK_SCHEME);

    return Optional.of(String.format("%s://%s:%d/%s", protocol.getProtocol(), hostname, healthcheckPort.get(), uri));
  }

  private void saveFailure(SingularityHealthcheckAsyncHandler handler, String message) {
    handler.saveResult(Optional.<Integer> absent(), Optional.<String> absent(), Optional.of(message), Optional.<Throwable>absent());
  }

  private boolean shouldHealthcheck(final SingularityTask task, final Optional<SingularityRequestWithState> request, Optional<SingularityPendingDeploy> pendingDeploy) {
    if (disasterManager.isDisabled(SingularityAction.RUN_HEALTH_CHECKS)) {
      return false;
    }
    if (!task.getTaskRequest().getRequest().isLongRunning()
        || !task.getTaskRequest().getDeploy().getHealthcheck().isPresent()
        || task.getTaskRequest().getDeploy().getHealthcheck().get().getHealthcheckResultFilePath().isPresent()) {
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

  private Callback wrappedHttp2Handler(final SingularityHealthcheckAsyncHandler handler) {
    return new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        handler.onFailed(e);
      }

      @Override
      public void onResponse(Call call, okhttp3.Response response) throws IOException {
        Optional<String> maybeResponseExcerpt = Optional.absent();

        String responseExcerpt = response.peekBody(configuration.getMaxHealthcheckResponseBodyBytes()).string();
        if (responseExcerpt.length() > 0) {
          maybeResponseExcerpt = Optional.of(responseExcerpt);
        }

        handler.onCompleted(Optional.of(response.code()), maybeResponseExcerpt);
      }
    };
  }

  private AsyncCompletionHandler<com.ning.http.client.Response> wrappedHttp1Handler(final SingularityHealthcheckAsyncHandler handler) {
    return new AsyncCompletionHandler<com.ning.http.client.Response>() {
      @Override
      public void onThrowable(Throwable t) {
        handler.onFailed(t);
      }

      @Override
      public com.ning.http.client.Response onCompleted(com.ning.http.client.Response response) throws Exception {
        Optional<String> maybeResponseExcerpt = Optional.absent();

        if (response.hasResponseBody()) {
          maybeResponseExcerpt = Optional.of(response.getResponseBodyExcerpt(configuration.getMaxHealthcheckResponseBodyBytes()));
        }

        handler.onCompleted(Optional.of(response.getStatusCode()), maybeResponseExcerpt);

        return response;
      }
    };
  }

  private void asyncHealthcheck(final SingularityTask task) {
    final Optional<String> uri = getHealthcheckUri(task);
    final SingularityHealthcheckAsyncHandler handler = new SingularityHealthcheckAsyncHandler(exceptionNotifier, configuration, this, newTaskChecker, taskManager, task);

    if (!uri.isPresent()) {
      saveFailure(handler, "Invalid healthcheck uri or ports not present");
      return;
    }
    handler.setHealthcheckUri(uri.get());

    final Integer timeoutSeconds;
    final String method;

    if (task.getTaskRequest().getDeploy().getHealthcheck().isPresent()) {
      HealthcheckOptions options = task.getTaskRequest().getDeploy().getHealthcheck().get();

      method = options.getMethod().or(HealthcheckMethod.GET).getMethod();
      timeoutSeconds = options.getResponseTimeoutSeconds().or(configuration.getHealthcheckTimeoutSeconds());
    } else {
      timeoutSeconds = configuration.getHealthcheckTimeoutSeconds();
      method = HealthcheckMethod.GET.getMethod();
    }

    try {
      HealthcheckProtocol protocol = task.getTaskRequest().getDeploy().getHealthcheck().get().getProtocol().or(HealthcheckProtocol.HTTP);

      LOG.trace("Issuing a healthcheck ({}) for task {} with timeout {}s", uri.get(), task.getTaskId(), timeoutSeconds);

      if (protocol == HealthcheckProtocol.HTTP2 || protocol == HealthcheckProtocol.HTTPS2) {
        // Creates a lightweight new client which shares the underlying resource pools of the original instance.
        http2.newBuilder()
            .retryOnConnectionFailure(false)
            .followRedirects(true)
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .cache(null)
            .build()
            .newCall(
                new okhttp3.Request.Builder()
                    .method(method, null)
                    .url(uri.get())
                    .build()
            ).enqueue(wrappedHttp2Handler(handler));
      } else {
        RequestBuilder builder = new RequestBuilder("GET");
        builder.setFollowRedirects(true);
        builder.setUrl(uri.get());
        builder.setRequestTimeout((int) TimeUnit.SECONDS.toMillis(timeoutSeconds));

        http.prepareRequest(builder.build()).execute(wrappedHttp1Handler(handler));
      }
    } catch (Throwable t) {
      LOG.debug("Exception while preparing healthcheck ({}) for task ({})", uri.get(), task.getTaskId(), t);
      exceptionNotifier.notify(String.format("Error preparing healthcheck (%s)", t.getMessage()), t, ImmutableMap.of("taskId", task.getTaskId().toString()));
      saveFailure(handler, String.format("Healthcheck (%s) failed due to exception: %s", uri.get(), t.getMessage()));
    }
  }

}
