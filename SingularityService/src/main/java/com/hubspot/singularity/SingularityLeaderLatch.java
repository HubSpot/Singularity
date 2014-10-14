package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import io.dropwizard.lifecycle.Managed;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;

import com.google.common.net.HostAndPort;

public class SingularityLeaderLatch extends LeaderLatch implements Managed {
  private static final String LEADER_PATH = "/leader";

  private final AtomicBoolean started = new AtomicBoolean();
  private final AtomicBoolean stopped = new AtomicBoolean();

  @Inject
  public SingularityLeaderLatch(@Named(SingularityMainModule.HTTP_HOST_AND_PORT) final HostAndPort httpHostAndPort,
      final CuratorFramework curatorFramework, final Set<LeaderLatchListener> listeners) throws Exception {
    super(checkNotNull(curatorFramework, "curatorFramework is null"), LEADER_PATH, httpHostAndPort.toString());

    checkNotNull(listeners, "listeners is null");
    for (LeaderLatchListener listener : listeners) {
      addListener(listener);
    }
  }

  @Override
  public void start() throws Exception {
    if (!started.getAndSet(true)) {
      super.start();
    }
  }

  @Override
  public void stop() throws Exception {
    if (started.get() && !stopped.getAndSet(true)) {
      super.close();
    }
  }
}
