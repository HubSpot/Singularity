package com.hubspot.singularity.s3downloader.config;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.hubspot.singularity.runner.base.config.SingularityConfigurationLoader;

public class SingularityS3DownloaderConfigurationLoader extends SingularityConfigurationLoader {

  public static final String NUM_DOWNLOADER_THREADS = "s3downloader.downloader.threads";

  public static final String HTTP_SERVER_TIMEOUT = "s3downloader.http.timeout";


  public SingularityS3DownloaderConfigurationLoader() {
    super("/etc/singularity.s3downloader.properties");
  }

  @Override
  public void bindDefaults(Properties properties) {
    properties.put(HTTP_SERVER_TIMEOUT, Long.toString(TimeUnit.MINUTES.toMillis(30)));
    properties.put(NUM_DOWNLOADER_THREADS, "25");
  }

}
