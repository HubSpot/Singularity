package com.hubspot.singularity.client;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.ning.http.client.AsyncHttpClient;

public class SingularityClusterManager {

  private final CuratorFramework curator;
  private final String contextPath;
  private final AsyncHttpClient httpClient;
  private final ObjectMapper objectMapper;

  private static final String LEADER_PATH = "/leader";

  @Inject
  public SingularityClusterManager(@Named(SingularityClientModule.CONTEXT_PATH) String contextPath, @Named(SingularityClientModule.CURATOR_NAME) CuratorFramework curator, @Named(SingularityClientModule.HTTP_CLIENT_NAME) AsyncHttpClient httpClient,
      @Named(SingularityClientModule.OBJECT_MAPPER_NAME) ObjectMapper objectMapper) {
    this.contextPath = contextPath;
    this.curator = curator;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
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
    try {
      final List<String> leaders = curator.getChildren().forPath(LEADER_PATH);
      final List<String> hosts = Lists.newArrayListWithCapacity(leaders.size());

      for (String leader : leaders) {
        byte[] data = curator.getData().forPath(ZKPaths.makePath(LEADER_PATH, leader));

        hosts.add(JavaUtils.toString(data));
      }

      return Joiner.on(",").join(hosts);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public SingularityClient getClusterClient(String cluster) {
    return new SingularityClient(contextPath, httpClient, objectMapper, getClusterMembers(cluster));
  }

}
