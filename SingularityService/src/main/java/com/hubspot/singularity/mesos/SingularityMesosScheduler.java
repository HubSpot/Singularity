package com.hubspot.singularity.mesos;

import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;

@Singleton
public class SingularityMesosScheduler implements Scheduler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosScheduler.class);

  private final SingularityMesosFrameworkMessageHandler messageHandler;
  private final SingularitySlaveAndRackManager slaveAndRackManager;
  private final DisasterManager disasterManager;
  private final SchedulerDriverSupplier schedulerDriverSupplier;
  private final OfferCache offerCache;
  private final SingularityMesosOfferScheduler offerScheduler;
  private final SingularityMesosStatusUpdateHandler statusUpdateHandler;
  private final boolean offerCacheEnabled;

  @Inject
  public SingularityMesosScheduler(SingularityMesosFrameworkMessageHandler messageHandler, SingularitySlaveAndRackManager slaveAndRackManager, SchedulerDriverSupplier schedulerDriverSupplier,
      OfferCache offerCache, SingularityMesosOfferScheduler offerScheduler, SingularityMesosStatusUpdateHandler statusUpdateHandler, DisasterManager disasterManager, SingularityConfiguration configuration) {
    this.messageHandler = messageHandler;
    this.slaveAndRackManager = slaveAndRackManager;
    this.schedulerDriverSupplier = schedulerDriverSupplier;
    this.disasterManager = disasterManager;
    this.offerCache = offerCache;
    this.offerScheduler = offerScheduler;
    this.statusUpdateHandler = statusUpdateHandler;
    this.offerCacheEnabled = configuration.isCacheOffers();
  }

  @Override
  public void registered(SchedulerDriver driver, Protos.FrameworkID frameworkId, Protos.MasterInfo masterInfo) {
    LOG.info("Registered driver {}, with frameworkId {} and master {}", driver, frameworkId, masterInfo);
    schedulerDriverSupplier.setSchedulerDriver(driver);
  }

  @Override
  public void reregistered(SchedulerDriver driver, Protos.MasterInfo masterInfo) {
    LOG.info("Reregistered driver {}, with master {}", driver, masterInfo);
    schedulerDriverSupplier.setSchedulerDriver(driver);
  }

  @Override
  @Timed
  public void resourceOffers(SchedulerDriver driver, List<Protos.Offer> offers) {
    LOG.info("Received {} offer(s)", offers.size());
    if (disasterManager.isDisabled(SingularityAction.PROCESS_OFFERS)) {
      LOG.info("Processing offers is currently disabled, declining {} offers", offers.size());
      for (Protos.Offer offer : offers) {
        driver.declineOffer(offer.getId());
      }
      return;
    }
    if (offerCacheEnabled) {
      if (disasterManager.isDisabled(SingularityAction.CACHE_OFFERS)) {
        offerCache.disableOfferCache();
      } else {
        offerCache.enableOfferCache();
      }
    }

    for (Offer offer : offers) {
      String rolesInfo = MesosUtils.getRoles(offer).toString();
      LOG.debug("Received offer ID {} with roles {} from {} ({}) for {} cpu(s), {} memory, {} ports, and {} disk", offer.getId().getValue(), rolesInfo, offer.getHostname(), offer.getSlaveId().getValue(), MesosUtils.getNumCpus(offer), MesosUtils.getMemory(offer),
          MesosUtils.getNumPorts(offer), MesosUtils.getDisk(offer));
    }

    final long start = System.currentTimeMillis();

    final Set<Protos.OfferID> acceptedOffers = Sets.newHashSetWithExpectedSize(offers.size());

    try {
      List<SingularityOfferHolder> offerHolders = offerScheduler.checkOffers(offers, acceptedOffers);

      for (SingularityOfferHolder offerHolder : offerHolders) {
        if (!offerHolder.getAcceptedTasks().isEmpty()) {
          offerHolder.launchTasks(driver);

          acceptedOffers.add(offerHolder.getOffer().getId());
        } else {
          offerCache.cacheOffer(driver, start, offerHolder.getOffer());
        }
      }
    } catch (Throwable t) {
      LOG.error("Received fatal error while handling offers - will decline all available offers", t);

      for (Protos.Offer offer : offers) {
        if (acceptedOffers.contains(offer.getId())) {
          continue;
        }

        driver.declineOffer(offer.getId());
      }

      throw t;
    }

    LOG.info("Finished handling {} new offer(s) ({}), {} accepted, {} declined/cached", offers.size(), JavaUtils.duration(start), acceptedOffers.size(),
        offers.size() - acceptedOffers.size());
  }

  @Override
  public void offerRescinded(SchedulerDriver driver, Protos.OfferID offerId) {
    LOG.info("Offer {} rescinded", offerId);

    offerCache.rescindOffer(driver, offerId);
  }

  @Override
  public void statusUpdate(SchedulerDriver driver, Protos.TaskStatus status) {
    statusUpdateHandler.processStatusUpdate(status);
  }

  @Override
  public void frameworkMessage(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, byte[] data) {
    LOG.info("Framework message from executor {} on slave {} with {} bytes of data", executorId, slaveId, data.length);

    messageHandler.handleMessage(executorId, slaveId, data);
  }

  @Override
  public void disconnected(SchedulerDriver driver) {
    schedulerDriverSupplier.setSchedulerDriver(null);
    LOG.warn("Scheduler/Driver disconnected");
  }

  @Override
  public void slaveLost(SchedulerDriver driver, Protos.SlaveID slaveId) {
    LOG.warn("Lost a slave {}", slaveId);

    slaveAndRackManager.slaveLost(slaveId);
  }

  @Override
  public void executorLost(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, int status) {
    LOG.warn("Lost an executor {} on slave {} with status {}", executorId, slaveId, status);
  }

  @Override
  public void error(SchedulerDriver driver, String message) {
    LOG.warn("Error from mesos: {}", message);
  }

  public boolean isConnected() {
    return schedulerDriverSupplier.get().isPresent();
  }

}
