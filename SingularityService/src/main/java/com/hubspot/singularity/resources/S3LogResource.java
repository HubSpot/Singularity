package com.hubspot.singularity.resources;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jets3t.service.S3Service;
import org.jets3t.service.model.S3Object;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityS3FormatHelper;
import com.hubspot.singularity.SingularityS3Log;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.config.S3Configuration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.RequestHistoryHelper;

@Path(SingularityService.API_BASE_PATH + "/logs")
@Produces({ MediaType.APPLICATION_JSON })
public class S3LogResource extends AbstractHistoryResource {

  private static final Logger LOG = LoggerFactory.getLogger(S3LogResource.class);

  private final Optional<S3Service> s3;
  private final Optional<S3Configuration> configuration;
  private final DeployManager deployManager;
  private final RequestHistoryHelper requestHistoryHelper;

  @Inject
  public S3LogResource(HistoryManager historyManager, RequestHistoryHelper requestHistoryHelper, TaskManager taskManager, DeployManager deployManager, Optional<S3Service> s3, Optional<S3Configuration> configuration) {
    super(historyManager, taskManager, deployManager);
    this.s3 = s3;
    this.deployManager = deployManager;
    this.configuration = configuration;
    this.requestHistoryHelper = requestHistoryHelper;
  }

  private Collection<String> getS3PrefixesForTask(SingularityTaskId taskId) {
    SingularityTaskHistory history = getTaskHistory(taskId);

    SimplifiedTaskState taskState = SingularityTaskHistoryUpdate.getCurrentState(history.getTaskUpdates());

    final long start = taskId.getStartedAt();
    final long end = taskState == SimplifiedTaskState.DONE ? Iterables.getLast(history.getTaskUpdates()).getTimestamp() : System.currentTimeMillis();

    Optional<String> tag = Optional.empty();
    if (history.getTask().getTaskRequest().getDeploy().getExecutorData().isPresent()) {
      tag = history.getTask().getTaskRequest().getDeploy().getExecutorData().get().getLoggingTag();
    }

    Collection<String> prefixes = SingularityS3FormatHelper.getS3KeyPrefixes(configuration.get().getS3KeyFormat(), taskId, tag, start, end);

    LOG.trace("Task {} got S3 prefixes {} for start {}, end {}, tag {}", taskId, prefixes, start, end, tag);

    return prefixes;
  }

  private boolean isCurrentDeploy(String requestId, String deployId) {
    return deployId.equals(deployManager.getInUseDeployId(requestId).orElse(null));
  }

  private Collection<String> getS3PrefixesForRequest(String requestId) {
    Optional<SingularityRequestHistory> firstHistory = requestHistoryHelper.getFirstHistory(requestId);

    if (!firstHistory.isPresent()) {
      throw WebExceptions.notFound("No request history found for %s", requestId);
    }

    final long start = firstHistory.get().getCreatedAt();

    Optional<SingularityRequestHistory> lastHistory = requestHistoryHelper.getLastHistory(requestId);

    long end = System.currentTimeMillis();

    if (lastHistory.isPresent() && (lastHistory.get().getEventType() == RequestHistoryType.DELETED || lastHistory.get().getEventType() == RequestHistoryType.PAUSED)) {
      end = lastHistory.get().getCreatedAt() + TimeUnit.DAYS.toMillis(1);
    }

    Collection<String> prefixes = SingularityS3FormatHelper.getS3KeyPrefixes(configuration.get().getS3KeyFormat(), requestId, start, end);

    LOG.trace("Request {} got S3 prefixes {} for start {}, end {}", requestId, prefixes, start, end);

    return prefixes;
  }

  private Collection<String> getS3PrefixesForDeploy(String requestId, String deployId) {
    SingularityDeployHistory deployHistory = getDeployHistory(requestId, deployId);

    final long start = deployHistory.getDeployMarker().getTimestamp();

    long end = System.currentTimeMillis();

    if (!isCurrentDeploy(requestId, deployId) && deployHistory.getDeployStatistics().isPresent() && deployHistory.getDeployStatistics().get().getLastFinishAt().isPresent()) {
      end = deployHistory.getDeployStatistics().get().getLastFinishAt().get() + TimeUnit.DAYS.toMillis(1);
    }

    Optional<String> tag = Optional.empty();

    if (deployHistory.getDeploy().isPresent() && deployHistory.getDeploy().get().getExecutorData().isPresent()) {
      tag = deployHistory.getDeploy().get().getExecutorData().get().getLoggingTag();
    }

    Collection<String> prefixes = SingularityS3FormatHelper.getS3KeyPrefixes(configuration.get().getS3KeyFormat(), requestId, deployId, tag, start, end);

    LOG.trace("Request {}, deploy {} got S3 prefixes {} for start {}, end {}, tag {}", requestId, deployId, prefixes, start, end, tag);

    return prefixes;
  }

