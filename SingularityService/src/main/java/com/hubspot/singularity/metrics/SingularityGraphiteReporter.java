package com.hubspot.singularity.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.config.GraphiteConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityGraphiteReporter {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityGraphiteReporter.class
  );

  private final GraphiteConfiguration graphiteConfiguration;
  private final MetricRegistry registry;
  private GraphiteReporter reporter = null;
  private Graphite graphite = null;
  private final String hostname;

  @Inject
  public SingularityGraphiteReporter(
    SingularityConfiguration configuration,
    MetricRegistry registry,
    @Named(SingularityMainModule.HOST_NAME_PROPERTY) String hostname
  ) {
    this.graphiteConfiguration = configuration.getGraphiteConfiguration();
    this.registry = registry;
    this.hostname =
      !Strings.isNullOrEmpty(graphiteConfiguration.getHostnameOmitSuffix()) &&
        hostname.endsWith(graphiteConfiguration.getHostnameOmitSuffix())
        ? hostname.substring(
          0,
          hostname.length() - graphiteConfiguration.getHostnameOmitSuffix().length()
        )
        : hostname;
  }

  private String buildGraphitePrefix() {
    if (Strings.isNullOrEmpty(graphiteConfiguration.getPrefix())) {
      return "";
    }

    return graphiteConfiguration.getPrefix().replace("{hostname}", hostname);
  }

  private Map<String, String> buildGraphiteTags() {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

    for (Map.Entry<String, String> entry : graphiteConfiguration.getTags().entrySet()) {
      builder.put(entry.getKey(), entry.getValue().replace("{hostname}", hostname));
    }

    return builder.build();
  }

  public void start() {
    if (!graphiteConfiguration.isEnabled()) {
      LOG.info("Not reporting data points to graphite.");
      return;
    }

    final String prefix = buildGraphitePrefix();
    final Map<String, String> tags = buildGraphiteTags();

    LOG.info(
      "Reporting data points to graphite server {}:{} every {} seconds with prefix '{}', predicates '{}', and tags '{}'.",
      graphiteConfiguration.getHostname(),
      graphiteConfiguration.getPort(),
      graphiteConfiguration.getPeriodSeconds(),
      prefix,
      JavaUtils.COMMA_JOINER.join(graphiteConfiguration.getPredicates()),
      JavaUtils.COMMA_EQUALS_MAP_JOINER.join(tags)
    );

    graphite =
      new GraphiteWithTags(
        new InetSocketAddress(
          graphiteConfiguration.getHostname(),
          graphiteConfiguration.getPort()
        ),
        SocketFactory.getDefault(),
        Charsets.UTF_8,
        tags
      );

    final GraphiteReporter.Builder reporterBuilder = GraphiteReporter.forRegistry(
      registry
    );

    if (!Strings.isNullOrEmpty(prefix)) {
      reporterBuilder.prefixedWith(prefix);
    }

    if (!graphiteConfiguration.getPredicates().isEmpty()) {
      reporterBuilder.filter(
        new MetricFilter() {

          @Override
          public boolean matches(String name, Metric metric) {
            for (String predicate : graphiteConfiguration.getPredicates()) {
              if (name.startsWith(predicate)) {
                return true;
              }
            }
            return false;
          }
        }
      );
    }

    reporter = reporterBuilder.build(graphite);
    reporter.start(graphiteConfiguration.getPeriodSeconds(), TimeUnit.SECONDS);
  }

  public void stop() throws Exception {
    if (graphite != null) {
      LOG.info("Closing GraphiteSender");
      graphite.close();
      LOG.info("Closed GraphiteSender");
    }
    if (reporter != null) {
      LOG.info("Closing GraphiteReporter");
      reporter.stop();
      LOG.info("Closed GraphiteReporter");
    }
  }
}
