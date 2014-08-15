package com.hubspot.singularity.s3uploader.config;

import java.util.Properties;

import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.config.SingularityConfigurationLoader;

public class SingularityS3UploaderConfigurationLoader extends SingularityConfigurationLoader {

  public static final String POLL_MILLIS = "s3uploader.poll.for.shutdown.millis";

  public static final String CHECK_FOR_UPLOADS_EVERY_SECONDS = "s3uploader.check.uploads.every.seconds";
  public static final String STOP_CHECKING_AFTER_HOURS_WITHOUT_NEW_FILE = "s3uploader.stop.checking.after.hours.without.new.file";

  public static final String EXECUTOR_CORE_THREADS = "s3uploader.core.threads";

  public SingularityS3UploaderConfigurationLoader() {
    super("/etc/singularity.s3uploader.properties", Optional.of("singularity-s3uploader.log"));
  }

  @Override
  protected void bindDefaults(Properties properties) {
    properties.put(POLL_MILLIS, "1000");
    properties.put(EXECUTOR_CORE_THREADS, "3");

    properties.put(CHECK_FOR_UPLOADS_EVERY_SECONDS, "600");
    properties.put(STOP_CHECKING_AFTER_HOURS_WITHOUT_NEW_FILE, "168");
  }

}
