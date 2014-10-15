package com.hubspot.singularity.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.ning.http.client.AsyncHttpClient;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

@Singleton
public class SingularityClientProvider implements Provider<SingularityClient> {
  private static final String DEFAULT_CONTEXT_PATH = "singularity";

  private final ObjectMapper objectMapper;
  private final AsyncHttpClient httpClient;

  private String contextPath = DEFAULT_CONTEXT_PATH;
  private List<String> hosts = Collections.emptyList();

  @Inject
  public SingularityClientProvider(@Named(SingularityClientModule.HTTP_CLIENT_NAME) AsyncHttpClient httpClient, @Named(SingularityClientModule.OBJECT_MAPPER_NAME) ObjectMapper objectMapper) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  @Inject(optional=true) // optional because we have a default
  public SingularityClientProvider setContextPath(@Named(SingularityClientModule.CONTEXT_PATH) String contextPath) {
    this.contextPath = contextPath;
    return this;
  }

  @Inject(optional=true) // optional in case we use Curator
  public SingularityClientProvider setHosts(@Named(SingularityClientModule.HOSTS_PROPERTY_NAME) String commaSeparatedHosts) {
    return setHosts(commaSeparatedHosts.split(","));
  }

  @Inject(optional=true) // optional in case we use fixed hosts
  public SingularityClientProvider setCurator(@Named(SingularityClientModule.CURATOR_NAME) CuratorFramework curator) {
    return setHosts(getClusterMembers(curator));
  }

  public SingularityClientProvider setHosts(List<String> hosts) {
    this.hosts = ImmutableList.copyOf(hosts);
    return this;
  }

  public SingularityClientProvider setHosts(String... hosts) {
    this.hosts = Arrays.asList(hosts);
    return this;
  }

  @Override
  public SingularityClient get() {
    Preconditions.checkState(contextPath != null, "contextPath null");
    Preconditions.checkState(!hosts.isEmpty(), "no hosts provided");
    return new SingularityClient(contextPath, httpClient, objectMapper, hosts);
  }

  @Deprecated
  public SingularityClient buildClient(String contextPath, String hosts) {
    return new SingularityClient(contextPath, httpClient, objectMapper, hosts);
  }

  static String getClusterMembers(CuratorFramework curator) {
    try {
      final List<String> leaders = curator.getChildren().forPath(SingularityClusterManager.LEADER_PATH);
      final List<String> hosts = Lists.newArrayListWithCapacity(leaders.size());

      for (String leader : leaders) {
        byte[] data = curator.getData().forPath(ZKPaths.makePath(SingularityClusterManager.LEADER_PATH, leader));

        hosts.add(JavaUtils.toString(data));
      }

      return Joiner.on(",").join(hosts);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
