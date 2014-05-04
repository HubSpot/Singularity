package com.hubspot.singularity.s3uploader.config;

import java.util.Properties;

import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseConfigurationLoader;

public class SingularityS3UploaderConfigurationLoader extends SingularityRunnerBaseConfigurationLoader {

  public static final String POLL_MILLIS = "s3uploader.poll.for.shutdown.millis";
  public static final String S3_ACCESS_KEY = "s3uploader.s3.access.key";
  public static final String S3_SECRET_KEY = "s3uploader.s3.secret.key";
  
  public static final String CHECK_FOR_UPLOADS_EVERY_SECONDS = "s3uploader.check.uploads.every.seconds";
  public static final String STOP_CHECKING_AFTER_SECONDS_WITHOUT_NEW_FILE = "s3uploader.stop.checking.after.seconds.without.new.file";
  
  public static final String EXECUTOR_CORE_THREADS = "s3uploader.core.threads";
  
  @Override
  protected void bindDefaults(Properties properties) {
    super.bindDefaults(properties);

    properties.put(POLL_MILLIS, "1000");
    properties.put(EXECUTOR_CORE_THREADS, "3");
    
    properties.put(CHECK_FOR_UPLOADS_EVERY_SECONDS, "60");
    properties.put(STOP_CHECKING_AFTER_SECONDS_WITHOUT_NEW_FILE, "600");
  }
  
}
