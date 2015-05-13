package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkNotFound;
import static com.hubspot.singularity.WebExceptions.timeout;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
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

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityS3FormatHelper;
import com.hubspot.singularity.SingularityS3Log;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.S3Configuration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.RequestHistoryHelper;
import com.hubspot.singularity.ldap.SingularityAuthManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path(S3LogResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity task logs stored in S3.", value=S3LogResource.PATH)
public class S3LogResource extends AbstractHistoryResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/logs";

  private static final Logger LOG = LoggerFactory.getLogger(S3LogResource.class);

  private final Optional<S3Service> s3;
  private final Optional<S3Configuration> configuration;
  private final DeployManager deployManager;
  private final RequestHistoryHelper requestHistoryHelper;
  private final RequestManager requestManager;
  private final SingularityValidator validator;
  private final SingularityAuthManager authManager;

  private static final Comparator<SingularityS3Log> LOG_COMPARATOR = new Comparator<SingularityS3Log>() {

    @Override
    public int compare(SingularityS3Log o1, SingularityS3Log o2) {
      return Longs.compare(o2.getLastModified(), o1.getLastModified());
    }

  };

  @Inject
  public S3LogResource(RequestManager requestManager, HistoryManager historyManager, RequestHistoryHelper requestHistoryHelper, TaskManager taskManager, DeployManager deployManager, Optional<S3Service> s3,
      Optional<S3Configuration> configuration, SingularityValidator validator, SingularityAuthManager authManager) {
    super(historyManager, taskManager, deployManager);
    this.requestManager = requestManager;
    this.s3 = s3;
    this.deployManager = deployManager;
    this.configuration = configuration;
    this.requestHistoryHelper = requestHistoryHelper;
    this.validator = validator;
    this.authManager = authManager;
  }

  private Collection<String> getS3PrefixesForTask(SingularityTaskId taskId) {
    SingularityTaskHistory history = getTaskHistory(taskId);

    SimplifiedTaskState taskState = SingularityTaskHistoryUpdate.getCurrentState(history.getTaskUpdates());

    final long start = taskId.getStartedAt();
    final long end = taskState == SimplifiedTaskState.DONE ? Iterables.getLast(history.getTaskUpdates()).getTimestamp() : System.currentTimeMillis();

    Optional<String> tag = Optional.absent();
    if (history.getTask().getTaskRequest().getDeploy().getExecutorData().isPresent()) {
      tag = history.getTask().getTaskRequest().getDeploy().getExecutorData().get().getLoggingTag();
    }

    Collection<String> prefixes = SingularityS3FormatHelper.getS3KeyPrefixes(configuration.get().getS3KeyFormat(), taskId, tag, start, end);

    LOG.trace("Task {} got S3 prefixes {} for start {}, end {}, tag {}", taskId, prefixes, start, end, tag);

    return prefixes;
  }

  private boolean isCurrentDeploy(String requestId, String deployId) {
    return deployId.equals(deployManager.getInUseDeployId(requestId).orNull());
  }

  private Collection<String> getS3PrefixesForRequest(String requestId) {
    Optional<SingularityRequestHistory> firstHistory = requestHistoryHelper.getFirstHistory(requestId);

    checkNotFound(firstHistory.isPresent(), "No request history found for %s", requestId);

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

    Optional<String> tag = Optional.absent();

    if (deployHistory.getDeploy().isPresent() && deployHistory.getDeploy().get().getExecutorData().isPresent()) {
      tag = deployHistory.getDeploy().get().getExecutorData().get().getLoggingTag();
    }

    Collection<String> prefixes = SingularityS3FormatHelper.getS3KeyPrefixes(configuration.get().getS3KeyFormat(), requestId, deployId, tag, start, end);

    LOG.trace("Request {}, deploy {} got S3 prefixes {} for start {}, end {}, tag {}", requestId, deployId, prefixes, start, end, tag);

    return prefixes;
  }

  private List<SingularityS3Log> getS3LogsWithExecutorService(Optional<String> group, ListeningExecutorService executorService, Collection<String> prefixes) throws InterruptedException, ExecutionException, TimeoutException {
    List<ListenableFuture<S3Object[]>> futures = Lists.newArrayListWithCapacity(prefixes.size());

    final String s3Bucket = (group.isPresent() && configuration.get().getS3BucketForGroup().containsKey(group.get())) ? configuration.get().getS3BucketForGroup().get(group.get()) : configuration.get().getS3Bucket();

    for (final String s3Prefix : prefixes) {
      futures.add(executorService.submit(new Callable<S3Object[]>() {

        @Override
        public S3Object[] call() throws Exception {
          return s3.get().listObjects(s3Bucket, s3Prefix, null);
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
      logFutures.add(executorService.submit(new Callable<SingularityS3Log>() {

        @Override
        public SingularityS3Log call() throws Exception {
          String getUrl = s3.get().createSignedGetUrl(configuration.get().getS3Bucket(), s3Object.getKey(), expireAt);

          return new SingularityS3Log(getUrl, s3Object.getKey(), s3Object.getLastModifiedDate().getTime(), s3Object.getContentLength());
        }

      }));
    }

    return Futures.allAsList(logFutures).get(configuration.get().getWaitForS3LinksSeconds(), TimeUnit.SECONDS);
  }

  private List<SingularityS3Log> getS3Logs(Optional<String> group, Collection<String> prefixes) throws InterruptedException, ExecutionException, TimeoutException {
    ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(Math.min(prefixes.size(), configuration.get().getMaxS3Threads()),
        new ThreadFactoryBuilder().setNameFormat("S3LogFetcher-%d").build()));

    try {
      List<SingularityS3Log> logs = Lists.newArrayList(getS3LogsWithExecutorService(group, executorService, prefixes));
      Collections.sort(logs, LOG_COMPARATOR);
      return logs;
    } finally {
      executorService.shutdownNow();
    }
  }

  private void checkS3() {
    checkNotFound(s3.isPresent(), "S3 configuration was absent");
  }

  @GET
  @Path("/task/{taskId}")
  @ApiOperation("Retrieve the list of logs stored in S3 for a specific task.")
  public List<SingularityS3Log> getS3LogsForTask(@ApiParam("The task ID to search for") @PathParam("taskId") String taskId) throws Exception {
    checkS3();

    SingularityTaskId taskIdObject = getTaskIdObject(taskId);

    try {
      final Optional<SingularityRequestWithState> maybeRequest = requestManager.getRequest(taskIdObject.getRequestId());
      checkNotFound(maybeRequest.isPresent(), "Request ID %s does not exist", taskIdObject.getRequestId());
      validator.checkForAuthorization(maybeRequest.get().getRequest(), Optional.<SingularityRequest>absent(), authManager.getUser());
      return getS3Logs(maybeRequest.get().getRequest().getGroup(), getS3PrefixesForTask(taskIdObject));
    } catch (TimeoutException te) {
      throw timeout("Timed out waiting for response from S3 for %s", taskId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  @GET
  @Path("/request/{requestId}")
  @ApiOperation("Retrieve the list of logs stored in S3 for a specific request.")
  public List<SingularityS3Log> getS3LogsForRequest(@ApiParam("The request ID to search for") @PathParam("requestId") String requestId) throws Exception {
    checkS3();

    try {
      final Optional<SingularityRequestWithState> maybeRequest = requestManager.getRequest(requestId);
      checkNotFound(maybeRequest.isPresent(), "Request ID %s does not exist", requestId);
      validator.checkForAuthorization(maybeRequest.get().getRequest(), Optional.<SingularityRequest>absent(), authManager.getUser());
      return getS3Logs(maybeRequest.get().getRequest().getGroup(), getS3PrefixesForRequest(requestId));
    } catch (TimeoutException te) {
      throw timeout("Timed out waiting for response from S3 for %s", requestId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}")
  @ApiOperation("Retrieve the list of logs stored in S3 for a specific deploy.")
  public List<SingularityS3Log> getS3LogsForDeploy(@ApiParam("The request ID to search for") @PathParam("requestId") String requestId,
      @ApiParam("The deploy ID to search for") @PathParam("deployId") String deployId) throws Exception {
    checkS3();

    try {
      final Optional<SingularityRequestWithState> maybeRequest = requestManager.getRequest(requestId);
      checkNotFound(maybeRequest.isPresent(), "Request ID %s does not exist", requestId);
      validator.checkForAuthorization(maybeRequest.get().getRequest(), Optional.<SingularityRequest>absent(), authManager.getUser());
      return getS3Logs(maybeRequest.get().getRequest().getGroup(), getS3PrefixesForDeploy(requestId, deployId));
    } catch (TimeoutException te) {
      throw timeout("Timed out waiting for response from S3 for %s-%s", requestId, deployId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

}
