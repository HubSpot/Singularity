package com.hubspot.singularity.oomkiller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.hubspot.singularity.oomkiller.config.SingularityOOMKillerConfiguration;
import com.hubspot.singularity.runner.base.shared.ProcessFailedException;
import com.hubspot.singularity.runner.base.shared.SimpleProcessManager;

public class SingularityCGroupProcLocator {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityCGroupProcLocator.class);

  private final String procsFormat;

  @Inject
  public SingularityCGroupProcLocator(SingularityOOMKillerConfiguration oomKillerConfiguration) {
    this.procsFormat = oomKillerConfiguration.getCgroupProcsPathFormat();
  }

  public List<String> getPids(String container) throws InterruptedException, ProcessFailedException {
    String path = String.format(procsFormat, container);

    return new SimpleProcessManager(LOG).runCommandWithOutput(ImmutableList.of("cat", path));
  }

}
