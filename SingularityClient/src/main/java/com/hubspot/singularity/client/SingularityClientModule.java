package com.hubspot.singularity.client;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpConfig;
import com.hubspot.horizon.ning.NingHttpClient;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityClientCredentials;

public class SingularityClientModule extends AbstractModule {

  public static final String HTTP_CLIENT_NAME = "singularity.http.client";

  // bind this name to not use the curator discovery, eg: http://localhost:5060,http://localhost:7000
  public static final String HOSTS_PROPERTY_NAME = "singularity.hosts";

  // bind this instead to provide a curator framework to discover singularity
  public static final String CURATOR_NAME = "singularity.curator";

  // bind this to provide the path for singularity eg: singularity/v2/api
  public static final String CONTEXT_PATH = "singularity.context.path";

  public static final String CREDENTIALS_PROPERTY_NAME = "singularity.client.credentials";

  // bind this to an int for the number of retry attempts on the request
  public static final String RETRY_ATTEMPTS = "singularity.client.retry.attempts";

  // bind this to a Predicate<HttpResponse> to say whether a request should be retried
  public static final String RETRY_STRATEGY = "singularity.client.retry.strategy";

  private final List<String> hosts;
  private final Optional<HttpConfig> httpConfig;

  public SingularityClientModule(HttpConfig httpConfig) {
    this(null, httpConfig);
  }

  public SingularityClientModule() {
    this(null, null);
  }

  public SingularityClientModule(List<String> hosts) {
    this(hosts, null);
  }

  public SingularityClientModule(List<String> hosts, HttpConfig httpConfig) {
    this.hosts = hosts;
    this.httpConfig = Optional.fromNullable(httpConfig);
  }

  @Override
  protected void configure() {
    ObjectMapper objectMapper = JavaUtils.newObjectMapper();

    HttpClient httpClient = new NingHttpClient(httpConfig.or(HttpConfig.newBuilder().setObjectMapper(objectMapper).build()));
    bind(HttpClient.class).annotatedWith(Names.named(HTTP_CLIENT_NAME)).toInstance(httpClient);

    bind(SingularityClient.class).toProvider(SingularityClientProvider.class).in(Scopes.SINGLETON);

    if (hosts != null) {
      bindHosts(binder()).toInstance(hosts);
    }
  }

  public static LinkedBindingBuilder<List<String>> bindHosts(Binder binder) {
    return binder.bind(new TypeLiteral<List<String>>() {}).annotatedWith(Names.named(HOSTS_PROPERTY_NAME));
  }

  public static LinkedBindingBuilder<String> bindContextPath(Binder binder) {
    return binder.bind(String.class).annotatedWith(Names.named(CONTEXT_PATH));
  }

  public static LinkedBindingBuilder<CuratorFramework> bindCurator(Binder binder) {
    return binder.bind(CuratorFramework.class).annotatedWith(Names.named(CURATOR_NAME));
  }

  public static LinkedBindingBuilder<SingularityClientCredentials> bindCredentials(Binder binder) {
    return binder.bind(SingularityClientCredentials.class).annotatedWith(Names.named(CREDENTIALS_PROPERTY_NAME));
  }
}
