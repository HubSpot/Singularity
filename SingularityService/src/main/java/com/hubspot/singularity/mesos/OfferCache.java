package com.hubspot.singularity.mesos;

import com.hubspot.singularity.mesos.SingularityOfferCache.CachedOffer;
import java.util.List;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;

public interface OfferCache {
  void cacheOffer(long timestamp, Offer offer);

  void rescindOffer(OfferID offerId);

  void useOffer(CachedOffer cachedOffer);

  List<CachedOffer> checkoutOffers();

  void returnOffer(CachedOffer cachedOffer);

  List<Offer> peekOffers();

  void disableOfferCache();

  void enableOfferCache();

  void invalidateAll();
}
