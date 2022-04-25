package com.hubspot.singularity.mesos;

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
import com.hubspot.singularity.mesos.SingularityMesosOfferManager.CachedOffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityMesosOfferManager
  implements RemovalListener<OfferID, CachedOffer> {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityMesosOfferManager.class
  );

  private final Cache<OfferID, CachedOffer> offerCache;
  private final SingularityMesosSchedulerClient schedulerClient;
  private final SingularityConfiguration configuration;
  private final SingularityAgentAndRackHelper agentAndRackHelper;
  private final AtomicBoolean useOfferCache = new AtomicBoolean(true);

  @Inject
  public SingularityMesosOfferManager(
    SingularityConfiguration configuration,
    SingularityMesosSchedulerClient schedulerClient,
    SingularityAgentAndRackHelper agentAndRackHelper
  ) {
    this.configuration = configuration;
    this.schedulerClient = schedulerClient;
    this.agentAndRackHelper = agentAndRackHelper;

    offerCache =
      CacheBuilder
        .newBuilder()
        .expireAfterWrite(
          configuration.getMesosConfiguration().getOfferTimeout(),
          TimeUnit.MILLISECONDS
        )
        .maximumSize(configuration.getOfferCacheSize())
        .removalListener(this)
        .build();
  }

  public void cacheOffer(long timestamp, Offer offer) {
    if (!useOfferCache.get()) {
      schedulerClient.decline(Collections.singletonList(offer.getId()));
      return;
    }
    LOG.debug(
      "Caching offer {} for {}",
      offer.getId().getValue(),
      JavaUtils.durationFromMillis(configuration.getCacheOffersForMillis())
    );

    offerCache.put(offer.getId(), new CachedOffer(offer));
  }

  public void onRemoval(RemovalNotification<OfferID, CachedOffer> notification) {
    if (notification.getCause() == RemovalCause.EXPLICIT) {
      return;
    }

    LOG.debug(
      "Cache removal for {} due to {}",
      notification.getKey(),
      notification.getCause()
    );

    synchronized (offerCache) {
      if (notification.getValue().offerState == OfferState.AVAILABLE) {
        declineOffer(notification.getKey());
      } else {
        notification.getValue().expire();
      }
    }
  }

  public void rescindOffer(OfferID offerId) {
    CachedOffer maybeCached = offerCache.getIfPresent(offerId);
    if (maybeCached != null) {
      LOG.info(
        "Offer {} on {} rescinded",
        offerId.getValue(),
        maybeCached.getOffer().getHostname()
      );
    } else {
      LOG.info("Offer {} rescinded (not in cache)", offerId.getValue());
    }
    offerCache.invalidate(offerId);
  }

  public void invalidateAll() {
    offerCache.invalidateAll();
  }

  public void useOffer(OfferID offerId) {
    offerCache.invalidate(offerId);
  }

  public List<SingularityOfferHolder> checkoutOffers() {
    // Force Guava cache to perform maintenance operations and reach a consistent state.
    offerCache.cleanUp();

    List<CachedOffer> offers = new ArrayList<>((int) offerCache.size());
    for (CachedOffer cachedOffer : offerCache.asMap().values()) {
      cachedOffer.checkOut();
      offers.add(cachedOffer);
    }
    return offers
      .stream()
      .map(c -> c.offer)
      .collect(Collectors.groupingBy(o -> o.getAgentId().getValue()))
      .entrySet()
      .stream()
      .filter(e -> e.getValue().size() > 0)
      .map(
        e -> {
          List<Offer> offersList = e.getValue();
          String agentId = e.getKey();
          return new SingularityOfferHolder(
            offersList,
            agentAndRackHelper.getRackIdOrDefault(offersList.get(0)),
            agentId,
            offersList.get(0).getHostname(),
            agentAndRackHelper.getTextAttributes(offersList.get(0)),
            agentAndRackHelper.getReservedAgentAttributes(offersList.get(0))
          );
        }
      )
      .collect(Collectors.toList());
  }

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

  public void returnOffer(OfferID offerId) {
    synchronized (offerCache) {
      CachedOffer cachedOffer = offerCache.getIfPresent(offerId);
      if (cachedOffer == null) {
        LOG.error("No cached offer {} in memory", offerId.getValue());
        return;
      }
      if (cachedOffer.offerState == OfferState.EXPIRED) {
        declineOffer(offerId);
      } else {
        cachedOffer.checkIn();
      }
    }
  }

  private void declineOffer(OfferID offerId) {
    if (!schedulerClient.isRunning()) {
      LOG.error(
        "No active scheduler driver present to handle expired offer {} - this should never happen",
        offerId.getValue()
      );
      return;
    }

    schedulerClient.decline(Collections.singletonList(offerId));

    LOG.debug("Declined cached offer {}", offerId.getValue());
  }

  private enum OfferState {
    AVAILABLE,
    CHECKED_OUT,
    EXPIRED
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
      Preconditions.checkState(
        offerState == OfferState.AVAILABLE,
        "Offer %s was in state %s",
        offerId,
        offerState
      );
      this.offerState = OfferState.CHECKED_OUT;
    }

    private void checkIn() {
      Preconditions.checkState(
        offerState == OfferState.CHECKED_OUT,
        "Offer %s was in state %s",
        offerId,
        offerState
      );
      this.offerState = OfferState.AVAILABLE;
    }

    private void expire() {
      Preconditions.checkState(
        offerState == OfferState.CHECKED_OUT,
        "Offer %s was in state %s",
        offerId,
        offerState
      );
      this.offerState = OfferState.EXPIRED;
    }
  }
}
