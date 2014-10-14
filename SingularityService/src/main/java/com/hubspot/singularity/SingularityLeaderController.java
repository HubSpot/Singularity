package com.hubspot.singularity;

import io.dropwizard.lifecycle.Managed;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Singleton;

import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.MasterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.StateManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityLeaderController implements Managed, LeaderLatchListener {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityLeaderController.class);

  private final StateManager stateManager;
  private final SingularityDriverManager driverManager;
  private final SingularityAbort abort;
  private final SingularityExceptionNotifier exceptionNotifier;

  private final long saveStateEveryMs;

  private final AtomicBoolean started = new AtomicBoolean();
  private final AtomicBoolean stopped = new AtomicBoolean();
  private final StatePoller statePoller;

  private volatile boolean master;

  @Inject
  public SingularityLeaderController(StateManager stateManager, SingularityConfiguration configuration, SingularityDriverManager driverManager, SingularityAbort abort, SingularityExceptionNotifier exceptionNotifier) {
    this.driverManager = driverManager;
    this.stateManager = stateManager;
    this.abort = abort;
    this.exceptionNotifier = exceptionNotifier;

    this.saveStateEveryMs = TimeUnit.SECONDS.toMillis(configuration.getSaveStateEverySeconds());
    this.statePoller = new StatePoller();

    this.master = false;
  }

  @Override
  public void start() throws Exception {
    if (!started.getAndSet(true)) {
      statePoller.start();
    }
  }

  @Override
  public void stop() throws Exception {
    if (started.get() && !stopped.getAndSet(true)) {
      LOG.info("Graceful STOP initiating...");
      statePoller.finish();
      LOG.info("STOP finished");
    }
  }

  @Override
  public void isLeader() {
    LOG.info("We are now the leader! Current status {}", driverManager.getCurrentStatus());

    master = true;

    if (driverManager.getCurrentStatus() != Protos.Status.DRIVER_RUNNING) {
      try {
        driverManager.startMesos();
        statePoller.wake();
      } catch (Throwable t) {
        LOG.error("While starting driver", t);
        exceptionNotifier.notify(t);
        abort.abort();
      }

      if (driverManager.getCurrentStatus() != Protos.Status.DRIVER_RUNNING) {
        abort.abort();
      }
    }
  }

  public boolean isMaster() {
    return master;
  }

  public Optional<MasterInfo> getMaster() {
    return driverManager.getMaster();
  }

  public Optional<Long> getLastOfferTimestamp() {
    return driverManager.getLastOfferTimestamp();
  }

  public Protos.Status getCurrentStatus() {
    return driverManager.getCurrentStatus();
  }

  @Override
  public void notLeader() {
    LOG.info("We are not the leader! Current status {}", driverManager.getCurrentStatus());

    master = false;

    if (driverManager.getCurrentStatus() == Protos.Status.DRIVER_RUNNING) {
      try {
        driverManager.stop();

        statePoller.wake();
      } catch (Throwable t) {
        LOG.error("While stopping driver", t);
        exceptionNotifier.notify(t);
      } finally {
        abort.abort();
      }
    }
  }

  private SingularityHostState getHostState() {
    final boolean master = isMaster();
    final Protos.Status driverStatus = getCurrentStatus();

    final RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
    final long uptime = mxBean.getUptime();

    final long now = System.currentTimeMillis();
    final Optional<Long> lastOfferTimestamp = getLastOfferTimestamp();
    final Optional<Long> millisSinceLastOfferTimestamp = lastOfferTimestamp.isPresent() ? Optional.of(now - lastOfferTimestamp.get()) : Optional.<Long> absent();

    String hostAddress = null;

    try {
      hostAddress = JavaUtils.getHostAddress();
    } catch (Exception e) {
      hostAddress = "Unknown";
    }

    String mesosMaster = null;
    Optional<MasterInfo> mesosMasterInfo = getMaster();

    if (mesosMasterInfo.isPresent()) {
      mesosMaster = MesosUtils.getMasterHostAndPort(mesosMasterInfo.get());
    }

    return new SingularityHostState(master, uptime, driverStatus.name(), millisSinceLastOfferTimestamp, hostAddress, JavaUtils.getHostName().or("unknown"), mesosMaster);
  }

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
        } catch (InterruptedException e) {
          LOG.trace("Caught interrupted exception, running the loop");
        } catch (Throwable t) {
          LOG.error("Caught exception while saving state", t);
          exceptionNotifier.notify(t);
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
