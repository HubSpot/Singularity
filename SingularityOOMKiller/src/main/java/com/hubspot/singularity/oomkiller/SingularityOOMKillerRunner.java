package com.hubspot.singularity.oomkiller;

import java.util.Arrays;

import com.hubspot.mesos.client.SingularityMesosClientModule;
import com.hubspot.singularity.client.SingularityClientModule;
import com.hubspot.singularity.oomkiller.config.SingularityOOMKillerConfiguration;
import com.hubspot.singularity.oomkiller.config.SingularityOOMKillerModule;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;
import com.hubspot.singularity.runner.base.shared.SingularityRunner;

public class SingularityOOMKillerRunner {

  public static void main(String... args) {
    new SingularityOOMKillerRunner().run(args);
  }

  private SingularityOOMKillerRunner() {}

  public void run(String[] args) {
    new SingularityRunner().run(Arrays.asList(new SingularityRunnerBaseModule(SingularityOOMKillerConfiguration.class), new SingularityOOMKillerModule(), new SingularityClientModule(), new SingularityMesosClientModule()));
  }

}
