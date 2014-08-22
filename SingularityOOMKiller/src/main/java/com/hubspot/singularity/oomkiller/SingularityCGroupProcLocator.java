package com.hubspot.singularity.oomkiller;

import java.util.List;

import com.google.inject.Inject;
import com.hubspot.singularity.oomkiller.config.SingularityOOMKillerConfiguration;
import com.hubspot.singularity.runner.base.shared.SimpleProcessExecutor;

public class SingularityCGroupProcLocator {

  private final String procsFormat;
  private final SimpleProcessExecutor simpleProcessExecutor;

  @Inject
  public SingularityCGroupProcLocator(SingularityOOMKillerConfiguration oomKillerConfiguration, SimpleProcessExecutor simpleProcessExecutor) {
    this.procsFormat = oomKillerConfiguration.getCgroupProcsPathFormat();
    this.simpleProcessExecutor = simpleProcessExecutor;
  }

  public List<String> getPids(String container) {
    String path = String.format(procsFormat, container);

    return simpleProcessExecutor.getProcessOutput("cat", path);
  }

}
