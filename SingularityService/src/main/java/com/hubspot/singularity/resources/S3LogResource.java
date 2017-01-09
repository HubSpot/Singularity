package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkNotFound;
import static com.hubspot.singularity.WebExceptions.timeout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import com.amazonaws.services.s3.model.S3ObjectSummary;
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
import com.hubspot.singularity.SingularityS3LogMetadata;
import com.hubspot.singularity.SingularityS3UploaderFile;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.ContinuationToken;
import com.hubspot.singularity.api.SingularityS3SearchRequest;
import com.hubspot.singularity.api.SingularityS3SearchResult;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.S3Configuration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.RequestHistoryHelper;
import com.hubspot.singularity.helpers.SingularityS3Services;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path(S3LogResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity task logs stored in S3.", value=S3LogResource.PATH)
public class S3LogResource extends AbstractHistoryResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/logs";
  private static final Logger LOG = LoggerFactory.getLogger(S3LogResource.class);
  private static final String CONTENT_DISPOSITION_DOWNLOAD_HEADER = "attachment";
  private static final String CONTENT_ENCODING_DOWNLOAD_HEADER = "identity";
  private static final int DEFAULT_MAX_PER_PAGE = 10;

  private final SingularityS3Services s3Services;
  private final Optional<S3Configuration> configuration;
  private final RequestHistoryHelper requestHistoryHelper;
  private final RequestManager requestManager;

  private static final Comparator<SingularityS3LogMetadata> LOG_COMPARATOR = new Comparator<SingularityS3LogMetadata>() {

    @Override
    public int compare(SingularityS3LogMetadata o1, SingularityS3LogMetadata o2) {
      return Longs.compare(o2.getLastModified(), o1.getLastModified());
    }

  };

  @Inject
  public S3LogResource(RequestManager requestManager, HistoryManager historyManager, RequestHistoryHelper requestHistoryHelper, TaskManager taskManager, DeployManager deployManager,
      Optional<S3Configuration> configuration, SingularityAuthorizationHelper authorizationHelper, Optional<SingularityUser> user,SingularityS3Services s3Services) {
    super(historyManager, taskManager, deployManager, authorizationHelper, user);
    this.requestManager = requestManager;
    this.configuration = configuration;
    this.requestHistoryHelper = requestHistoryHelper;
    this.s3Services = s3Services;
  }

  private Collection<String> getS3PrefixesForTask(S3Configuration s3Configuration, SingularityTaskId taskId, Optional<Long> startArg, Optional<Long> endArg, Optional<String> maybeGroup) {
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

    Collection<String> prefixes = SingularityS3FormatHelper.getS3KeyPrefixes(s3Configuration.getS3KeyFormat(), taskId, tag, start, end, maybeGroup.or(SingularityS3FormatHelper.DEFAULT_GROUP_NAME));
    for (SingularityS3UploaderFile additionalFile : s3Configuration.getS3UploaderAdditionalFiles()) {
      if (additionalFile.getS3UploaderKeyPattern().isPresent() && !additionalFile.getS3UploaderKeyPattern().get().equals(s3Configuration.getS3KeyFormat())) {
        prefixes.addAll(SingularityS3FormatHelper.getS3KeyPrefixes(additionalFile.getS3UploaderKeyPattern().get(), taskId, tag, start, end, maybeGroup.or(SingularityS3FormatHelper.DEFAULT_GROUP_NAME)));
      }
    }

    LOG.trace("Task {} got S3 prefixes {} for start {}, end {}, tag {}", taskId, prefixes, start, end, tag);

    return prefixes;
  }

  private boolean isCurrentDeploy(String requestId, String deployId) {
    return deployId.equals(deployManager.getInUseDeployId(requestId).orNull());
  }

  private Collection<String> getS3PrefixesForRequest(S3Configuration s3Configuration, String requestId, Optional<Long> startArg, Optional<Long> endArg, Optional<String> maybeGroup) {
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

    Collection<String> prefixes = SingularityS3FormatHelper.getS3KeyPrefixes(s3Configuration.getS3KeyFormat(), requestId, start, end, maybeGroup.or(SingularityS3FormatHelper.DEFAULT_GROUP_NAME));
    for (SingularityS3UploaderFile additionalFile : s3Configuration.getS3UploaderAdditionalFiles()) {
      if (additionalFile.getS3UploaderKeyPattern().isPresent() && !additionalFile.getS3UploaderKeyPattern().get().equals(s3Configuration.getS3KeyFormat())) {
        prefixes.addAll(SingularityS3FormatHelper.getS3KeyPrefixes(additionalFile.getS3UploaderKeyPattern().get(), requestId, start, end, maybeGroup.or(SingularityS3FormatHelper.DEFAULT_GROUP_NAME)));
      }
    }

    LOG.trace("Request {} got S3 prefixes {} for start {}, end {}", requestId, prefixes, start, end);

    return prefixes;
  }

  private Collection<String> getS3PrefixesForDeploy(S3Configuration s3Configuration, String requestId, String deployId, Optional<Long> startArg, Optional<Long> endArg, Optional<String> maybeGroup) {
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

    Collection<String> prefixes = SingularityS3FormatHelper.getS3KeyPrefixes(s3Configuration.getS3KeyFormat(), requestId, deployId, tag, start, end, maybeGroup.or(SingularityS3FormatHelper.DEFAULT_GROUP_NAME));
    for (SingularityS3UploaderFile additionalFile : s3Configuration.getS3UploaderAdditionalFiles()) {
      if (additionalFile.getS3UploaderKeyPattern().isPresent() && !additionalFile.getS3UploaderKeyPattern().get().equals(s3Configuration.getS3KeyFormat())) {
        prefixes.addAll(SingularityS3FormatHelper.getS3KeyPrefixes(additionalFile.getS3UploaderKeyPattern().get(), requestId, deployId, tag, start, end, maybeGroup.or(SingularityS3FormatHelper.DEFAULT_GROUP_NAME)));
      }
    }

    LOG.trace("Request {}, deploy {} got S3 prefixes {} for start {}, end {}, tag {}", requestId, deployId, prefixes, start, end, tag);

    return prefixes;
  }

  private List<SingularityS3LogMetadata> getS3LogsWithExecutorService(S3Configuration s3Configuration, Optional<String> group, ListeningExecutorService executorService, Collection<String> prefixes, final SingularityS3SearchRequest search, final Set<ContinuationToken> continuationTokens, final boolean paginated) throws InterruptedException, ExecutionException, TimeoutException {
    List<ListenableFuture<List<S3ObjectSummary>>> futures = Lists.newArrayListWithCapacity(prefixes.size());

    List<String> s3Buckets = new ArrayList<>();
    String defaultS3Bucket = (group.isPresent() && s3Configuration.getGroupOverrides().containsKey(group.get())) ? s3Configuration.getGroupOverrides().get(group.get()).getS3Bucket() : s3Configuration.getS3Bucket();
    s3Buckets.add(defaultS3Bucket);
    for (SingularityS3UploaderFile uploaderFile : s3Configuration.getS3UploaderAdditionalFiles()) {
      if (uploaderFile.getS3UploaderBucket().isPresent() && !uploaderFile.getS3UploaderBucket().get().equals(defaultS3Bucket)) {
        s3Buckets.add(uploaderFile.getS3UploaderBucket().get());
      }
    }

    for (final String s3Bucket : s3Buckets) {
      final AmazonS3 s3Client = s3Services.getServiceByGroupAndBucketOrDefault(group.or(SingularityS3FormatHelper.DEFAULT_GROUP_NAME), s3Bucket);

      for (final String s3Prefix : prefixes) {
        final Optional<ContinuationToken> maybeContinuationToken = getContinuationToken(s3Bucket, s3Prefix, search.getContinuationTokens());
        if (maybeContinuationToken.isPresent() && maybeContinuationToken.get().isLastPage()) {
          LOG.trace("No further content for prefix {} in bucket {}, skipping", s3Prefix, s3Bucket);
          continuationTokens.add(maybeContinuationToken.get());
        }
        futures.add(executorService.submit(new Callable<List<S3ObjectSummary>>() {

          @Override
          public List<S3ObjectSummary> call() throws Exception {
            ListObjectsV2Request request = new ListObjectsV2Request().withBucketName(s3Bucket).withPrefix(s3Prefix);
            if (paginated) {
              if (maybeContinuationToken.isPresent()) {
                request.setContinuationToken(maybeContinuationToken.get().getValue());
              }
              request.setMaxKeys(search.getMaxPerPage().or(DEFAULT_MAX_PER_PAGE));
            }

            ListObjectsV2Result result = s3Client.listObjectsV2(request);
            continuationTokens.add(new ContinuationToken(s3Bucket, s3Prefix, result.getNextContinuationToken(), result.getObjectSummaries().isEmpty()));

            return result.getObjectSummaries();
          }
        }));
      }
    }

    final long start = System.currentTimeMillis();
    List<List<S3ObjectSummary>> results = Futures.allAsList(futures).get(s3Configuration.getWaitForS3ListSeconds(), TimeUnit.SECONDS);

    List<S3ObjectSummary> objects = Lists.newArrayListWithExpectedSize(results.size() * 2);

    for (List<S3ObjectSummary> s3Objects : results) {
      for (final S3ObjectSummary s3Object : s3Objects) {
        objects.add(s3Object);
      }
    }

    LOG.trace("Got {} objects from S3 after {}", objects.size(), JavaUtils.duration(start));

    List<ListenableFuture<SingularityS3LogMetadata>> logFutures = Lists.newArrayListWithCapacity(objects.size());
    final Date expireAt = new Date(System.currentTimeMillis() + s3Configuration.getExpireS3LinksAfterMillis());

    for (final S3ObjectSummary s3Object : objects) {
      final AmazonS3 s3Client = s3Services.getServiceByGroupAndBucketOrDefault(group.or(SingularityS3FormatHelper.DEFAULT_GROUP_NAME), s3Object.getBucketName());

      logFutures.add(executorService.submit(new Callable<SingularityS3LogMetadata>() {
        @Override
        public SingularityS3LogMetadata call() throws Exception {
          Optional<Long> maybeStartTime = Optional.absent();
          Optional<Long> maybeEndTime = Optional.absent();
          if (!search.isExcludeMetadata()) {
            GetObjectMetadataRequest metadataRequest = new GetObjectMetadataRequest(s3Object.getBucketName(), s3Object.getKey());
            Map<String, Object> objectMetadata = s3Client.getObjectMetadata(metadataRequest).getRawMetadata();
            maybeStartTime = getMetadataAsLong(objectMetadata, SingularityS3Log.LOG_START_S3_ATTR);
            maybeEndTime = getMetadataAsLong(objectMetadata, SingularityS3Log.LOG_END_S3_ATTR);
          }

          if (search.isListOnly()) {
            return new SingularityS3LogMetadata(s3Object.getKey(), s3Object.getLastModified().getTime(), s3Object.getSize(), maybeStartTime, maybeEndTime);
          } else {
            GeneratePresignedUrlRequest getUrlRequest = new GeneratePresignedUrlRequest(s3Object.getBucketName(), s3Object.getKey())
                .withMethod(HttpMethod.GET)
                .withExpiration(expireAt);
            String getUrl = s3Client.generatePresignedUrl(getUrlRequest).toString();

            ResponseHeaderOverrides downloadHeaders = new ResponseHeaderOverrides();
            downloadHeaders.setContentDisposition(CONTENT_DISPOSITION_DOWNLOAD_HEADER);
            downloadHeaders.setContentEncoding(CONTENT_ENCODING_DOWNLOAD_HEADER);
            GeneratePresignedUrlRequest downloadUrlRequest = new GeneratePresignedUrlRequest(s3Object.getBucketName(), s3Object.getKey())
                .withMethod(HttpMethod.GET)
                .withExpiration(expireAt)
                .withResponseHeaders(downloadHeaders);
            String downloadUrl = s3Client.generatePresignedUrl(downloadUrlRequest).toString();

            return new SingularityS3Log(getUrl, s3Object.getKey(), s3Object.getLastModified().getTime(), s3Object.getSize(), downloadUrl, maybeStartTime, maybeEndTime);
          }
        }

      }));
    }

    return Futures.allAsList(logFutures).get(s3Configuration.getWaitForS3LinksSeconds(), TimeUnit.SECONDS);
  }

  private Optional<Long> getMetadataAsLong(Map<String, Object> objectMetadata, String keyName) {
    try {
      if (objectMetadata.containsKey(keyName)) {
        Object maybeLong = objectMetadata.get(keyName);
        if (maybeLong instanceof String) {
          return Optional.of(Long.parseLong((String) maybeLong));
        } else {
          return Optional.of((Long) maybeLong);
        }
      } else {
        return Optional.absent();
      }
    } catch (Exception e) {
      return Optional.absent();
    }
  }

  private SingularityS3SearchResult getS3Logs(S3Configuration s3Configuration, Optional<String> group, Collection<String> prefixes, final SingularityS3SearchRequest search, final boolean paginated) throws InterruptedException, ExecutionException, TimeoutException {
    if (prefixes.isEmpty()) {
      return SingularityS3SearchResult.empty();
    }

    ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(Math.min(prefixes.size(), s3Configuration.getMaxS3Threads()),
        new ThreadFactoryBuilder().setNameFormat("S3LogFetcher-%d").build()));

    try {
      final Set<ContinuationToken> continuationTokens = new HashSet<>();
      List<SingularityS3LogMetadata> logs = Lists.newArrayList(getS3LogsWithExecutorService(s3Configuration, group, executorService, prefixes, search, continuationTokens, paginated));
      Collections.sort(logs, LOG_COMPARATOR);
      return new SingularityS3SearchResult(continuationTokens, isFinalPageForAllPrefixes(continuationTokens), logs);
    } finally {
      executorService.shutdownNow();
    }
  }

  private boolean isFinalPageForAllPrefixes(Set<ContinuationToken> continuationTokens) {
    boolean finalPage = true;
    for (ContinuationToken token : continuationTokens) {
      finalPage = finalPage && token.isLastPage();
    }
    return finalPage;
  }

  private Optional<ContinuationToken> getContinuationToken(String bucket, String prefix, Set<ContinuationToken> tokens) {
    for (ContinuationToken token : tokens) {
      if (token.getBucket().equals(bucket) && token.getPrefix().equals(prefix)) {
        return Optional.of(token);
      }
    }
    return Optional.absent();
  }

  private void checkS3() {
    checkNotFound(s3Services.isS3ConfigPresent(), "S3 configuration was absent");
    checkNotFound(configuration.isPresent(), "S3 configuration was absent");
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

  @GET
  @Path("/task/{taskId}")
  @ApiOperation("Retrieve the list of logs stored in S3 for a specific task.")
  public List<SingularityS3LogMetadata> getS3LogsForTask(
      @ApiParam("The task ID to search for") @PathParam("taskId") String taskId,
      @ApiParam("Start timestamp (millis, 13 digit)") @QueryParam("start") Optional<Long> start,
      @ApiParam("End timestamp (mills, 13 digit)") @QueryParam("end") Optional<Long> end,
      @ApiParam("Exclude custom object metadata") @QueryParam("excludeMetadata") Optional<Boolean> excludeMetadata,
      @ApiParam("Do not generate download/get urls, only list the files and metadata") @QueryParam("list") Optional<Boolean> listOnly) throws Exception {
    checkS3();

    SingularityTaskId taskIdObject = getTaskIdObject(taskId);

    final SingularityS3SearchRequest search = new SingularityS3SearchRequest(start, end, excludeMetadata,listOnly, Optional.<Integer>absent(), Collections.<ContinuationToken>emptySet());

    try {
      Optional<String> maybeGroup = getRequestGroupForTask(taskIdObject);
      return getS3Logs(configuration.get(), getRequestGroupForTask(taskIdObject), getS3PrefixesForTask(configuration.get(), taskIdObject, start, end, maybeGroup), search, false).getResults();
    } catch (TimeoutException te) {
      throw timeout("Timed out waiting for response from S3 for %s", taskId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  @POST
  @Path("/task/{taskId}/paginated")
  @ApiOperation("Retrieve the list of logs stored in S3 for a specific task.")
  public SingularityS3SearchResult getPaginatedS3LogsForTask(
      @ApiParam("The task ID to search for") @PathParam("taskId") String taskId,
      @ApiParam(required = true) SingularityS3SearchRequest search) throws Exception {
    checkS3();
    SingularityTaskId taskIdObject = getTaskIdObject(taskId);

    try {
      Optional<String> maybeGroup = getRequestGroupForTask(taskIdObject);
      return getS3Logs(configuration.get(), getRequestGroupForTask(taskIdObject), getS3PrefixesForTask(configuration.get(), taskIdObject, search.getStart(), search.getEnd(), maybeGroup), search, true);
    } catch (TimeoutException te) {
      throw timeout("Timed out waiting for response from S3 for %s", taskId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  @GET
  @Path("/request/{requestId}")
  @ApiOperation("Retrieve the list of logs stored in S3 for a specific request.")
  public List<SingularityS3LogMetadata> getS3LogsForRequest(
      @ApiParam("The request ID to search for") @PathParam("requestId") String requestId,
      @ApiParam("Start timestamp (millis, 13 digit)") @QueryParam("start") Optional<Long> start,
      @ApiParam("End timestamp (mills, 13 digit)") @QueryParam("end") Optional<Long> end,
      @ApiParam("Exclude custom object metadata") @QueryParam("excludeMetadata") Optional<Boolean> excludeMetadata,
      @ApiParam("Do not generate download/get urls, only list the files and metadata") @QueryParam("list") Optional<Boolean> listOnly,
      @ApiParam("Max number of results to return per bucket searched") @QueryParam("maxPerPage") Optional<Integer> maxPerPage) throws Exception {
    checkS3();

    try {
      Optional<String> maybeGroup = getRequestGroup(requestId);
      final SingularityS3SearchRequest search = new SingularityS3SearchRequest(start, end, excludeMetadata,listOnly, Optional.<Integer>absent(), Collections.<ContinuationToken>emptySet());

      return getS3Logs(configuration.get(), getRequestGroup(requestId), getS3PrefixesForRequest(configuration.get(), requestId, start, end, maybeGroup), search, false).getResults();
    } catch (TimeoutException te) {
      throw timeout("Timed out waiting for response from S3 for %s", requestId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  @POST
  @Path("/request/{requestId}/paginated")
  @ApiOperation("Retrieve the list of logs stored in S3 for a specific request.")
  public SingularityS3SearchResult getPaginatedS3LogsForRequest(
      @ApiParam("The request ID to search for") @PathParam("requestId") String requestId,
      @ApiParam(required = true) SingularityS3SearchRequest search) throws Exception {
    checkS3();

    try {
      Optional<String> maybeGroup = getRequestGroup(requestId);
      return getS3Logs(configuration.get(), getRequestGroup(requestId), getS3PrefixesForRequest(configuration.get(), requestId, search.getStart(), search.getEnd(), maybeGroup), search, true);
    } catch (TimeoutException te) {
      throw timeout("Timed out waiting for response from S3 for %s", requestId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}")
  @ApiOperation("Retrieve the list of logs stored in S3 for a specific deploy.")
  public List<SingularityS3LogMetadata> getS3LogsForDeploy(
      @ApiParam("The request ID to search for") @PathParam("requestId") String requestId,
      @ApiParam("The deploy ID to search for") @PathParam("deployId") String deployId,
      @ApiParam("Start timestamp (millis, 13 digit)") @QueryParam("start") Optional<Long> start,
      @ApiParam("End timestamp (mills, 13 digit)") @QueryParam("end") Optional<Long> end,
      @ApiParam("Exclude custom object metadata") @QueryParam("excludeMetadata") Optional<Boolean> excludeMetadata,
      @ApiParam("Do not generate download/get urls, only list the files and metadata") @QueryParam("list") Optional<Boolean> listOnly,
      @ApiParam("Max number of results to return per bucket searched") @QueryParam("maxPerPage") Optional<Integer> maxPerPage) throws Exception {
    checkS3();

    try {
      Optional<String> maybeGroup = getRequestGroup(requestId);
      final SingularityS3SearchRequest search = new SingularityS3SearchRequest(start, end, excludeMetadata,listOnly, Optional.<Integer>absent(), Collections.<ContinuationToken>emptySet());

      return getS3Logs(configuration.get(), maybeGroup, getS3PrefixesForDeploy(configuration.get(), requestId, deployId, start, end, maybeGroup), search, false).getResults();
    } catch (TimeoutException te) {
      throw timeout("Timed out waiting for response from S3 for %s-%s", requestId, deployId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  @POST
  @Path("/request/{requestId}/deploy/{deployId}/paginated")
  @ApiOperation("Retrieve the list of logs stored in S3 for a specific deploy.")
  public SingularityS3SearchResult getPaginatedS3LogsForDeploy(
      @ApiParam("The request ID to search for") @PathParam("requestId") String requestId,
      @ApiParam("The deploy ID to search for") @PathParam("deployId") String deployId,
      @ApiParam(required = true) SingularityS3SearchRequest search) throws Exception {
    checkS3();

    try {
      Optional<String> maybeGroup = getRequestGroup(requestId);
      return getS3Logs(configuration.get(), maybeGroup, getS3PrefixesForDeploy(configuration.get(), requestId, deployId, search.getStart(), search.getEnd(), maybeGroup), search, true);
    } catch (TimeoutException te) {
      throw timeout("Timed out waiting for response from S3 for %s-%s", requestId, deployId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
}
