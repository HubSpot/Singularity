package com.hubspot.singularity.mesos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityOfferCache.CachedOffer;

@Singleton
public class SingularityOfferCache implements OfferCache, RemovalListener<String, CachedOffer> {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityOfferCache.class);

  private final Cache<String, CachedOffer> offerCache;
  private final SingularityMesosSchedulerClient schedulerClient;
  private final SingularityConfiguration configuration;
  private final AtomicBoolean useOfferCache = new AtomicBoolean(true);

  @Inject
  public SingularityOfferCache(SingularityConfiguration configuration, SingularityMesosSchedulerClient schedulerClient) {
    this.configuration = configuration;
    this.schedulerClient = schedulerClient;

    offerCache = CacheBuilder.newBuilder()
        .expireAfterWrite(configuration.getCacheOffersForMillis(), TimeUnit.MILLISECONDS)
        .maximumSize(configuration.getOfferCacheSize())
        .removalListener(this)
        .build();
  }

  @Override
  public void cacheOffer(long timestamp, Offer offer) {
    if (!useOfferCache.get()) {
      schedulerClient.decline(Collections.singletonList(offer.getId()));
      return;
    }
    LOG.debug("Caching offer {} for {}", offer.getId().getValue(), JavaUtils.durationFromMillis(configuration.getCacheOffersForMillis()));

    offerCache.put(offer.getId().getValue(), new CachedOffer(offer));
  }

  @Override
  public void onRemoval(RemovalNotification<String, CachedOffer> notification) {
    if (notification.getCause() == RemovalCause.EXPLICIT) {
      return;
    }

    LOG.debug("Cache removal for {} due to {}", notification.getKey(), notification.getCause());

    synchronized (offerCache) {
      if (notification.getValue().offerState == OfferState.AVAILABLE) {
        declineOffer(notification.getValue());
      } else {
        notification.getValue().expire();
      }
    }
  }

  @Override
  public void rescindOffer(OfferID offerId) {
    offerCache.invalidate(offerId.getValue());
  }

  @Override
  public void useOffer(CachedOffer cachedOffer) {
    offerCache.invalidate(cachedOffer.offerId);
  }

  @Override
  public List<CachedOffer> checkoutOffers() {
    if (!useOfferCache.get()) {
      return Collections.emptyList();
    }

    // Force Guava cache to perform maintenance operations and reach a consistent state.
    offerCache.cleanUp();

    List<CachedOffer> offers = new ArrayList<>((int) offerCache.size());
    for (CachedOffer cachedOffer : offerCache.asMap().values()) {
      cachedOffer.checkOut();
      offers.add(cachedOffer);
    }
    return offers;
  }

  @Override
  public List<Offer> peekOffers() {
    if (!useOfferCache.get()) {
      return Collections.emptyList();
    }
    List<Offer> offers = new ArrayList<>((int) offerCache.size());
    for (CachedOffer cachedOffer : offerCache.asMap().values()) {
      offers.add(cachedOffer.offer);
    }
    return offers;
  }

  @Override
  public void returnOffer(CachedOffer cachedOffer) {
    synchronized (offerCache) {
      if (cachedOffer.offerState == OfferState.EXPIRED) {
        declineOffer(cachedOffer);
      } else {
        cachedOffer.checkIn();
      }
    }
  }

  @Override
  public void disableOfferCache() {
    useOfferCache.set(false);
  }

  @Override
  public void enableOfferCache() {
    useOfferCache.set(true);
  }

  private void declineOffer(CachedOffer offer) {
    if (!schedulerClient.isRunning()) {
      LOG.error("No active scheduler driver present to handle expired offer {} - this should never happen", offer.offerId);
      return;
    }

    schedulerClient.decline(Collections.singletonList(offer.offer.getId()));

    LOG.debug("Declined cached offer {}", offer.offerId);
  }

  private enum OfferState {
    AVAILABLE, CHECKED_OUT, EXPIRED;
  }

  public static class CachedOffer {

    private final String offerId;
    private final Offer offer;
    private OfferState offerState;

    public CachedOffer(Offer offer) {
      this.offerId = offer.getId().getValue();
      this.offer = offer;
      this.offerState = OfferState.AVAILABLE;
    }

    public Offer getOffer() {
      return offer;
    }

    public String getOfferId() {
      return offerId;
    }

    private void checkOut() {
      Preconditions.checkState(offerState == OfferState.AVAILABLE, "Offer %s was in state %s", offerId, offerState);
      this.offerState = OfferState.CHECKED_OUT;
    }

    private void checkIn() {
      Preconditions.checkState(offerState == OfferState.CHECKED_OUT, "Offer %s was in state %s", offerId, offerState);
      this.offerState = OfferState.AVAILABLE;
    }

    private void expire() {
      Preconditions.checkState(offerState == OfferState.CHECKED_OUT, "Offer %s was in state %s", offerId, offerState);
      this.offerState = OfferState.EXPIRED;
    }

  }
}
