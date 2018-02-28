package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkBadRequest;
import static com.hubspot.singularity.WebExceptions.checkNotFound;
import static com.hubspot.singularity.WebExceptions.timeout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.Consumes;
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
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
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
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.ContinuationToken;
import com.hubspot.singularity.api.SingularityS3SearchRequest;
import com.hubspot.singularity.api.SingularityS3SearchResult;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.S3Configuration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.RequestHistoryHelper;
import com.hubspot.singularity.helpers.S3ObjectSummaryHolder;
import com.hubspot.singularity.helpers.SingularityS3Service;
import com.hubspot.singularity.helpers.SingularityS3Services;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import io.dropwizard.auth.Auth;

@Path(ApiPaths.S3_LOG_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity task logs stored in S3.", value=ApiPaths.S3_LOG_RESOURCE_PATH)
public class S3LogResource extends AbstractHistoryResource {
  private static final Logger LOG = LoggerFactory.getLogger(S3LogResource.class);
  private static final String CONTENT_DISPOSITION_DOWNLOAD_HEADER = "attachment";
  private static final String CONTENT_ENCODING_DOWNLOAD_HEADER = "identity";
  private static final String CONTINUATION_TOKEN_KEY_FORMAT = "%s-%s-%s";
  private static final int DEFAULT_TARGET_MAX_RESULTS = 10;

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
      Optional<S3Configuration> configuration, SingularityAuthorizationHelper authorizationHelper, SingularityS3Services s3Services) {
    super(historyManager, taskManager, deployManager, authorizationHelper);
    this.requestManager = requestManager;
    this.configuration = configuration;
    this.requestHistoryHelper = requestHistoryHelper;
    this.s3Services = s3Services;
  }

  // Generation of prefixes
  private Collection<String> getS3PrefixesForTask(S3Configuration s3Configuration, SingularityTaskId taskId, Optional<Long> startArg, Optional<Long> endArg, String group, SingularityUser user) {
    Optional<SingularityTaskHistory> history = getTaskHistory(taskId, user);

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

    Collection<String> prefixes = SingularityS3FormatHelper.getS3KeyPrefixes(s3Configuration.getS3KeyFormat(), taskId, tag, start, end, group);
    for (SingularityS3UploaderFile additionalFile : s3Configuration.getS3UploaderAdditionalFiles()) {
      if (additionalFile.getS3UploaderKeyPattern().isPresent() && !additionalFile.getS3UploaderKeyPattern().get().equals(s3Configuration.getS3KeyFormat())) {
        prefixes.addAll(SingularityS3FormatHelper.getS3KeyPrefixes(additionalFile.getS3UploaderKeyPattern().get(), taskId, tag, start, end, group));
      }
    }

    LOG.trace("Task {} got S3 prefixes {} for start {}, end {}, tag {}", taskId, prefixes, start, end, tag);

    return prefixes;
  }

  private boolean isCurrentDeploy(String requestId, String deployId) {
    return deployId.equals(deployManager.getInUseDeployId(requestId).orNull());
  }

  private Collection<String> getS3PrefixesForRequest(S3Configuration s3Configuration, String requestId, Optional<Long> startArg, Optional<Long> endArg, String group) {
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

    Collection<String> prefixes = SingularityS3FormatHelper.getS3KeyPrefixes(s3Configuration.getS3KeyFormat(), requestId, start, end, group);
    for (SingularityS3UploaderFile additionalFile : s3Configuration.getS3UploaderAdditionalFiles()) {
      if (additionalFile.getS3UploaderKeyPattern().isPresent() && !additionalFile.getS3UploaderKeyPattern().get().equals(s3Configuration.getS3KeyFormat())) {
        prefixes.addAll(SingularityS3FormatHelper.getS3KeyPrefixes(additionalFile.getS3UploaderKeyPattern().get(), requestId, start, end, group));
      }
    }

    LOG.trace("Request {} got S3 prefixes {} for start {}, end {}", requestId, prefixes, start, end);

    return prefixes;
  }

  private Collection<String> getS3PrefixesForDeploy(S3Configuration s3Configuration, String requestId, String deployId, Optional<Long> startArg, Optional<Long> endArg, String group, SingularityUser user) {
    SingularityDeployHistory deployHistory = getDeployHistory(requestId, deployId, user);

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

    Collection<String> prefixes = SingularityS3FormatHelper.getS3KeyPrefixes(s3Configuration.getS3KeyFormat(), requestId, deployId, tag, start, end, group);
    for (SingularityS3UploaderFile additionalFile : s3Configuration.getS3UploaderAdditionalFiles()) {
      if (additionalFile.getS3UploaderKeyPattern().isPresent() && !additionalFile.getS3UploaderKeyPattern().get().equals(s3Configuration.getS3KeyFormat())) {
        prefixes.addAll(SingularityS3FormatHelper.getS3KeyPrefixes(additionalFile.getS3UploaderKeyPattern().get(), requestId, deployId, tag, start, end, group));
      }
    }

    LOG.trace("Request {}, deploy {} got S3 prefixes {} for start {}, end {}, tag {}", requestId, deployId, prefixes, start, end, tag);

    return prefixes;
  }

  private Map<SingularityS3Service, Set<String>> getServiceToPrefixes(SingularityS3SearchRequest search, SingularityUser user) {
    Map<SingularityS3Service, Set<String>> servicesToPrefixes = new HashMap<>();

    if (!search.getTaskIds().isEmpty()) {
      for (String taskId : search.getTaskIds()) {
        SingularityTaskId taskIdObject = getTaskIdObject(taskId);
        String group = getRequestGroupForTask(taskIdObject, user).or(SingularityS3FormatHelper.DEFAULT_GROUP_NAME);
        Set<String> s3Buckets = getBuckets(group);
        Collection<String> prefixes = getS3PrefixesForTask(configuration.get(), taskIdObject, search.getStart(), search.getEnd(), group, user);
        for (String s3Bucket : s3Buckets) {
          SingularityS3Service s3Service = s3Services.getServiceByGroupAndBucketOrDefault(group, s3Bucket);
          if (!servicesToPrefixes.containsKey(s3Service)) {
            servicesToPrefixes.put(s3Service, new HashSet<String>());
          }
          servicesToPrefixes.get(s3Service).addAll(prefixes);
        }
      }
    }
    if (!search.getRequestsAndDeploys().isEmpty()) {
      for (Map.Entry<String, List<String>> entry : search.getRequestsAndDeploys().entrySet()) {
        String group = getRequestGroup(entry.getKey(), user).or(SingularityS3FormatHelper.DEFAULT_GROUP_NAME);
        Set<String> s3Buckets = getBuckets(group);
        List<String> prefixes = new ArrayList<>();
        if (!entry.getValue().isEmpty()) {
          for (String deployId : entry.getValue()) {
            prefixes.addAll(getS3PrefixesForDeploy(configuration.get(), entry.getKey(), deployId, search.getStart(), search.getEnd(), group, user));
          }
        } else {
          prefixes.addAll(getS3PrefixesForRequest(configuration.get(), entry.getKey(), search.getStart(), search.getEnd(), group));
        }
        for (String s3Bucket : s3Buckets) {
          SingularityS3Service s3Service = s3Services.getServiceByGroupAndBucketOrDefault(group, s3Bucket);
          if (!servicesToPrefixes.containsKey(s3Service)) {
            servicesToPrefixes.put(s3Service, new HashSet<String>());
          }
          servicesToPrefixes.get(s3Service).addAll(prefixes);
        }
      }
    }

    // Trim prefixes to search. Less specific prefixes will contain all results of matching + more specific ones
    for (Map.Entry<SingularityS3Service, Set<String>> entry : servicesToPrefixes.entrySet()) {
      Set<String> results = new HashSet<>();
      boolean contains = false;
      for (String prefix : entry.getValue()) {
        for (String unique : results) {
          if (prefix.startsWith(unique) && prefix.length() > unique.length()) {
            contains = true;
            break;
          } else if (unique.startsWith(prefix) && unique.length() > prefix.length()) {
            results.remove(unique);
            results.add(prefix);
            contains = true;
            break;
          }
        }
        if (!contains) {
          results.add(prefix);
        }
      }
      entry.getValue().retainAll(results);
    }

    return servicesToPrefixes;
  }

  private Set<String> getBuckets(String group) {
    Set<String> s3Buckets = new HashSet<>();
    s3Buckets.add(configuration.get().getGroupOverrides().containsKey(group) ? configuration.get().getGroupOverrides().get(group).getS3Bucket() : configuration.get().getS3Bucket());
    s3Buckets.add(configuration.get().getGroupS3SearchConfigs().containsKey(group) ? configuration.get().getGroupS3SearchConfigs().get(group).getS3Bucket() : configuration.get().getS3Bucket());
    for (SingularityS3UploaderFile uploaderFile : configuration.get().getS3UploaderAdditionalFiles()) {
      if (uploaderFile.getS3UploaderBucket().isPresent() && !s3Buckets.contains(uploaderFile.getS3UploaderBucket().get())) {
        s3Buckets.add(uploaderFile.getS3UploaderBucket().get());
      }
    }
    return s3Buckets;
  }

  // Fetching logs
  private List<SingularityS3LogMetadata> getS3LogsWithExecutorService(S3Configuration s3Configuration, ListeningExecutorService executorService, Map<SingularityS3Service, Set<String>> servicesToPrefixes, int totalPrefixCount, final SingularityS3SearchRequest search, final ConcurrentHashMap<String, ContinuationToken> continuationTokens, final boolean paginated) throws InterruptedException, ExecutionException, TimeoutException {
    List<ListenableFuture<List<S3ObjectSummaryHolder>>> futures = Lists.newArrayListWithCapacity(totalPrefixCount);

    final AtomicInteger resultCount = new AtomicInteger();

    for (final Map.Entry<SingularityS3Service, Set<String>> entry : servicesToPrefixes.entrySet()) {
      final String s3Bucket = entry.getKey().getBucket();
      final String group = entry.getKey().getGroup();
      final AmazonS3 s3Client = entry.getKey().getS3Client();

      for (final String s3Prefix : entry.getValue()) {
        final String key = String.format(CONTINUATION_TOKEN_KEY_FORMAT, group, s3Bucket, s3Prefix);
        if (search.getContinuationTokens().containsKey(key) && search.getContinuationTokens().get(key).isLastPage()) {
          LOG.trace("No further content for prefix {} in bucket {}, skipping", s3Prefix, s3Bucket);
          continuationTokens.putIfAbsent(key, search.getContinuationTokens().get(key));
          continue;
        }
        futures.add(executorService.submit(new Callable<List<S3ObjectSummaryHolder>>() {

          @Override
          public List<S3ObjectSummaryHolder> call() throws Exception {
            ListObjectsV2Request request = new ListObjectsV2Request().withBucketName(s3Bucket).withPrefix(s3Prefix);
            if (paginated) {
              Optional<ContinuationToken> token = Optional.absent();
              if (search.getContinuationTokens().containsKey(key) && !Strings.isNullOrEmpty(search.getContinuationTokens().get(key).getValue())) {
                request.setContinuationToken(search.getContinuationTokens().get(key).getValue());
                token = Optional.of(search.getContinuationTokens().get(key));
              }
              int targetResultCount = search.getMaxPerPage().or(DEFAULT_TARGET_MAX_RESULTS);
              request.setMaxKeys(targetResultCount);
              if (resultCount.get() < targetResultCount) {
                ListObjectsV2Result result = s3Client.listObjectsV2(request);
                if (result.getObjectSummaries().isEmpty()) {
                  continuationTokens.putIfAbsent(key, new ContinuationToken(result.getNextContinuationToken(), true));
                  return Collections.emptyList();
                } else {
                  boolean addToList = incrementIfLessThan(resultCount, result.getObjectSummaries().size(), targetResultCount);
                  if (addToList) {
                    continuationTokens.putIfAbsent(key, new ContinuationToken(result.getNextContinuationToken(), !result.isTruncated()));
                    List<S3ObjectSummaryHolder> objectSummaryHolders = new ArrayList<>();
                    for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                      objectSummaryHolders.add(new S3ObjectSummaryHolder(group, objectSummary));
                    }
                    return objectSummaryHolders;
                  } else {
                    continuationTokens.putIfAbsent(key, token.or(new ContinuationToken(null, false)));
                    return Collections.emptyList();
                  }
                }
              } else {
                continuationTokens.putIfAbsent(key, token.or(new ContinuationToken(null, false)));
                return Collections.emptyList();
              }
            } else {
              ListObjectsV2Result result = s3Client.listObjectsV2(request);
              List<S3ObjectSummaryHolder> objectSummaryHolders = new ArrayList<>();
              for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                objectSummaryHolders.add(new S3ObjectSummaryHolder(group, objectSummary));
              }
              return objectSummaryHolders;
            }
          }
        }));
      }
    }

    final long start = System.currentTimeMillis();
    List<List<S3ObjectSummaryHolder>> results = Futures.allAsList(futures).get(s3Configuration.getWaitForS3ListSeconds(), TimeUnit.SECONDS);

    List<S3ObjectSummaryHolder> objects = Lists.newArrayListWithExpectedSize(results.size() * 2);

    for (List<S3ObjectSummaryHolder> s3ObjectSummaryHolders : results) {
      for (final S3ObjectSummaryHolder s3ObjectHolder : s3ObjectSummaryHolders) {
        objects.add(s3ObjectHolder);
      }
    }

    LOG.trace("Got {} objects from S3 after {}", objects.size(), JavaUtils.duration(start));

    List<ListenableFuture<SingularityS3LogMetadata>> logFutures = Lists.newArrayListWithCapacity(objects.size());
    final Date expireAt = new Date(System.currentTimeMillis() + s3Configuration.getExpireS3LinksAfterMillis());

    for (final S3ObjectSummaryHolder s3ObjectHolder : objects) {
      final S3ObjectSummary s3Object = s3ObjectHolder.getObjectSummary();
      final AmazonS3 s3Client = s3Services.getServiceByGroupAndBucketOrDefault(s3ObjectHolder.getGroup(), s3Object.getBucketName()).getS3Client();

      logFutures.add(executorService.submit(new Callable<SingularityS3LogMetadata>() {
        @Override
        public SingularityS3LogMetadata call() throws Exception {
          Optional<Long> maybeStartTime = Optional.absent();
          Optional<Long> maybeEndTime = Optional.absent();
          if (!search.isExcludeMetadata()) {
            GetObjectMetadataRequest metadataRequest = new GetObjectMetadataRequest(s3Object.getBucketName(), s3Object.getKey());
            Map<String, String> objectMetadata = s3Client.getObjectMetadata(metadataRequest).getUserMetadata();
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

  private boolean incrementIfLessThan(AtomicInteger count, int add, int threshold) {
    while (true) {
      int current = count.get();
      if (current >= threshold) {
        return false;
      }
      if (count.compareAndSet(current, current + add)) {
        return true;
      }
    }
  }

  private Optional<Long> getMetadataAsLong(Map<String, String> objectMetadata, String keyName) {
    try {
      if (objectMetadata.containsKey(keyName)) {
        Object maybeLong = objectMetadata.get(keyName);
        return Optional.of(Long.parseLong((String) maybeLong));
      } else {
        return Optional.absent();
      }
    } catch (Exception e) {
      return Optional.absent();
    }
  }

  private SingularityS3SearchResult getS3Logs(S3Configuration s3Configuration, Map<SingularityS3Service, Set<String>> servicesToPrefixes, final SingularityS3SearchRequest search, final boolean paginated) throws InterruptedException, ExecutionException, TimeoutException {
    int totalPrefixCount = 0;
    for (Map.Entry<SingularityS3Service, Set<String>> entry : servicesToPrefixes.entrySet()) {
      totalPrefixCount += entry.getValue().size();
    }

    if (totalPrefixCount == 0) {
      return SingularityS3SearchResult.empty();
    }

    ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(Math.min(totalPrefixCount, s3Configuration.getMaxS3Threads()),
        new ThreadFactoryBuilder().setNameFormat("S3LogFetcher-%d").build()));

    try {
      final ConcurrentHashMap<String, ContinuationToken> continuationTokens = new ConcurrentHashMap<>();
      List<SingularityS3LogMetadata> logs = Lists.newArrayList(getS3LogsWithExecutorService(s3Configuration, executorService, servicesToPrefixes, totalPrefixCount, search, continuationTokens, paginated));
      Collections.sort(logs, LOG_COMPARATOR);
      return new SingularityS3SearchResult(continuationTokens, isFinalPageForAllPrefixes(continuationTokens.values()), logs);
    } finally {
      executorService.shutdownNow();
    }
  }

  private boolean isFinalPageForAllPrefixes(Collection<ContinuationToken> continuationTokens) {
    for (ContinuationToken token : continuationTokens) {
      if (!token.isLastPage()) {
        return false;
      }
    }
    return true;
  }

  // Finding request group
  private Optional<String> getRequestGroupForTask(final SingularityTaskId taskId, SingularityUser user) {
    Optional<SingularityTaskHistory> maybeTaskHistory = getTaskHistory(taskId, user);
    if (maybeTaskHistory.isPresent()) {
      SingularityRequest request = maybeTaskHistory.get().getTask().getTaskRequest().getRequest();
      authorizationHelper.checkForAuthorization(request, user, SingularityAuthorizationScope.READ);
      return request.getGroup();
    } else {
      return getRequestGroup(taskId.getRequestId(), user);
    }
  }

  private Optional<String> getRequestGroup(final String requestId, SingularityUser user) {
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

  private void checkS3() {
    checkNotFound(s3Services.isS3ConfigPresent(), "S3 configuration was absent");
    checkNotFound(configuration.isPresent(), "S3 configuration was absent");
  }

  @GET
  @Path("/task/{taskId}")
  @ApiOperation("Retrieve the list of logs stored in S3 for a specific task.")
  public List<SingularityS3LogMetadata> getS3LogsForTask(
      @Auth SingularityUser user,
      @ApiParam("The task ID to search for") @PathParam("taskId") String taskId,
      @ApiParam("Start timestamp (millis, 13 digit)") @QueryParam("start") Optional<Long> start,
      @ApiParam("End timestamp (mills, 13 digit)") @QueryParam("end") Optional<Long> end,
      @ApiParam("Exclude custom object metadata") @QueryParam("excludeMetadata") boolean excludeMetadata,
      @ApiParam("Do not generate download/get urls, only list the files and metadata") @QueryParam("list") boolean listOnly) throws Exception {
    checkS3();

    final SingularityS3SearchRequest search = new SingularityS3SearchRequest(
        Collections.<String, List<String>>emptyMap(),
        Collections.singletonList(taskId),
        start,
        end,
        excludeMetadata,
        listOnly,
        Optional.<Integer>absent(),
        Collections.<String, ContinuationToken>emptyMap());

    try {
      return getS3Logs(configuration.get(), getServiceToPrefixes(search, user), search, false).getResults();
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
      @Auth SingularityUser user,
      @ApiParam("The request ID to search for") @PathParam("requestId") String requestId,
      @ApiParam("Start timestamp (millis, 13 digit)") @QueryParam("start") Optional<Long> start,
      @ApiParam("End timestamp (mills, 13 digit)") @QueryParam("end") Optional<Long> end,
      @ApiParam("Exclude custom object metadata") @QueryParam("excludeMetadata") boolean excludeMetadata,
      @ApiParam("Do not generate download/get urls, only list the files and metadata") @QueryParam("list") boolean listOnly,
      @ApiParam("Max number of results to return per bucket searched") @QueryParam("maxPerPage") Optional<Integer> maxPerPage) throws Exception {
    checkS3();

    try {
      final SingularityS3SearchRequest search = new SingularityS3SearchRequest(
          ImmutableMap.of(requestId, Collections.<String>emptyList()),
          Collections.<String>emptyList(),
          start,
          end,
          excludeMetadata,
          listOnly,
          Optional.<Integer>absent(),
          Collections.<String, ContinuationToken>emptyMap());

      return getS3Logs(configuration.get(), getServiceToPrefixes(search, user), search, false).getResults();
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
      @Auth SingularityUser user,
      @ApiParam("The request ID to search for") @PathParam("requestId") String requestId,
      @ApiParam("The deploy ID to search for") @PathParam("deployId") String deployId,
      @ApiParam("Start timestamp (millis, 13 digit)") @QueryParam("start") Optional<Long> start,
      @ApiParam("End timestamp (mills, 13 digit)") @QueryParam("end") Optional<Long> end,
      @ApiParam("Exclude custom object metadata") @QueryParam("excludeMetadata") boolean excludeMetadata,
      @ApiParam("Do not generate download/get urls, only list the files and metadata") @QueryParam("list") boolean listOnly,
      @ApiParam("Max number of results to return per bucket searched") @QueryParam("maxPerPage") Optional<Integer> maxPerPage) throws Exception {
    checkS3();

    try {
      final SingularityS3SearchRequest search = new SingularityS3SearchRequest(
          ImmutableMap.of(requestId, Collections.singletonList(deployId)),
          Collections.<String>emptyList(),
          start,
          end,
          excludeMetadata,
          listOnly,
          Optional.<Integer>absent(),
          Collections.<String, ContinuationToken>emptyMap());

      return getS3Logs(configuration.get(), getServiceToPrefixes(search, user), search, false).getResults();
    } catch (TimeoutException te) {
      throw timeout("Timed out waiting for response from S3 for %s-%s", requestId, deployId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  @POST
  @Path("/search")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation("Retrieve a paginated list of logs stored in S3")
  public SingularityS3SearchResult getPaginatedS3Logs(@Auth SingularityUser user, @ApiParam(required = true) SingularityS3SearchRequest search) throws Exception {
    checkS3();

    checkBadRequest(!search.getRequestsAndDeploys().isEmpty() || !search.getTaskIds().isEmpty(), "Must specify at least one request or task to search");

    try {
      return getS3Logs(configuration.get(), getServiceToPrefixes(search, user), search, true);
    } catch (TimeoutException te) {
      throw timeout("Timed out waiting for response from S3 for %s", search);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
}
