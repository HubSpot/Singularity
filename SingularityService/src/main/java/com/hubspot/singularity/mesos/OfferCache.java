package com.hubspot.singularity.mesos;

import java.util.List;

import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.SchedulerDriver;

import com.hubspot.singularity.mesos.SingularityOfferCache.CachedOffer;

public interface OfferCache {

  public void cacheOffer(SchedulerDriver driver, long timestamp, Offer offer);

  public void rescindOffer(SchedulerDriver driver, OfferID offerId);

  public void useOffer(CachedOffer cachedOffer);

  public List<CachedOffer> checkoutOffers();

  public void returnOffer(CachedOffer cachedOffer);

  public List<Offer> peekOffers();

  public void disableOfferCache();

  public void enableOfferCache();

}
