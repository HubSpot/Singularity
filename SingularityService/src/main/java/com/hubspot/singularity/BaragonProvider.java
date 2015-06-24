package com.hubspot.singularity;

import java.net.URI;
import java.net.URISyntaxException;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.client.BaragonClientProvider;
import com.hubspot.baragon.client.BaragonServiceClient;
import com.hubspot.horizon.HttpClient;
import com.hubspot.singularity.config.BaragonConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaragonProvider {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonProvider.class);
  private final SingularityConfiguration config;
  private final HttpClient httpClient;

  @Inject
  public BaragonProvider(SingularityConfiguration config,
                         @Named(SingularityMainModule.BARAGON_HTTP_CLIENT) HttpClient httpClient) {
    this.config = config;
    this.httpClient = httpClient;
  }

  public BaragonServiceClient create() {
    try {
      BaragonClientProvider provider = new BaragonClientProvider(httpClient);
      if (config.getLoadBalancerConfig().isPresent()) {
        BaragonConfiguration lbConfig = config.getLoadBalancerConfig().get();
        provider.setAuthkey(lbConfig.getAuthkey());
        provider.setContextPath(parseContextPath(lbConfig.getBaseUri()));
        provider.setHosts(parseHost(lbConfig.getBaseUri()));
      } else if (config.getLoadBalancerUri() != null) {
        LOG.info("Setting loadBalancerUri is deprecated, please specify the baragon => baseUri instead");
        provider.setHosts(parseHost(config.getLoadBalancerUri()));
        provider.setContextPath(parseContextPath(config.getLoadBalancerUri()));
        if (config.getLoadBalancerQueryParams().isPresent() && config.getLoadBalancerQueryParams().get().containsKey("authkey")) {
          LOG.info("Setting loadBalancerQueryParams is deprecated, please specify the baragon => authkey instead");
          provider.setAuthkey(Optional.of(config.getLoadBalancerQueryParams().get().get("authkey")));
        }
      }
      return provider.get();
    } catch (URISyntaxException e) {
      throw new RuntimeException(String.format("Cannot create BaragonServiceClient due %s", e));
    }
  }

  private String parseHost(String url) throws URISyntaxException {
    URI uri = new URI(url);
    return String.format("%s:%s",uri.getHost(), uri.getPort());
  }

  private String parseContextPath(String url) throws URISyntaxException {
    URI uri = new URI(url);
    String path = uri.getPath();
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    if (path.endsWith("/request")) {
      path = path.replace("/request", "");
    }
    return path;
  }
}
