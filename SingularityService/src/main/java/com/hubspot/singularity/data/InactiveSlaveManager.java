package com.hubspot.singularity.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class InactiveSlaveManager extends CuratorManager {
  private static final String ROOT_PATH = "/inactive";
  private final Transcoder<String[]> inactiveSlaveTranscoder;

  @Inject
  public InactiveSlaveManager(CuratorFramework curator,
                              SingularityConfiguration configuration,
                              MetricRegistry metricRegistry,
                              Transcoder<String[]> inactiveSlaveTranscoder) {
    super(curator, configuration, metricRegistry);

    this.inactiveSlaveTranscoder = inactiveSlaveTranscoder;
  }

  public Set<String> getInactiveSlaves() {
    Optional<String[]> maybeInactiveSlaves = getData(ROOT_PATH, this.inactiveSlaveTranscoder);
    if (maybeInactiveSlaves.isPresent()) {
      return new HashSet<String>(Arrays.asList(maybeInactiveSlaves.get()));
    } else {
      return new HashSet<String>();
    }
  }

  public void deactiveSlave(String slave) {
    Set<String> inactiveSlaves = getInactiveSlaves();
    inactiveSlaves.add(slave);

    save(ROOT_PATH, inactiveSlaves.toArray(new String[0]), inactiveSlaveTranscoder);
  }

  public void activateSlave(String slave) {
    Set<String> inactiveSlaves = getInactiveSlaves();
    inactiveSlaves.remove(slave);

    save(ROOT_PATH, inactiveSlaves.toArray(new String[0]), inactiveSlaveTranscoder);
  }
}
