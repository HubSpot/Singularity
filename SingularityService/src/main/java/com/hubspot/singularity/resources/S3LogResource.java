package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkNotFound;
import static com.hubspot.singularity.WebExceptions.timeout;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
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
import com.hubspot.singularity.SingularityAuthorizationScope;
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
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.S3Configuration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.RequestHistoryHelper;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path(S3LogResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity task logs stored in S3.", value=S3LogResource.PATH)
public class S3LogResource extends AbstractHistoryResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/logs";
  private static final List<String> SUPPORTED_COMPRESSED_FILE_EXTENTIONS = Arrays.asList(".gz", ".bz2");

  private static final Logger LOG = LoggerFactory.getLogger(S3LogResource.class);

  private static final String FORCE_DOWNLOAD_S3_PARAMS = "response-content-disposition=attachment&response-content-encoding=identity";

  private static final int DEFAULT_READ_LENGTH = 65000;

  private final Optional<S3Service> s3ServiceDefault;
  private final Map<String, S3Service> s3GroupOverride;
  private final Optional<S3Configuration> configuration;
  private final RequestHistoryHelper requestHistoryHelper;
  private final RequestManager requestManager;

  private static final Comparator<SingularityS3Log> LOG_COMPARATOR = new Comparator<SingularityS3Log>() {

    @Override
    public int compare(SingularityS3Log o1, SingularityS3Log o2) {
      return Longs.compare(o2.getLastModified(), o1.getLastModified());
    }

  };

  @Inject
  public S3LogResource(RequestManager requestManager, HistoryManager historyManager, RequestHistoryHelper requestHistoryHelper, TaskManager taskManager, DeployManager deployManager, Optional<S3Service> s3ServiceDefault,
      Optional<S3Configuration> configuration, SingularityAuthorizationHelper authorizationHelper, Optional<SingularityUser> user, Map<String, S3Service> s3GroupOverride) {
    super(historyManager, taskManager, deployManager, authorizationHelper, user);
    this.requestManager = requestManager;
    this.s3ServiceDefault = s3ServiceDefault;
    this.configuration = configuration;
    this.requestHistoryHelper = requestHistoryHelper;
    this.s3GroupOverride = s3GroupOverride;
  }

  private Collection<String> getS3PrefixesForTask(S3Configuration s3Configuration, SingularityTaskId taskId, Optional<Long> startArg, Optional<Long> endArg) {
    Optional<SingularityTaskHistory> history = getTaskHistory(taskId);

    long start = taskId.getStartedAt();
    if (startArg.isPresent()) {
      start = Math.max(startArg.get(), start);
    }

    long end = start + s3Configuration.getMissingTaskDefaultS3SearchPeriodMillis();

    if (history.isPresent()) {
      SimplifiedTaskState taskState = SingularityTaskHistoryUpdate.getCurrentState(history.get().getTaskUpdates());

      if (taskState == SimplifiedTaskState.DONE) {
        end = Iterables.getLast(history.get().getTaskUpdates()).getTimestamp();
      } else {
        end = System.currentTimeMillis();
      }
    }

    if (endArg.isPresent()) {
      end = Math.min(endArg.get(), end);
    }

    Optional<String> tag = Optional.absent();
    if (history.isPresent() && history.get().getTask().getTaskRequest().getDeploy().getExecutorData().isPresent()) {
      tag = history.get().getTask().getTaskRequest().getDeploy().getExecutorData().get().getLoggingTag();
    }

    Collection<String> prefixes = SingularityS3FormatHelper.getS3KeyPrefixes(s3Configuration.getS3KeyFormat(), taskId, tag, start, end);

    LOG.trace("Task {} got S3 prefixes {} for start {}, end {}, tag {}", taskId, prefixes, start, end, tag);

    return prefixes;
  }

  private boolean isCurrentDeploy(String requestId, String deployId) {
    return deployId.equals(deployManager.getInUseDeployId(requestId).orNull());
  }

  private Collection<String> getS3PrefixesForRequest(S3Configuration s3Configuration, String requestId, Optional<Long> startArg, Optional<Long> endArg) {
    Optional<SingularityRequestHistory> firstHistory = requestHistoryHelper.getFirstHistory(requestId);

    checkNotFound(firstHistory.isPresent(), "No request history found for %s", requestId);

    long start = firstHistory.get().getCreatedAt();
    if (startArg.isPresent()) {
      start = Math.max(startArg.get(), start);
    }

    Optional<SingularityRequestHistory> lastHistory = requestHistoryHelper.getLastHistory(requestId);

    long end = System.currentTimeMillis();

    if (lastHistory.isPresent() && (lastHistory.get().getEventType() == RequestHistoryType.DELETED || lastHistory.get().getEventType() == RequestHistoryType.PAUSED)) {
      end = lastHistory.get().getCreatedAt() + TimeUnit.DAYS.toMillis(1);
    }

    if (endArg.isPresent()) {
      end = Math.min(endArg.get(), end);
    }

    Collection<String> prefixes = SingularityS3FormatHelper.getS3KeyPrefixes(s3Configuration.getS3KeyFormat(), requestId, start, end);

    LOG.trace("Request {} got S3 prefixes {} for start {}, end {}", requestId, prefixes, start, end);

    return prefixes;
  }

  private Collection<String> getS3PrefixesForDeploy(S3Configuration s3Configuration, String requestId, String deployId, Optional<Long> startArg, Optional<Long> endArg) {
    SingularityDeployHistory deployHistory = getDeployHistory(requestId, deployId);

    long start = deployHistory.getDeployMarker().getTimestamp();
    if (startArg.isPresent()) {
      start = Math.max(startArg.get(), start);
    }

    long end = System.currentTimeMillis();

    if (!isCurrentDeploy(requestId, deployId) && deployHistory.getDeployStatistics().isPresent() && deployHistory.getDeployStatistics().get().getLastFinishAt().isPresent()) {
      end = deployHistory.getDeployStatistics().get().getLastFinishAt().get() + TimeUnit.DAYS.toMillis(1);
    }

    if (endArg.isPresent()) {
      end = Math.min(endArg.get(), end);
    }

    Optional<String> tag = Optional.absent();

    if (deployHistory.getDeploy().isPresent() && deployHistory.getDeploy().get().getExecutorData().isPresent()) {
      tag = deployHistory.getDeploy().get().getExecutorData().get().getLoggingTag();
    }

    Collection<String> prefixes = SingularityS3FormatHelper.getS3KeyPrefixes(s3Configuration.getS3KeyFormat(), requestId, deployId, tag, start, end);

    LOG.trace("Request {}, deploy {} got S3 prefixes {} for start {}, end {}, tag {}", requestId, deployId, prefixes, start, end, tag);

    return prefixes;
  }

  private List<SingularityS3Log> getS3LogsWithExecutorService(S3Configuration s3Configuration, Optional<String> group, ListeningExecutorService executorService, Collection<String> prefixes) throws InterruptedException, ExecutionException, TimeoutException {
    List<ListenableFuture<S3Object[]>> futures = Lists.newArrayListWithCapacity(prefixes.size());

    final String s3Bucket = (group.isPresent() && s3Configuration.getGroupOverrides().containsKey(group.get())) ? s3Configuration.getGroupOverrides().get(group.get()).getS3Bucket() : s3Configuration.getS3Bucket();

    final S3Service s3Service = (group.isPresent() && s3GroupOverride.containsKey(group.get())) ? s3GroupOverride.get(group.get()) : s3ServiceDefault.get();

    for (final String s3Prefix : prefixes) {
      futures.add(executorService.submit(new Callable<S3Object[]>() {

        @Override
        public S3Object[] call() throws Exception {
          return s3Service.listObjects(s3Bucket, s3Prefix, null);
        }
      }));
    }

    final long start = System.currentTimeMillis();
    List<S3Object[]> results = Futures.allAsList(futures).get(s3Configuration.getWaitForS3ListSeconds(), TimeUnit.SECONDS);

    List<S3Object> objects = Lists.newArrayListWithExpectedSize(results.size() * 2);

    for (S3Object[] s3Objects : results) {
      for (final S3Object s3Object : s3Objects) {
        objects.add(s3Object);
      }
    }

    LOG.trace("Got {} objects from S3 after {}", objects.size(), JavaUtils.duration(start));

    List<ListenableFuture<SingularityS3Log>> logFutures = Lists.newArrayListWithCapacity(objects.size());
    final Date expireAt = new Date(System.currentTimeMillis() + s3Configuration.getExpireS3LinksAfterMillis());

    for (final S3Object s3Object : objects) {
      logFutures.add(executorService.submit(new Callable<SingularityS3Log>() {

        @Override
        public SingularityS3Log call() throws Exception {
          String getUrl = s3Service.createSignedGetUrl(s3Bucket, s3Object.getKey(), expireAt);
          String downloadUrl = s3Service.createSignedUrl("GET", s3Bucket, s3Object.getKey(), FORCE_DOWNLOAD_S3_PARAMS, null, expireAt.getTime() / 1000, false);

          return new SingularityS3Log(getUrl, s3Object.getKey(), s3Object.getLastModifiedDate().getTime(), s3Object.getContentLength(), downloadUrl);
        }

      }));
    }

    return Futures.allAsList(logFutures).get(s3Configuration.getWaitForS3LinksSeconds(), TimeUnit.SECONDS);
  }

  private List<SingularityS3Log> getS3Logs(S3Configuration s3Configuration, Optional<String> group, Collection<String> prefixes) throws InterruptedException, ExecutionException, TimeoutException {
    if (prefixes.isEmpty()) {
      return Collections.emptyList();
    }

    ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(Math.min(prefixes.size(), s3Configuration.getMaxS3Threads()),
        new ThreadFactoryBuilder().setNameFormat("S3LogFetcher-%d").build()));

    try {
      List<SingularityS3Log> logs = Lists.newArrayList(getS3LogsWithExecutorService(s3Configuration, group, executorService, prefixes));
      Collections.sort(logs, LOG_COMPARATOR);
      return logs;
    } finally {
      executorService.shutdownNow();
    }
  }

  private void checkS3() {
    checkNotFound(s3ServiceDefault.isPresent(), "S3 configuration was absent");
    checkNotFound(configuration.isPresent(), "S3 configuration was absent");
  }

  private void checkForCompressedFile(String key) {
    boolean isSupportedFileType = false;
    for (String type : SUPPORTED_COMPRESSED_FILE_EXTENTIONS) {
      if (key.endsWith(type)) {
        isSupportedFileType = true;
      }
    }
    if (!isSupportedFileType) {
      WebExceptions.badRequest(String.format("Not a supported file type. (%s)", key));
    }
  }

  private Optional<String> getRequestGroupForTask(final SingularityTaskId taskId) {
    Optional<SingularityTaskHistory> maybeTaskHistory = getTaskHistory(taskId);
    if (maybeTaskHistory.isPresent()) {
      SingularityRequest request = maybeTaskHistory.get().getTask().getTaskRequest().getRequest();
      authorizationHelper.checkForAuthorization(request, user, SingularityAuthorizationScope.READ);
      return request.getGroup();
    } else {
      return getRequestGroup(taskId.getRequestId());
    }
  }

  private Optional<String> getRequestGroup(final String requestId) {
    final Optional<SingularityRequestWithState> maybeRequest = requestManager.getRequest(requestId);
    if (maybeRequest.isPresent()) {
      authorizationHelper.checkForAuthorization(maybeRequest.get().getRequest(), user, SingularityAuthorizationScope.READ);
      return maybeRequest.get().getRequest().getGroup();
    } else {
      Optional<SingularityRequestHistory> maybeRequestHistory = requestHistoryHelper.getLastHistory(requestId);
      if (maybeRequestHistory.isPresent()) {
        authorizationHelper.checkForAuthorization(maybeRequestHistory.get().getRequest(), user, SingularityAuthorizationScope.READ);
        return maybeRequestHistory.get().getRequest().getGroup();
      } else {
        // Deleted requests with no history data are searchable, but only by admins since we have no auth information about them
        authorizationHelper.checkAdminAuthorization(user);
        return Optional.absent();
      }
    }
  }

  private SingularityS3Log getS3Log(S3Configuration s3Configuration, String requestId, String key) throws Exception {
    try {
      Optional<String> group = getRequestGroup(requestId);

      final Date expireAt = new Date(System.currentTimeMillis() + s3Configuration.getExpireS3LinksAfterMillis());
      final String s3Bucket = (group.isPresent() && s3Configuration.getGroupOverrides().containsKey(group.get())) ? s3Configuration.getGroupOverrides().get(group.get()).getS3Bucket() : s3Configuration.getS3Bucket();
      final S3Service s3Service = (group.isPresent() && s3GroupOverride.containsKey(group.get())) ? s3GroupOverride.get(group.get()) : s3ServiceDefault.get();

      S3Object s3Object = s3Service.getObject(s3Bucket, key);
      String getUrl = s3Service.createSignedGetUrl(s3Bucket, s3Object.getKey(), expireAt);
      String downloadUrl = s3Service.createSignedUrl("GET", s3Bucket, s3Object.getKey(), FORCE_DOWNLOAD_S3_PARAMS, null, expireAt.getTime() / 1000, false);
      return new SingularityS3Log(getUrl, s3Object.getKey(), s3Object.getLastModifiedDate().getTime(), s3Object.getContentLength(), downloadUrl);
    } catch (S3ServiceException e) {
      if (e.getResponseCode() == 404) {
        throw WebExceptions.notFound(String.format("Object with key %s does not exist", key));
      }

      throw e;
    }
  }

  @GET
  @Path("/task/{taskId}")
  @ApiOperation("Retrieve the list of logs stored in S3 for a specific task.")
  public List<SingularityS3Log> getS3LogsForTask(
      @ApiParam("The task ID to search for") @PathParam("taskId") String taskId,
      @ApiParam("Start timestamp (millis, 13 digit)") @QueryParam("start") Optional<Long> start,
      @ApiParam("End timestamp (mills, 13 digit)") @QueryParam("end") Optional<Long> end) throws Exception {
    checkS3();

    SingularityTaskId taskIdObject = getTaskIdObject(taskId);

    try {
      return getS3Logs(configuration.get(), getRequestGroupForTask(taskIdObject), getS3PrefixesForTask(configuration.get(), taskIdObject, start, end));
    } catch (TimeoutException te) {
      throw timeout("Timed out waiting for response from S3 for %s", taskId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  @GET
  @Path("/request/{requestId}")
  @ApiOperation("Retrieve the list of logs stored in S3 for a specific request.")
  public List<SingularityS3Log> getS3LogsForRequest(
      @ApiParam("The request ID to search for") @PathParam("requestId") String requestId,
      @ApiParam("Start timestamp (millis, 13 digit)") @QueryParam("start") Optional<Long> start,
      @ApiParam("End timestamp (mills, 13 digit)") @QueryParam("end") Optional<Long> end) throws Exception {
    checkS3();

    try {
      return getS3Logs(configuration.get(), getRequestGroup(requestId), getS3PrefixesForRequest(configuration.get(), requestId, start, end));
    } catch (TimeoutException te) {
      throw timeout("Timed out waiting for response from S3 for %s", requestId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}")
  @ApiOperation("Retrieve the list of logs stored in S3 for a specific deploy.")
  public List<SingularityS3Log> getS3LogsForDeploy(
      @ApiParam("The request ID to search for") @PathParam("requestId") String requestId,
      @ApiParam("The deploy ID to search for") @PathParam("deployId") String deployId,
      @ApiParam("Start timestamp (millis, 13 digit)") @QueryParam("start") Optional<Long> start,
      @ApiParam("End timestamp (mills, 13 digit)") @QueryParam("end") Optional<Long> end) throws Exception {
    checkS3();

    try {
      return getS3Logs(configuration.get(), getRequestGroup(requestId), getS3PrefixesForDeploy(configuration.get(), requestId, deployId, start, end));
    } catch (TimeoutException te) {
      throw timeout("Timed out waiting for response from S3 for %s-%s", requestId, deployId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

}
