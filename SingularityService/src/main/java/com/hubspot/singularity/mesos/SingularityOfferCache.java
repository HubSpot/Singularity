package com.hubspot.singularity.mesos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.Protos.Status;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityOfferCache implements OfferCache, RemovalListener<String, Offer> {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityOfferCache.class);

  private final Cache<String, Offer> offerCache;
  private final SchedulerDriverSupplier schedulerDriverSupplier;
  private final SingularityConfiguration configuration;

  @Inject
  public SingularityOfferCache(SingularityConfiguration configuration, SchedulerDriverSupplier schedulerDriverSupplier) {
    this.configuration = configuration;
    this.schedulerDriverSupplier = schedulerDriverSupplier;

    offerCache = CacheBuilder.newBuilder()
        .expireAfterWrite(configuration.getCacheOffersForMillis(), TimeUnit.MILLISECONDS)
        .maximumSize(configuration.getOfferCacheSize())
        .removalListener(this)
        .build();
  }

  @Override
  public void cacheOffer(SchedulerDriver driver, long timestamp, Offer offer) {
    LOG.debug("Caching offer {} for {}", offer.getId().getValue(), JavaUtils.durationFromMillis(configuration.getCacheOffersForMillis()));

    offerCache.put(offer.getId().getValue(), offer);
  }

  @Override
  public void onRemoval(RemovalNotification<String, Offer> notification) {
    if (notification.getCause() == RemovalCause.EXPLICIT) {
      return;
    }

    Optional<SchedulerDriver> driver = schedulerDriverSupplier.get();

    if (!driver.isPresent()) {
      LOG.error("No scheduler driver present to handle expired offer {} - this should never happen", notification.getKey());
      return;
    }

    Status status = driver.get().declineOffer(notification.getValue().getId());
    LOG.debug("Declined removed offer {} from cache (reason {}) - driver status {}", notification.getKey(), notification.getCause(), status);
  }

  @Override
  public void rescindOffer(SchedulerDriver driver, OfferID offerId) {
    offerCache.invalidate(offerId.getValue());
  }

  @Override
  public void useOffer(OfferID offerId) {
    offerCache.invalidate(offerId.getValue());
  }

  @Override
  public List<Offer> getCachedOffers() {
    return new ArrayList<>(offerCache.asMap().values());
  }

}
