package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HostAndPort;

import io.dropwizard.lifecycle.Managed;

public class SingularityLeaderLatch extends LeaderLatch implements Managed {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityLeaderLatch.class);

  private static final String LEADER_PATH = "/leader";

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
  public void stop() throws Exception {
    close();
  }

  @Override
  public void close() throws IOException {
    LOG.info("Stopping leader latch");
    super.close();
  }

}
