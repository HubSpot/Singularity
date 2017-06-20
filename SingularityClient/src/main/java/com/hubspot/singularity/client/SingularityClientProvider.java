package com.hubspot.singularity.client;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpResponse;
import com.hubspot.singularity.SingularityClientCredentials;

@Singleton
public class SingularityClientProvider implements Provider<SingularityClient> {
  private static final String DEFAULT_CONTEXT_PATH = "singularity/api";

  private final HttpClient httpClient;

  private String contextPath = DEFAULT_CONTEXT_PATH;
  private List<String> hosts = Collections.emptyList();
  private Optional<SingularityClientCredentials> credentials = Optional.absent();
  private boolean ssl = false;

  private int retryAttempts = 3;
  private Predicate<HttpResponse> retryStrategy = HttpResponse::isServerError;

  @Inject
  public SingularityClientProvider(@Named(SingularityClientModule.HTTP_CLIENT_NAME) HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Inject(optional=true) // optional because we have a default
  public SingularityClientProvider setContextPath(@Named(SingularityClientModule.CONTEXT_PATH) String contextPath) {
    this.contextPath = contextPath;
    return this;
  }

  @Inject(optional=true) // optional in case we use fixed hosts
  public SingularityClientProvider setHosts(@Named(SingularityClientModule.HOSTS_PROPERTY_NAME) String commaSeparatedHosts) {
    return setHosts(commaSeparatedHosts.split(","));
  }

  @Inject(optional=true) // optional in case we use Curator
  public SingularityClientProvider setCurator(@Named(SingularityClientModule.CURATOR_NAME) CuratorFramework curator) {
    return setHosts(getClusterMembers(curator));
  }

  @Inject(optional=true)
  public SingularityClientProvider setHosts(@Named(SingularityClientModule.HOSTS_PROPERTY_NAME) List<String> hosts) {
    this.hosts = ImmutableList.copyOf(hosts);
    return this;
  }

  @Inject(optional=true)
  public SingularityClientProvider setCredentials(@Named(SingularityClientModule.CREDENTIALS_PROPERTY_NAME) SingularityClientCredentials credentials) {
    this.credentials = Optional.of(credentials);
    return this;
  }

  @Inject(optional = true)
  public SingularityClientProvider setRetryAttempts(@Named(SingularityClientModule.RETRY_ATTEMPTS) int retryAttempts) {
    this.retryAttempts = retryAttempts;
    return this;
  }

  @Inject(optional = true)
  public SingularityClientProvider setRetryStrategy(@Named(SingularityClientModule.RETRY_STRATEGY) Predicate<HttpResponse> retryStrategy) {
    this.retryStrategy = retryStrategy;
    return this;
  }


  public SingularityClientProvider setHosts(String... hosts) {
    this.hosts = Arrays.asList(hosts);
    return this;
  }

  @Inject(optional=true)
  public SingularityClientProvider setSsl(boolean ssl) {
    this.ssl = ssl;
    return this;
  }

  @Override
  public SingularityClient get() {
    Preconditions.checkState(contextPath != null, "contextPath null");
    Preconditions.checkState(!hosts.isEmpty(), "no hosts provided");
    return new SingularityClient(contextPath, httpClient, hosts, credentials, ssl);
  }

  public SingularityClient get(Optional<SingularityClientCredentials> credentials) {
    Preconditions.checkState(contextPath != null, "contextPath null");
    Preconditions.checkState(!hosts.isEmpty(), "no hosts provided");
    Preconditions.checkNotNull(credentials);
    return new SingularityClient(contextPath, httpClient, hosts, credentials, ssl);
  }

  static String getClusterMembers(CuratorFramework curator) {
    try {
      final List<String> leaders = curator.getChildren().forPath(SingularityClusterManager.LEADER_PATH);
      final List<String> hosts = Lists.newArrayListWithCapacity(leaders.size());

      for (String leader : leaders) {
        byte[] data = curator.getData().forPath(ZKPaths.makePath(SingularityClusterManager.LEADER_PATH, leader));

        hosts.add(new String(data, UTF_8));
      }

      return Joiner.on(",").join(hosts);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
