package com.hubspot.singularity.mesos;

import java.util.List;

import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;

import com.hubspot.singularity.mesos.SingularityOfferCache.CachedOffer;

public interface OfferCache {

  public void cacheOffer(long timestamp, Offer offer);

  public void rescindOffer(OfferID offerId);

  public void useOffer(CachedOffer cachedOffer);

  public List<CachedOffer> checkoutOffers();

  public void returnOffer(CachedOffer cachedOffer);

  public List<Offer> peekOffers();

  public void disableOfferCache();

  public void enableOfferCache();

}
