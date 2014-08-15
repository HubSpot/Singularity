package com.hubspot.singularity.s3.base.config;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.config.SingularityConfigurationLoader;

public class SingularityS3ConfigurationLoader extends SingularityConfigurationLoader {

  public static final String ARTIFACT_CACHE_DIRECTORY = "artifact.cache.directory";

  public static final String S3_ACCESS_KEY = "s3.access.key";
  public static final String S3_SECRET_KEY = "s3.secret.key";

  public static final String S3_CHUNK_SIZE = "s3.downloader.chunk.size";
  public static final String S3_DOWNLOAD_TIMEOUT_MILLIS = "s3.downloader.timeout.millis";

  public static final String LOCAL_DOWNLOAD_HTTP_PORT = "s3.downloader.http.port";
  public static final String LOCAL_DOWNLOAD_HTTP_DOWNLOAD_PATH = "s3.downloader.http.download.path";

  public SingularityS3ConfigurationLoader() {
    super("/etc/singularity.s3base.properties", Optional.<String> absent());
  }

  protected void bindDefaults(Properties properties) {
    properties.put(S3_ACCESS_KEY, "");
    properties.put(S3_SECRET_KEY, "");

    properties.put(S3_CHUNK_SIZE, "104857600");
    properties.put(S3_DOWNLOAD_TIMEOUT_MILLIS, Long.toString(TimeUnit.MINUTES.toMillis(1)));

    properties.put(LOCAL_DOWNLOAD_HTTP_DOWNLOAD_PATH, "/download");
    properties.put(LOCAL_DOWNLOAD_HTTP_PORT, "7070");
  }

}