  private Collection<SingularityS3Log> getS3Logs(Collection<String> prefixes) throws InterruptedException, ExecutionException, TimeoutException {
    ListeningExecutorService es = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(Math.min(prefixes.size(), configuration.get().getMaxS3Threads()), new ThreadFactoryBuilder().setNameFormat("S3LogFetcher-%d").build()));

    List<ListenableFuture<S3Object[]>> futures = Lists.newArrayListWithCapacity(prefixes.size());

    for (final String s3Prefix : prefixes) {
      futures.add(es.submit(new Callable<S3Object[]>() {

        @Override
        public S3Object[] call() throws Exception {
          return s3.get().listObjects(configuration.get().getS3Bucket(), s3Prefix, null);
        }
      }));
    }

    final long start = System.currentTimeMillis();
    List<S3Object[]> results = Futures.allAsList(futures).get(configuration.get().getWaitForS3ListSeconds(), TimeUnit.SECONDS);

    List<S3Object> objects = Lists.newArrayListWithExpectedSize(results.size() * 2);

    for (S3Object[] s3Objects : results) {
      for (final S3Object s3Object : s3Objects) {
        objects.add(s3Object);
      }
    }

    LOG.trace("Got {} objects from S3 after {}", objects.size(), JavaUtils.duration(start));

    List<ListenableFuture<SingularityS3Log>> logFutures = Lists.newArrayListWithCapacity(objects.size());
    final Date expireAt = new Date(System.currentTimeMillis() + configuration.get().getExpireS3LinksAfterMillis());

    for (final S3Object s3Object : objects) {
      logFutures.add(es.submit(new Callable<SingularityS3Log>() {

        @Override
        public SingularityS3Log call() throws Exception {
          String getUrl = s3.get().createSignedGetUrl(configuration.get().getS3Bucket(), s3Object.getKey(), expireAt);

          return new SingularityS3Log(getUrl, s3Object.getKey(), s3Object.getLastModifiedDate().getTime(), s3Object.getContentLength());
        }

      }));
    }

    return Futures.allAsList(logFutures).get(configuration.get().getWaitForS3LinksSeconds(), TimeUnit.SECONDS);
  }

  private void checkS3() {
    if (!s3.isPresent()) {
      throw WebExceptions.notFound("S3 configuration was absent");
    }
  }

  @GET
  @Path("task/{taskId}")
  public Collection<SingularityS3Log> getS3LogsForTask(@PathParam("taskId") String taskId) throws Exception {
    checkS3();

    SingularityTaskId taskIdObject = getTaskIdObject(taskId);

    try {
      return getS3Logs(getS3PrefixesForTask(taskIdObject));
    } catch (TimeoutException te) {
      throw WebExceptions.timeout("Timed out waiting for response from S3 for %s", taskId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  @GET
  @Path("request/{requestId}")
  public Collection<SingularityS3Log> getS3LogsForRequest(@PathParam("requestId") String requestId) throws Exception {
    checkS3();

    try {
      return getS3Logs(getS3PrefixesForRequest(requestId));
    } catch (TimeoutException te) {
      throw WebExceptions.timeout("Timed out waiting for response from S3 for %s", requestId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  @GET
  @Path("request/{requestId}/deploy/{deployId}")
  public Collection<SingularityS3Log> getS3LogsForDeploy(@PathParam("requestId") String requestId, @PathParam("deployId") String deployId) throws Exception {
    checkS3();

    try {
      return getS3Logs(getS3PrefixesForDeploy(requestId, deployId));
    } catch (TimeoutException te) {
      throw WebExceptions.timeout("Timed out waiting for response from S3 for %s-%s", requestId, deployId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

}
