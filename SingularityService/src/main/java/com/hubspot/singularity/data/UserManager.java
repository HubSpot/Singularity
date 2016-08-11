package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;

public class UserManager extends CuratorManager {

  @Inject
  public UserManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry) {
    super(curator, configuration, metricRegistry);
  }

}
