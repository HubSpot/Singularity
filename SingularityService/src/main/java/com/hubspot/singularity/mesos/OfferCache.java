package com.hubspot.singularity.mesos;

import java.util.List;

import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.SchedulerDriver;

public interface OfferCache {

  public void cacheOffer(SchedulerDriver driver, long timestamp, Offer offer);

  public void rescindOffer(SchedulerDriver driver, OfferID offerId);

  public void useOffer(OfferID offerId);

  public List<Offer> checkoutOffers();

  public void returnOffer(OfferID offerId);

}
