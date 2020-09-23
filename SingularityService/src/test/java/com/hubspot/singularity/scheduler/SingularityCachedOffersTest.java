package com.hubspot.singularity.scheduler;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.AgentPlacement;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.mesos.OfferCache;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.mesos.v1.Protos.Offer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SingularityCachedOffersTest extends SingularitySchedulerTestBase {
  @Inject
  private SingularitySchedulerPoller schedulerPoller;

  @Inject
  private OfferCache offerCache;

  public SingularityCachedOffersTest() {
    super(
      false,
      configuration -> {
        configuration.setCacheOffers(true);
        return null;
      }
    );
  }

  @Test
  public void testOfferCache() {
    configuration.setCacheOffers(true);
    configuration.setOfferCacheSize(2);
    List<Offer> offers2 = resourceOffers();

    sms.rescind(offers2.get(0).getId());

    initRequest();
    initFirstDeploy();

    requestResource.postRequest(
      request
        .toBuilder()
        .setAgentPlacement(Optional.of(AgentPlacement.SEPARATE))
        .setInstances(Optional.of(2))
        .build(),
      singularityUser
    );

    schedulerPoller.runActionOnPoll();

    Assertions.assertEquals(1, taskManager.getActiveTasks().size());

    resourceOffers();

    Assertions.assertEquals(2, taskManager.getActiveTasks().size());
  }

  @Test
  public void testLeftoverNewOffersAreCached() {
    configuration.setCacheOffers(true);

    Offer neededOffer = createOffer(1, 128, 1024, "agent1", "host1");
    Offer extraOffer = createOffer(4, 256, 0, "agent1", "host1");

    initRequest();
    initFirstDeploy();

    requestManager.addToPendingQueue(
      new SingularityPendingRequest(
        requestId,
        firstDeployId,
        System.currentTimeMillis(),
        Optional.empty(),
        PendingType.TASK_DONE,
        Optional.empty(),
        Optional.empty()
      )
    );

    scheduler.drainPendingQueue();
    sms.resourceOffers(ImmutableList.of(neededOffer, extraOffer)).join();

    List<Offer> cachedOffers = offerCache.peekOffers();
    Assertions.assertEquals(1, cachedOffers.size());
  }

  @Test
  public void testLeftoverCachedOffersAreReturnedToCache() throws Exception {
    configuration.setCacheOffers(true);

    Offer neededOffer = createOffer(
      1,
      128,
      1024,
      "agent1",
      "host1",
      Optional.empty(),
      Collections.emptyMap(),
      new String[] { "80:81" }
    );
    Offer extraOffer = createOffer(
      4,
      256,
      1024,
      "agent1",
      "host1",
      Optional.empty(),
      Collections.emptyMap(),
      new String[] { "83:84" }
    );

    sms.resourceOffers(ImmutableList.of(neededOffer, extraOffer)).join();

    initRequest();

    firstDeploy =
      initAndFinishDeploy(
        request,
        new SingularityDeployBuilder(request.getId(), firstDeployId)
        .setCommand(Optional.of("sleep 100")),
        Optional.of(new Resources(1, 128, 2, 0))
      );

    requestManager.addToPendingQueue(
      new SingularityPendingRequest(
        requestId,
        firstDeployId,
        System.currentTimeMillis(),
        Optional.empty(),
        PendingType.TASK_DONE,
        Optional.empty(),
        Optional.empty()
      )
    );

    schedulerPoller.runActionOnPoll();

    List<Offer> cachedOffers = offerCache.peekOffers();
    Assertions.assertEquals(1, cachedOffers.size());
  }

  @Test
  public void testOfferCombination() {
    configuration.setOfferCacheSize(2);

    // Each are half of needed memory
    Offer offer1 = createOffer(1, 64, 1024, "agent1", "host1");
    Offer offer2 = createOffer(1, 64, 1024, "agent1", "host1");
    sms.resourceOffers(ImmutableList.of(offer1, offer2)).join();

    initRequest();
    initFirstDeploy();
    requestManager.addToPendingQueue(
      new SingularityPendingRequest(
        requestId,
        firstDeployId,
        System.currentTimeMillis(),
        Optional.empty(),
        PendingType.TASK_DONE,
        Optional.empty(),
        Optional.empty()
      )
    );

    schedulerPoller.runActionOnPoll();

    Assertions.assertEquals(1, taskManager.getActiveTasks().size());

    Assertions.assertEquals(2, taskManager.getActiveTasks().get(0).getOffers().size());
  }
}
