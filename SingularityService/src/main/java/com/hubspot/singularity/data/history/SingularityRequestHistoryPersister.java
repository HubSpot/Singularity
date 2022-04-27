package com.hubspot.singularity.data.history;

import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityHistoryItem;
import com.hubspot.singularity.SingularityManagedThreadPoolFactory;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.async.CompletableFutures;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.history.SingularityRequestHistoryPersister.SingularityRequestHistoryParent;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityRequestHistoryPersister
  extends SingularityHistoryPersister<SingularityRequestHistoryParent> {

  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityRequestHistoryPersister.class
  );

  private final RequestManager requestManager;
  private final HistoryManager historyManager;
  private final SingularitySchedulerLock lock;

  @Inject
  public SingularityRequestHistoryPersister(
    SingularityConfiguration configuration,
    RequestManager requestManager,
    HistoryManager historyManager,
    SingularitySchedulerLock lock,
    SingularityManagedThreadPoolFactory managedThreadPoolFactory,
    @Named(SingularityHistoryModule.PERSISTER_LOCK) ReentrantLock persisterLock,
    @Named(
      SingularityHistoryModule.LAST_REQUEST_PERSISTER_SUCCESS
    ) AtomicLong lastPersisterSuccess
  ) {
    super(configuration, persisterLock, lastPersisterSuccess, managedThreadPoolFactory);
    this.requestManager = requestManager;
    this.historyManager = historyManager;
    this.lock = lock;
  }

  public static class SingularityRequestHistoryParent
    implements SingularityHistoryItem, Comparable<SingularityRequestHistoryParent> {

    private final List<SingularityRequestHistory> history;
    private final String requestId;
    private final long createTime;

    public SingularityRequestHistoryParent(
      List<SingularityRequestHistory> history,
      String requestId
    ) {
      this.history = history;
      this.requestId = requestId;

      long newestTimestamp = 0;

      for (SingularityRequestHistory historyItem : history) {
        if (historyItem.getCreatedAt() > newestTimestamp) {
          newestTimestamp = historyItem.getCreatedAt();
        }
      }

      createTime = newestTimestamp;
    }

    public String getRequestId() {
      return requestId;
    }

    @Override
    public long getCreateTimestampForCalculatingHistoryAge() {
      return createTime;
    }

    @Override
    public int compareTo(SingularityRequestHistoryParent o) {
      return Longs.compare(
        this.getCreateTimestampForCalculatingHistoryAge(),
        o.getCreateTimestampForCalculatingHistoryAge()
      );
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SingularityRequestHistoryParent that = (SingularityRequestHistoryParent) o;
      return (
        createTime == that.createTime &&
        Objects.equals(history, that.history) &&
        Objects.equals(requestId, that.requestId)
      );
    }

    @Override
    public int hashCode() {
      return Objects.hash(history, requestId, createTime);
    }

    @Override
    public String toString() {
      return (
        "SingularityRequestHistoryParent[" +
        "history=" +
        history +
        ", requestId='" +
        requestId +
        '\'' +
        ", createTime=" +
        createTime +
        ']'
      );
    }
  }

  @Override
  public void runActionOnPoll() {
    LOG.info("Attempting to grab persister lock");
    persisterLock.lock();
    AtomicBoolean persisterSuccess = new AtomicBoolean(true);
    try {
      LOG.info("Checking request history for persistence");

      final long start = System.currentTimeMillis();

      final List<SingularityRequestHistoryParent> requestHistoryParents = new ArrayList();

      AtomicInteger numHistoryTransferred = new AtomicInteger();

      for (String requestId : requestManager.getRequestIdsWithHistory()) {
        requestHistoryParents.add(
          new SingularityRequestHistoryParent(
            requestManager.getRequestHistory(requestId),
            requestId
          )
        );
      }

      Collections.sort(requestHistoryParents, Collections.reverseOrder()); // createdAt descending

      AtomicInteger i = new AtomicInteger();
      List<CompletableFuture<Void>> futures = new ArrayList<>();
      for (SingularityRequestHistoryParent requestHistoryParent : requestHistoryParents) {
        futures.add(
          CompletableFuture.runAsync(
            () ->
              lock.runWithRequestLock(
                () -> {
                  if (
                    moveToHistoryOrCheckForPurge(
                      requestHistoryParent,
                      i.getAndIncrement()
                    )
                  ) {
                    numHistoryTransferred.getAndAdd(requestHistoryParent.history.size());
                  } else {
                    LOG.error(
                      "Request History Persister failed on {}",
                      requestHistoryParent
                    );
                    persisterSuccess.getAndSet(false);
                  }
                },
                requestHistoryParent.requestId,
                "request history purger",
                SingularitySchedulerLock.Priority.LOW
              ),
            persisterExecutor
          )
        );
      }

      CompletableFutures.allOf(futures).join();
      LOG.info(
        "Transferred {} history updates for {} requests in {}",
        numHistoryTransferred,
        requestHistoryParents.size(),
        JavaUtils.duration(start)
      );
    } finally {
      if (persisterSuccess.get()) {
        lastPersisterSuccess.set(System.currentTimeMillis());
        LOG.info(
          "Finished run on request history persister at {}",
          lastPersisterSuccess.get()
        );
      }

      persisterLock.unlock();
    }
  }

  @Override
  protected long getMaxAgeInMillisOfItem() {
    return TimeUnit.HOURS.toMillis(
      configuration.getDeleteStaleRequestsFromZkWhenNoDatabaseAfterHours()
    );
  }

  @Override
  protected Optional<Integer> getMaxNumberOfItems() {
    return configuration.getMaxRequestsWithHistoryInZkWhenNoDatabase();
  }

  @Override
  protected boolean moveToHistory(SingularityRequestHistoryParent object) {
    for (SingularityRequestHistory requestHistory : object.history) {
      try {
        historyManager.saveRequestHistoryUpdate(requestHistory);
      } catch (Throwable t) {
        LOG.error("Failed to persist {} into History", requestHistory, t);
        return false;
      }

      requestManager.deleteHistoryItem(requestHistory);
    }

    return true;
  }

  @Override
  protected SingularityDeleteResult purgeFromZk(SingularityRequestHistoryParent object) {
    return requestManager.deleteHistoryParent(object.requestId);
  }
}
