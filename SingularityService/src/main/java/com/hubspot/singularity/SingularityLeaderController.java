package com.hubspot.singularity;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Singleton;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.mesos.v1.Protos.MasterInfo;
import org.apache.mesos.v1.Protos.Offer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityAbort.AbortReason;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.StateManager;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.singularity.mesos.OfferCache;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityLeaderController implements LeaderLatchListener, ConnectionStateListener {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityLeaderController.class);
  private static final Timer TIMER = new Timer();

  private final StateManager stateManager;
  private final SingularityAbort abort;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final HostAndPort hostAndPort;
  private final long saveStateEveryMs;
  private final StatePoller statePoller;
  private final SingularityMesosScheduler scheduler;
  private final OfferCache offerCache;
  private final SingularityConfiguration configuration;
  private final ReentrantLock stateHandlerLock;

  private volatile TimerTask lostConnectionStateChecker;
  private volatile boolean master;

  @Inject
  public SingularityLeaderController(StateManager stateManager,
                                     SingularityConfiguration configuration,
                                     SingularityAbort abort,
                                     SingularityExceptionNotifier exceptionNotifier,
                                     @Named(SingularityMainModule.HTTP_HOST_AND_PORT) HostAndPort hostAndPort,
                                     SingularityMesosScheduler scheduler,
                                     OfferCache offerCache) {
    this.stateManager = stateManager;
    this.abort = abort;
    this.exceptionNotifier = exceptionNotifier;

    this.hostAndPort = hostAndPort;
    this.saveStateEveryMs = TimeUnit.SECONDS.toMillis(configuration.getSaveStateEverySeconds());
    this.statePoller = new StatePoller();
    this.scheduler = scheduler;
    this.configuration = configuration;
    this.offerCache = offerCache;

    this.master = false;
    this.stateHandlerLock = new ReentrantLock();
  }

  public void start() {
    statePoller.start();
  }

  public void stop() {
    scheduler.notifyStopping();
    statePoller.finish();
    TIMER.cancel();
  }

  @Override
  public void stateChanged(CuratorFramework client, ConnectionState newState) {
    LOG.info("Received update for new zk connection state {}", newState);
    stateHandlerLock.lock();
    try {
      // If the new state is not connected `setZkConnectionState` will effectively pause all pollers from
      // continuing to process events. An explicit call to pauseForDatastoreReconnect is not needed here
      scheduler.setZkConnectionState(newState);
      if (!newState.isConnected()) {
        LOG.info("No longer connected to zk, pausing scheduler actions and waiting up to {}ms for reconnect",
            configuration.getZooKeeperConfiguration().getAbortAfterConnectionLostForMillis());
        if (lostConnectionStateChecker != null) {
          LOG.warn("Already started connection state check, due in {}ms", lostConnectionStateChecker.scheduledExecutionTime() - System.currentTimeMillis());
          return;
        }
        lostConnectionStateChecker = new TimerTask() {
          @Override
          public void run() {
            stateHandlerLock.lock();
            try {
              if (scheduler.getState().getZkConnectionState().isConnected()) {
                LOG.debug("Reconnected to zk, will not abort");
                return;
              }
              LOG.error("Aborting due to loss of zookeeper connection for {}ms. Current connection state {}",
                  configuration.getZooKeeperConfiguration().getAbortAfterConnectionLostForMillis(), newState);
              abort.abort(AbortReason.LOST_ZK_CONNECTION, Optional.empty());
            } finally {
              stateHandlerLock.unlock();
            }
          }
        };
        TIMER.schedule(lostConnectionStateChecker, configuration.getZooKeeperConfiguration().getAbortAfterConnectionLostForMillis());
      } else if (lostConnectionStateChecker != null) {
        LOG.info("Reconnected to zk, scheduler actions resumed");
        lostConnectionStateChecker.cancel();
        lostConnectionStateChecker = null;
      }
    } finally {
      stateHandlerLock.unlock();
    }
  }

  protected boolean isTestMode() {
    return false;
  }

  @Override
  public void isLeader() {
    stateHandlerLock.lock();
    try {
      LOG.info("We are now the leader! Current state {}", scheduler.getState());

      master = true;
      try {
        if (!isTestMode()) {
          statePoller.wake();
          scheduler.start();
        }
      } catch (Throwable t) {
        LOG.error("While starting driver", t);
        exceptionNotifier.notify(String.format("Error starting driver (%s)", t.getMessage()), t);
        try {
          scheduler.notifyStopping();
        } catch (Throwable th) {
          LOG.warn("While stopping scheduler due to bad initial start({})", th.getMessage());
        }
        abort.abort(AbortReason.UNRECOVERABLE_ERROR, Optional.of(t));
      }
    } finally {
      stateHandlerLock.unlock();
    }
  }

  public boolean isMaster() {
    return master;
  }

  public Optional<MasterInfo> getMaster() {
    return scheduler.getMaster();
  }

  public Optional<Long> getLastOfferTimestamp() {
    return scheduler.getLastOfferTimestamp();
  }

  @Override
  public void notLeader() {
    stateHandlerLock.lock();
    try {
      LOG.info("We are not the leader! Current state {}", scheduler.getState());
      master = false;
      if (!isTestMode()) {
        LOG.info("Might not be the leader, pausing scheduler actions");
        scheduler.pauseForDatastoreReconnect();
        // Check again if we are still not leader in a few seconds. LeaderLatch.reset can often get called on reconnect, which
        // will call notLeader/isLeader is quick succession.
        TIMER.schedule(new TimerTask() {
          @Override
          public void run() {
            stateHandlerLock.lock();
            try {
              if (master) {
                LOG.debug("Reconnected as leader before shutdown timeout");
              } else {
                LOG.info("No longer the leader, stopping scheduler actions");
                scheduler.notLeader();
              }
            } finally {
              stateHandlerLock.unlock();
            }
          }
        }, 5000);
      }
    } finally {
      stateHandlerLock.unlock();
    }
  }

  private SingularityHostState getHostState() {
    final boolean master = isMaster();

    final RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
    final long uptime = mxBean.getUptime();

    final long now = System.currentTimeMillis();
    final Optional<Long> lastOfferTimestamp = getLastOfferTimestamp();
    final Optional<Long> millisSinceLastOfferTimestamp = lastOfferTimestamp.isPresent() ? Optional.of(now - lastOfferTimestamp.get()) : Optional.<Long>empty();

    String mesosMaster = null;
    Optional<MasterInfo> mesosMasterInfo = getMaster();

    if (mesosMasterInfo.isPresent()) {
      mesosMaster = MesosUtils.getMasterHostAndPort(mesosMasterInfo.get());
    }

    double cachedCpus = 0;
    double cachedMemoryBytes = 0;
    int numCachedOffers = 0;

    for (Offer offer : offerCache.peekOffers()) {
      cachedCpus += MesosUtils.getNumCpus(offer);
      cachedMemoryBytes += MesosUtils.getMemory(offer);
      numCachedOffers++;
    }

    return new SingularityHostState(master, uptime, scheduler.getState().getMesosSchedulerState().name(), millisSinceLastOfferTimestamp, hostAndPort.getHost(), hostAndPort.getHost(), mesosMaster, scheduler.isRunning(),
       numCachedOffers, cachedCpus, cachedMemoryBytes);
  }

  // This thread lives inside of this class solely so that we can instantly update the state when the leader latch changes.
  public class StatePoller extends Thread {
    private final AtomicBoolean finished = new AtomicBoolean();
    private Lock lock = new ReentrantLock();
    private volatile boolean interrupted = false;

    StatePoller() {
      super("SingularityStatePoller");
      setDaemon(true);
    }

    public void finish() {
      finished.set(true);
      this.interrupt();
    }

    public void wake() {
      this.interrupted = true;
      if (lock.tryLock()) {
        try {
          this.interrupt();
        }
        finally {
          lock.unlock();
        }
      }
      else {
        LOG.trace("state poller in zk code");
      }
    }

    @Override
    public void run() {
      while (!finished.get()) {

        try {
          lock.lock();
          // save current interrupt state and clear it.
          interrupted |= Thread.interrupted();
          final SingularityHostState state = getHostState();
          LOG.trace("Saving state in ZK: " + state);
          stateManager.save(state);
          if (master) {
            stateManager.saveNewState();
          }
        } catch (InterruptedException e) {
          LOG.trace("Caught interrupted exception, running the loop");
        } catch (Throwable t) {
          // Can get wrapped in a runtime exception, check cause as well
          if (t.getCause() != null && t.getCause() instanceof InterruptedException) {
            LOG.trace("Caught interrupted exception, running the loop");
          } else {
            LOG.error("Caught exception while saving state", t);
            exceptionNotifier.notify(String.format("Caught exception while saving state (%s)", t.getMessage()), t);
          }
        }
        finally {
          lock.unlock();
          if (interrupted) {
            Thread.currentThread().interrupt();
          }
        }

        try {
          Thread.sleep(saveStateEveryMs);
        } catch (InterruptedException e) {
          interrupted = false;
          LOG.trace("Interrupted, running the loop");
        }
      }
    }
  }
}
