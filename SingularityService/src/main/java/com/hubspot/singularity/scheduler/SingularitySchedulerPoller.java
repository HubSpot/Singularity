package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.inject.Singleton;

import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.OfferCache;
import com.hubspot.singularity.mesos.SchedulerDriverSupplier;
import com.hubspot.singularity.mesos.SingularityMesosModule;
import com.hubspot.singularity.mesos.SingularityMesosOfferScheduler;
import com.hubspot.singularity.mesos.SingularityOfferCache.CachedOffer;
import com.hubspot.singularity.mesos.SingularityOfferHolder;

@Singleton
public class SingularitySchedulerPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularitySchedulerPoller.class);

  private final OfferCache offerCache;
  private final SchedulerDriverSupplier schedulerDriverSupplier;
  private final SingularityMesosOfferScheduler offerScheduler;

  @Inject
  SingularitySchedulerPoller(SingularityMesosOfferScheduler offerScheduler, OfferCache offerCache, SchedulerDriverSupplier schedulerDriverSupplier,
      SingularityConfiguration configuration, @Named(SingularityMesosModule.SCHEDULER_LOCK_NAME) final Lock lock) {
    super(configuration.getCheckSchedulerEverySeconds(), TimeUnit.SECONDS, lock);

    this.offerCache = offerCache;
    this.offerScheduler = offerScheduler;
    this.schedulerDriverSupplier = schedulerDriverSupplier;
  }

  @Override
  public void runActionOnPoll() {
    final long start = System.currentTimeMillis();

    List<CachedOffer> cachedOffers = offerCache.checkoutOffers();
    Map<String, CachedOffer> offerIdToCachedOffer = new HashMap<>(cachedOffers.size());
    List<Offer> offers = new ArrayList<>(cachedOffers.size());

    for (CachedOffer cachedOffer : cachedOffers) {
      offerIdToCachedOffer.put(cachedOffer.getOfferId(), cachedOffer);
      offers.add(cachedOffer.getOffer());
    }

    List<SingularityOfferHolder> offerHolders = offerScheduler.checkOffers(offers, Sets.<OfferID> newHashSet());

    if (offerHolders.isEmpty()) {
      return;
    }

    Optional<SchedulerDriver> driver = schedulerDriverSupplier.get();

    if (!driver.isPresent()) {
      LOG.error("No driver present, can't accept cached offers");
      return;
    }

    int acceptedOffers = 0;
    int launchedTasks = 0;

    for (SingularityOfferHolder offerHolder : offerHolders) {
      CachedOffer cachedOffer = offerIdToCachedOffer.get(offerHolder.getOffer().getId().getValue());

      if (!offerHolder.getAcceptedTasks().isEmpty()) {
        offerHolder.launchTasks(driver.get());
        launchedTasks += offerHolder.getAcceptedTasks().size();
        acceptedOffers++;
        offerCache.useOffer(cachedOffer);
      } else {
        offerCache.returnOffer(cachedOffer);
      }
    }

    LOG.info("Launched {} tasks on {} cached offers (returned {}) in {}", launchedTasks, acceptedOffers, offerHolders.size() - acceptedOffers, JavaUtils.duration(start));
  }

}
