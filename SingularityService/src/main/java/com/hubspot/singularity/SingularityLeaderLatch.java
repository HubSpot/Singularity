package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.net.HostAndPort;
import java.io.IOException;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingularityLeaderLatch extends LeaderLatch {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityLeaderLatch.class);

  private static final String LEADER_PATH = "/leader";

  @Inject
  public SingularityLeaderLatch(
    CuratorFramework curatorFramework,
    Set<LeaderLatchListener> listeners,
    @Named(SingularityMainModule.HTTP_HOST_AND_PORT) HostAndPort httpHostAndPort
  ) throws Exception {
    super(
      checkNotNull(curatorFramework, "curatorFramework is null"),
      LEADER_PATH,
      httpHostAndPort.toString()
    );
    checkNotNull(listeners, "listeners is null");
    for (LeaderLatchListener listener : listeners) {
      addListener(listener);
    }
  }

  @Override
  public void close() throws IOException {
    LOG.info("Stopping leader latch");
    super.close();
  }
}
