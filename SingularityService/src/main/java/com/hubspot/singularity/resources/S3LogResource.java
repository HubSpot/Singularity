package com.hubspot.singularity.resources;

import java.util.Collection;
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

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityS3FormatHelper;
import com.hubspot.singularity.SingularityS3Log;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.config.S3Configuration;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;

@Path("/logs")
@Produces({ MediaType.APPLICATION_JSON })
public class S3LogResource extends AbstractHistoryResource {

  private final Optional<S3Service> s3;
  private final Optional<S3Configuration> configuration;
  
  @Inject
  public S3LogResource(HistoryManager historyManager, TaskManager taskManager, Optional<S3Service> s3, Optional<S3Configuration> configuration) {
    super(historyManager, taskManager);
    this.s3 = s3;
    this.configuration = configuration;
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
    
    return SingularityS3FormatHelper.getS3KeyPrefixes(configuration.get().getS3KeyFormat(), taskId, tag, start, end);
  }
  
  private Collection<SingularityS3Log> getS3Logs(SingularityTaskId taskIdObject) throws InterruptedException, ExecutionException, TimeoutException {
    Collection<String> prefixes = getS3PrefixesForTask(taskIdObject);
    
    ListeningExecutorService es = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(Math.min(prefixes.size(), configuration.get().getMaxS3Threads()), new ThreadFactoryBuilder().setNameFormat(taskIdObject.toString() + "S3Fetcher" + "-%d").build()));
    
    List<ListenableFuture<S3Object[]>> futures = Lists.newArrayListWithCapacity(prefixes.size());
    
    for (final String s3Prefix : prefixes) {
      futures.add(es.submit(new Callable<S3Object[]>() {

        @Override
        public S3Object[] call() throws Exception {
          return s3.get().listObjects(configuration.get().getS3Bucket(), s3Prefix, null);
        }
      }));
    }
    
    List<S3Object[]> results = Futures.allAsList(futures).get(configuration.get().getWaitForS3ListSeconds(), TimeUnit.SECONDS);
    List<ListenableFuture<SingularityS3Log>> logFutures = Lists.newArrayListWithCapacity(results.size() * 2);
    
    final Date expireAt = new Date(System.currentTimeMillis() + configuration.get().getExpireS3LinksAfterMillis());
    
    for (S3Object[] s3Objects : results) {
      for (final S3Object s3Object : s3Objects) {
        logFutures.add(es.submit(new Callable<SingularityS3Log>() {

          @Override
          public SingularityS3Log call() throws Exception {
            String getUrl = s3.get().createSignedGetUrl(configuration.get().getS3Bucket(), s3Object.getKey(), expireAt);
            
            return new SingularityS3Log(getUrl, s3Object.getKey(), s3Object.getLastModifiedDate().getTime(), s3Object.getContentLength());
          }
        }));
      }
    }
    
    return Futures.allAsList(logFutures).get(configuration.get().getWaitForS3LinksSeconds(), TimeUnit.SECONDS);
  }
  
  @GET
  @Path("task/{taskId}")
  public Collection<SingularityS3Log> getS3Logs(@PathParam("taskId") String taskId) throws Exception {
    if (!s3.isPresent()) {
      throw WebExceptions.webException(501, "S3 configuration was absent");
    }
    
    SingularityTaskId taskIdObject = getTaskIdObject(taskId);
    
    try {
      return getS3Logs(taskIdObject);
    } catch (TimeoutException te) {
      throw WebExceptions.timeout("Timed out waiting for response from S3 for %s", taskId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
  
}
