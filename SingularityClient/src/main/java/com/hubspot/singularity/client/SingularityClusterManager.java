package com.hubspot.singularity.client;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SingularityClusterManager {

  private final CuratorFramework curator;
  private final String contextPath;

  private final SingularityClientProvider clientProvider;

  static final String LEADER_PATH = "/leader";

  @Inject
  public SingularityClusterManager(@Named(SingularityClientModule.CONTEXT_PATH) String contextPath, @Named(SingularityClientModule.CURATOR_NAME) CuratorFramework curator, SingularityClientProvider clientProvider) {
    this.contextPath = contextPath;
    this.curator = curator;
    this.clientProvider = clientProvider;
  }

  public List<String> getClusterNames() {
    try {
      return curator.getChildren().forPath("");
    } catch (KeeperException.NoNodeException e) {
      throw new RuntimeException("Singularity cluster not set up yet?");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String getClusterMembers(String cluster) {
    return SingularityClientProvider.getClusterMembers(curator);
  }

  @SuppressWarnings("deprecation")
  public SingularityClient getClusterClient(String cluster) {
    return clientProvider.setContextPath(contextPath).setHosts(getClusterMembers(cluster)).get();
  }

}
