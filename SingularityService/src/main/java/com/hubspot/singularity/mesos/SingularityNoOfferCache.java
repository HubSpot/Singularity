package com.hubspot.singularity.mesos;

import java.util.Collections;
import java.util.List;

import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.SchedulerDriver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.mesos.SingularityOfferCache.CachedOffer;

@Singleton
public class SingularityNoOfferCache implements OfferCache {

  @Inject
  public SingularityNoOfferCache() {
  }

  @Override
  public void cacheOffer(SchedulerDriver driver, long timestamp, Offer offer) {
    driver.declineOffer(offer.getId());
  }

  @Override
  public void rescindOffer(SchedulerDriver driver, OfferID offerId) {
    // no-op
  }

  @Override
  public void useOffer(CachedOffer cachedOffer) {
    // no-op
  }

  @Override
  public List<CachedOffer> checkoutOffers() {
    return Collections.emptyList();
  }

  @Override
  public void returnOffer(CachedOffer cachedOffer) {
    // no-op
  }

  @Override
  public List<Offer> peekOffers() {
    return Collections.emptyList();
  }

  @Override
  public void disableOfferCache() {
    // no-op
  }

  @Override
  public void enableOfferCache() {
    // no-op
  }

}
