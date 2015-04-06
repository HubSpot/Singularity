package com.hubspot.singularity.data.history;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityHistoryItem;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.scheduler.SingularityLeaderOnlyPoller;

public abstract class SingularityHistoryPersister<T extends SingularityHistoryItem> extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityHistoryPersister.class);

  protected final SingularityConfiguration configuration;

  public SingularityHistoryPersister(SingularityConfiguration configuration) {
    super(configuration.getPersistHistoryEverySeconds(), TimeUnit.SECONDS);

    this.configuration = configuration;
  }

  @Override
  protected boolean abortsOnError() {
    return false;
  }

  protected boolean persistsHistoryInsteadOfPurging() {
    return configuration.getDatabaseConfiguration().isPresent();
  }

  @Override
  protected boolean isEnabled() {
    return persistsHistoryInsteadOfPurging() || getMaxAgeInMillisOfItem() > 0;
  }

  protected abstract long getMaxAgeInMillisOfItem();

  protected abstract boolean moveToHistory(T object);

  protected abstract SingularityDeleteResult purgeFromZk(T object);

  protected boolean moveToHistoryOrCheckForPurge(T object) {
    final long start = System.currentTimeMillis();

    if (moveToHistoryOrCheckForPurgeAndShouldDelete(object)) {
      SingularityDeleteResult deleteResult = purgeFromZk(object);
      LOG.debug("{} {} (deleted: {}) in {}", persistsHistoryInsteadOfPurging() ? "Persisted" : "Purged", object, deleteResult, JavaUtils.duration(start));
      return true;
    }

    return false;
  }

  private boolean moveToHistoryOrCheckForPurgeAndShouldDelete(T object) {
    if (persistsHistoryInsteadOfPurging()) {
      return moveToHistory(object);
    }

    final long age = System.currentTimeMillis() - object.getCreateTimestampForCalculatingHistoryAge();

    if (age > getMaxAgeInMillisOfItem()) {
      LOG.trace("Deleting {} because it is {} old (max : {})", object, JavaUtils.durationFromMillis(age), JavaUtils.durationFromMillis(getMaxAgeInMillisOfItem()));
      return true;
    }

    return false;
  }

}
