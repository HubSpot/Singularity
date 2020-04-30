package com.hubspot.singularity;

import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.io.IOException;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingularityOkHttpClient extends OkHttpClient implements Managed {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityOkHttpClient.class
  );

  @Inject
  SingularityOkHttpClient() {}

  @Override
  public void start() {}

  @Override
  public void stop() {
    dispatcher().executorService().shutdown();
    connectionPool().evictAll();
    if (cache() != null) {
      try {
        cache().delete();
      } catch (IOException e) {
        LOG.warn("Unable to clean up client cache!");
      }
    }
  }
}
