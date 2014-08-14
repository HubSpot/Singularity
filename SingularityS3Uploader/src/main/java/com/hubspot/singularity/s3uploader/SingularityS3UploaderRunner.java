package com.hubspot.singularity.s3uploader;

import java.util.Arrays;

import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;
import com.hubspot.singularity.runner.base.shared.SingularityRunner;
import com.hubspot.singularity.s3.base.config.SingularityS3ConfigurationLoader;
import com.hubspot.singularity.s3uploader.config.SingularityS3UploaderConfigurationLoader;
import com.hubspot.singularity.s3uploader.config.SingularityS3UploaderModule;

public class SingularityS3UploaderRunner {

  public static void main(String... args) {
    new SingularityS3UploaderRunner().run(args);
  }

  private SingularityS3UploaderRunner() {}

  public void run(String[] args) {
    new SingularityRunner().run(Arrays.asList(new SingularityRunnerBaseModule(new SingularityS3UploaderConfigurationLoader(), new SingularityS3ConfigurationLoader()), new SingularityS3UploaderModule()));
  }

}
