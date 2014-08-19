package com.hubspot.singularity.s3uploader;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.runner.base.shared.JsonObjectFileHelper;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;
import com.hubspot.singularity.runner.base.shared.SingularityDriver;
import com.hubspot.singularity.runner.base.shared.WatchServiceHelper;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;
import com.hubspot.singularity.s3uploader.config.SingularityS3UploaderConfiguration;

public class SingularityS3UploaderDriver extends WatchServiceHelper implements SingularityDriver {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityS3UploaderDriver.class);

  private final SingularityS3UploaderConfiguration configuration;
  private final ScheduledExecutorService scheduler;
  private final Map<S3UploadMetadata, SingularityS3Uploader> metadataToUploader;
  private final Map<SingularityS3Uploader, Long> uploaderLastHadFilesAt;
  private final Lock runLock;
  private final ExecutorService executorService;
  private final FileSystem fileSystem;
  private final S3Service s3Service;
  private final Set<SingularityS3Uploader> expiring;
  private final SingularityS3UploaderMetrics metrics;
  private final JsonObjectFileHelper jsonObjectFileHelper;

  private ScheduledFuture<?> future;

  @Inject
  public SingularityS3UploaderDriver(SingularityS3UploaderConfiguration configuration, SingularityS3Configuration s3Configuration, SingularityS3UploaderMetrics metrics, JsonObjectFileHelper jsonObjectFileHelper) {
    super(configuration.getPollForShutDownMillis(), configuration.getS3MetadataDirectory(), ImmutableList.of(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE));

    this.metrics = metrics;

    this.fileSystem = FileSystems.getDefault();
    try {
      this.s3Service = new RestS3Service(new AWSCredentials(s3Configuration.getS3AccessKey(), s3Configuration.getS3SecretKey()));
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }

    this.jsonObjectFileHelper = jsonObjectFileHelper;
    this.configuration = configuration;

    this.metadataToUploader = Maps.newHashMap();
    this.uploaderLastHadFilesAt = Maps.newHashMap();
    this.expiring = Sets.newHashSet();

    this.metrics.setExpiringCollection(expiring);

    this.runLock = new ReentrantLock();

    this.executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("SingularityS3Uploader-%d").build());
    this.scheduler = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityS3Driver-%d").build());
  }

  private void readInitialFiles() throws IOException {
    final long start = System.currentTimeMillis();
    LOG.info("Scanning for metadata files (*{}) in {}", configuration.getS3MetadataSuffix(), configuration.getS3MetadataDirectory());

    int foundFiles = 0;

    for (Path file : JavaUtils.iterable(configuration.getS3MetadataDirectory())) {
      if (!isS3MetadataFile(file)) {
        continue;
      }

      if (handleNewOrModifiedS3Metadata(file)) {
        foundFiles++;
      }
    }

    LOG.info("Found {} file(s) in {}", foundFiles, JavaUtils.duration(start));
  }

  @Override
  public void startAndWait() {
    try {
      readInitialFiles();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }

    future = this.scheduler.scheduleAtFixedRate(new Runnable() {

      @Override
      public void run() {
        final long start = System.currentTimeMillis();

        runLock.lock();

        if (isStopped()) {
          LOG.warn("Driver is stopped, not checking uploads");
          return;
        }

        int uploads = 0;
        final int uploaders = metadataToUploader.size();
        metrics.startUploads();

        try {
          uploads = checkUploads();
        } catch (Throwable t) {
          LOG.error("Uncaught exception while checking {} upload(s)", uploaders, t);
        } finally {
          runLock.unlock();
          metrics.finishUploads();
          LOG.info("Found {} items from {} uploader(s) in {}", uploads, uploaders, JavaUtils.duration(start));
        }
      }
    }, configuration.getCheckUploadsEverySeconds(), configuration.getCheckUploadsEverySeconds(), TimeUnit.SECONDS);

    try {
      super.watch();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  @Override
  public void shutdown() {
    final long start = System.currentTimeMillis();
    LOG.info("Gracefully shutting down S3Uploader, this may take a few moments...");

    runLock.lock();
    try {
      if (!super.stop()) {
        LOG.info("Already shutting down, ignoring request");
        return;
      }
    } finally {
      runLock.unlock();
    }

    future.cancel(false);

    scheduler.shutdown();
    executorService.shutdown();

    LOG.info("Shut down in {}", JavaUtils.duration(start));
  }

  private int checkUploads() {
    if (metadataToUploader.isEmpty()) {
      return 0;
    }

    final Set<Path> filesToUpload = Collections.newSetFromMap(new ConcurrentHashMap<Path, Boolean>(metadataToUploader.size() * 2, 0.75f, metadataToUploader.size()));
    final Map<SingularityS3Uploader, Future<Integer>> futures = Maps.newHashMapWithExpectedSize(metadataToUploader.size());

    for (final SingularityS3Uploader uploader : metadataToUploader.values()) {
      futures.put(uploader, executorService.submit(new Callable<Integer>() {

        @Override
        public Integer call() {

          Integer returnValue = 0;
          try {
            returnValue = uploader.upload(filesToUpload);
          } catch (Throwable t) {
            LOG.error("Error while processing uploader {}", uploader, t);
          }
          return returnValue;
        }
      }));
    }

    LOG.info("Waiting on {} future(s)", futures.size());
    int totesUploads = 0;

    final long now = System.currentTimeMillis();
    final Set<SingularityS3Uploader> expiredUploaders = Sets.newHashSetWithExpectedSize(metadataToUploader.size());

    // TODO cancel/timeouts?
    for (Entry<SingularityS3Uploader, Future<Integer>> uploaderToFuture : futures.entrySet()) {
      final SingularityS3Uploader uploader = uploaderToFuture.getKey();
      try {
        final int foundFiles = uploaderToFuture.getValue().get();

        if (foundFiles == 0) {
          final long durationSinceLastFile = now - uploaderLastHadFilesAt.get(uploader);
          final boolean isFinished = isFinished(uploader);

          if ((durationSinceLastFile > configuration.getStopCheckingAfterMillisWithoutNewFile()) || isFinished) {
            LOG.info("Expiring uploader {}", uploader);
            expiredUploaders.add(uploader);
          } else {
            LOG.trace("Not expiring uploader {}, duration {} (max {}), isFinished: {})", uploader, durationSinceLastFile, configuration.getStopCheckingAfterMillisWithoutNewFile(), isFinished);
          }
        } else {
          LOG.trace("Updating uploader {} last expire time", uploader);
          uploaderLastHadFilesAt.put(uploader, now);
        }

        totesUploads += foundFiles;
      } catch (Throwable t) {
        LOG.error("Waiting on future", t);
      }
    }

    for (SingularityS3Uploader expiredUploader : expiredUploaders) {
      metrics.getUploaderCounter().dec();

      metadataToUploader.remove(expiredUploader.getUploadMetadata());
      uploaderLastHadFilesAt.remove(expiredUploader);
      expiring.remove(expiredUploader);

      try {
        Files.delete(expiredUploader.getMetadataPath());
      } catch (IOException e) {
        LOG.warn("Couldn't delete {}", expiredUploader.getMetadataPath(), e);
      }
    }

    return totesUploads;
  }

  private boolean isFinished(SingularityS3Uploader uploader) {
    return expiring.contains(uploader);
  }

  private boolean handleNewOrModifiedS3Metadata(Path filename) throws IOException {
    Optional<S3UploadMetadata> maybeMetadata = readS3UploadMetadata(filename);

    if (!maybeMetadata.isPresent()) {
      return false;
    }

    final S3UploadMetadata metadata = maybeMetadata.get();

    SingularityS3Uploader existingUploader = metadataToUploader.get(metadata);

    if (existingUploader != null) {
      if (existingUploader.getUploadMetadata().isFinished() == metadata.isFinished()) {
        LOG.debug("Ignoring metadata {} from {} because there was already one present", metadata, filename);
        return false;
      } else {
        LOG.info("Toggling uploader {} finish state to {}", existingUploader, metadata.isFinished());

        if (metadata.isFinished()) {
          expiring.add(existingUploader);
        } else {
          expiring.remove(existingUploader);
        }

        return true;
      }
    }

    try {
      metrics.getUploaderCounter().inc();

      SingularityS3Uploader uploader = new SingularityS3Uploader(s3Service, metadata, fileSystem, metrics, filename);

      if (metadata.isFinished()) {
        expiring.add(uploader);
      }

      LOG.info("Created new uploader {}", uploader);

      metadataToUploader.put(metadata, uploader);
      uploaderLastHadFilesAt.put(uploader, System.currentTimeMillis());
      return true;
    } catch (Throwable t) {
      LOG.info("Ignoring metadata {} because uploader couldn't be created", metadata, t);
      return false;
    }
  }

  @Override
  protected boolean processEvent(Kind<?> kind, final Path filename) throws IOException {
    metrics.getFilesystemEventsMeter().mark();

    if (!isS3MetadataFile(filename)) {
      return false;
    }

    runLock.lock();

    if (isStopped()) {
      LOG.warn("Driver is stopped, ignoring file watch event for {}", filename);
      return false;
    }

    try {
      final Path fullPath = configuration.getS3MetadataDirectory().resolve(filename);

      if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
        Optional<SingularityS3Uploader> found = Iterables.tryFind(metadataToUploader.values(), new Predicate<SingularityS3Uploader>() {
          @Override
          public boolean apply(SingularityS3Uploader input) {
            return input.getMetadataPath().equals(fullPath);
          }
        });

        LOG.trace("Found {} to match deleted path {}", found, filename);

        if (found.isPresent()) {
          expiring.add(found.get());
        }
      } else {
        return handleNewOrModifiedS3Metadata(fullPath);
      }

      return false;
    } finally {
      runLock.unlock();
    }
  }

  private Optional<S3UploadMetadata> readS3UploadMetadata(Path filename) throws IOException {
    return jsonObjectFileHelper.read(filename, LOG, S3UploadMetadata.class);
  }

  private boolean isS3MetadataFile(Path filename) {
    if (!filename.toString().endsWith(configuration.getS3MetadataSuffix())) {
      LOG.trace("Ignoring a file {} without {} suffix", filename, configuration.getS3MetadataSuffix());
      return false;
    }

    return true;
  }

}
