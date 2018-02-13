package com.hubspot.singularity.scheduler;

import java.util.Collections;
import java.util.List;

import org.apache.mesos.v1.Protos.Offer;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.mesos.OfferCache;

public class SingularityCachedOffersTest extends SingularitySchedulerTestBase {

  @Inject
  private SingularitySchedulerPoller schedulerPoller;

  @Inject
  private OfferCache offerCache;

  public SingularityCachedOffersTest() {
    super(false, (configuration) -> {
      configuration.setCacheOffers(true);
      return null;
    });
  }

  @Test
  public void testOfferCache() {
    configuration.setCacheOffers(true);
    configuration.setOfferCacheSize(2);
    List<Offer> offers2 = resourceOffers();

    sms.rescind(offers2.get(0).getId());

    initRequest();
    initFirstDeploy();

    requestResource.postRequest(request.toBuilder().setSlavePlacement(Optional.of(SlavePlacement.SEPARATE)).setInstances(Optional.of(2)).build(), singularityUser);

    schedulerPoller.runActionOnPoll();

    Assert.assertEquals(1, taskManager.getActiveTasks().size());

    resourceOffers();

    Assert.assertEquals(2, taskManager.getActiveTasks().size());
  }

  @Test
  public void testLeftoverNewOffersAreCached() {
    configuration.setCacheOffers(true);

    Offer neededOffer = createOffer(1, 128, 1024, "slave1", "host1");
    Offer extraOffer = createOffer(4, 256, 0, "slave1", "host1");

    initRequest();
    initFirstDeploy();

    requestManager.addToPendingQueue(
        new SingularityPendingRequest(
            requestId,
            firstDeployId,
            System.currentTimeMillis(),
            Optional.absent(),
            PendingType.TASK_DONE,
            Optional.absent(),
            Optional.absent()
        )
    );

    sms.resourceOffers(ImmutableList.of(neededOffer, extraOffer));

    List<Offer> cachedOffers = offerCache.peekOffers();
    Assert.assertEquals(1, cachedOffers.size());
  }

  @Test
  public void testLeftoverCachedOffersAreReturnedToCache() throws Exception {
    configuration.setCacheOffers(true);

    Offer neededOffer = createOffer(1, 128, 1024, "slave1", "host1", Optional.absent(), Collections.emptyMap(), new String[]{"80:81"});
    Offer extraOffer = createOffer(4, 256, 1024, "slave1", "host1", Optional.absent(), Collections.emptyMap(), new String[]{"83:84"});

    sms.resourceOffers(ImmutableList.of(neededOffer, extraOffer));

    initRequest();

    firstDeploy = initAndFinishDeploy(request, new SingularityDeployBuilder(request.getId(), firstDeployId)
        .setCommand(Optional.of("sleep 100")), Optional.of(new Resources(1, 128, 2, 0))
    );

    requestManager.addToPendingQueue(
        new SingularityPendingRequest(
            requestId,
            firstDeployId,
            System.currentTimeMillis(),
            Optional.absent(),
            PendingType.TASK_DONE,
            Optional.absent(),
            Optional.absent()
        )
    );

    schedulerPoller.runActionOnPoll();

    List<Offer> cachedOffers = offerCache.peekOffers();
    Assert.assertEquals(1, cachedOffers.size());
  }

  @Test
  public void testOfferCombination() {
    configuration.setOfferCacheSize(2);

    // Each are half of needed memory
    Offer offer1 = createOffer(1, 64, 1024, "slave1", "host1");
    Offer offer2 = createOffer(1, 64, 1024, "slave1", "host1");
    sms.resourceOffers(ImmutableList.of(offer1, offer2));

    initRequest();
    initFirstDeploy();
    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, firstDeployId, System.currentTimeMillis(), Optional.absent(), PendingType.TASK_DONE,
        Optional.absent(), Optional.absent()));

    schedulerPoller.runActionOnPoll();

    Assert.assertEquals(1, taskManager.getActiveTasks().size());

    Assert.assertEquals(2, taskManager.getActiveTasks().get(0).getOffers().size());
  }
}
