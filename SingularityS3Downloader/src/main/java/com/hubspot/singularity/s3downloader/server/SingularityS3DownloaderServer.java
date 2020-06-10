package com.hubspot.singularity.s3downloader.server;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.hubspot.singularity.runner.base.shared.SingularityDriver;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;
import com.hubspot.singularity.s3downloader.config.SingularityS3DownloaderConfiguration;
import java.util.Optional;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.unixsocket.UnixSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingularityS3DownloaderServer implements SingularityDriver {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityS3DownloaderServer.class
  );

  private final SingularityS3DownloaderConfiguration configuration;
  private final SingularityS3Configuration s3Configuration;
  private final SingularityS3DownloaderHandler handler;
  private Optional<Server> server;

  @Inject
  public SingularityS3DownloaderServer(
    SingularityS3DownloaderConfiguration configuration,
    SingularityS3Configuration s3Configuration,
    SingularityS3DownloaderHandler handler
  ) {
    this.configuration = configuration;
    this.s3Configuration = s3Configuration;
    this.handler = handler;
    this.server = Optional.empty();
  }

  @Override
  public void shutdown() {
    if (server.isPresent()) {
      try {
        server.get().stop();
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }
  }

  @Override
  public void startAndWait() {
    Preconditions.checkState(!server.isPresent());

    Preconditions.checkState(
      s3Configuration.getS3AccessKey().isPresent(),
      "s3AccessKey not set!"
    );
    Preconditions.checkState(
      s3Configuration.getS3SecretKey().isPresent(),
      "s3SecretKey not set!"
    );
    Preconditions.checkState(
      s3Configuration.getLocalDownloadHttpPort().isPresent() ||
      s3Configuration.getLocalDownloadSocket().isPresent(),
      "Must specify either unix socket (localDownloadSocket) or port (localDownloadHttpPort)"
    );

    Server server = new Server();

    if (s3Configuration.getLocalDownloadHttpPort().isPresent()) {
      ServerConnector http = new ServerConnector(server);
      http.setHost("localhost");
      http.setPort(s3Configuration.getLocalDownloadHttpPort().get());
      http.setIdleTimeout(configuration.getHttpServerTimeout());
      server.addConnector(http);
    }

    if (s3Configuration.getLocalDownloadSocket().isPresent()) {
      UnixSocketConnector unix = new UnixSocketConnector(server);
      unix.setAcceptQueueSize(128);
      unix.setUnixSocket(s3Configuration.getLocalDownloadSocket().get());
      server.addConnector(unix);
    }

    server.setHandler(handler);

    try {
      LOG.info(
        "Starting server on {} (configuration: {})",
        s3Configuration.getLocalDownloadHttpPort(),
        configuration
      );

      server.start();

      this.server = Optional.of(server);

      server.join();
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }
}
