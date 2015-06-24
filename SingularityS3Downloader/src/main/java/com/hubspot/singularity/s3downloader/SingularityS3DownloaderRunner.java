package com.hubspot.singularity.s3downloader;

import java.util.Arrays;

import com.google.common.collect.ImmutableSet;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.shared.SingularityRunner;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;
import com.hubspot.singularity.s3downloader.config.SingularityS3DownloaderConfiguration;
import com.hubspot.singularity.s3downloader.config.SingularityS3DownloaderModule;

public class SingularityS3DownloaderRunner {

  public static void main(String... args) {
    new SingularityS3DownloaderRunner().run(args);
  }

  private SingularityS3DownloaderRunner() {}

  public void run(String[] args) {
    new SingularityRunner().run(Arrays.asList(new SingularityRunnerBaseModule(SingularityS3DownloaderConfiguration.class, ImmutableSet.<Class<? extends BaseRunnerConfiguration>>of(SingularityS3Configuration.class)), new SingularityS3DownloaderModule()));
  }

}
