package com.hubspot.singularity.oomkiller;

import java.util.Arrays;

import com.hubspot.singularity.oomkiller.config.SingularityOOMKillerConfigurationLoader;
import com.hubspot.singularity.oomkiller.config.SingularityOOMKillerModule;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;
import com.hubspot.singularity.runner.base.shared.SingularityRunner;

public class SingularityOOMKillerRunner {

  public static void main(String... args) {
    new SingularityOOMKillerRunner().run(args);
  }
  
  private SingularityOOMKillerRunner() {}
  
  public void run(String[] args) {
    new SingularityRunner().run(Arrays.asList(new SingularityRunnerBaseModule(new SingularityOOMKillerConfigurationLoader()), new SingularityOOMKillerModule()));
  }

}
