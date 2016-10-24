package com.hubspot.singularity.s3uploader;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import org.jets3t.service.security.AWSCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.runner.base.config.MissingConfigException;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseModule;
import com.hubspot.singularity.runner.base.configuration.SingularityRunnerBaseConfiguration;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.runner.base.shared.JsonObjectFileHelper;
import com.hubspot.singularity.runner.base.shared.ProcessUtils;
import com.hubspot.singularity.runner.base.shared.S3UploadMetadata;
import com.hubspot.singularity.runner.base.shared.SingularityDriver;
import com.hubspot.singularity.runner.base.shared.WatchServiceHelper;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;
import com.hubspot.singularity.s3uploader.config.SingularityS3UploaderConfiguration;

public class SingularityS3UploaderDriver extends WatchServiceHelper implements SingularityDriver {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityS3UploaderDriver.class);

  private final SingularityRunnerBaseConfiguration baseConfiguration;
  private final SingularityS3Configuration s3Configuration;
  private final SingularityS3UploaderConfiguration configuration;
  private final ScheduledExecutorService scheduler;
  private final Map<S3UploadMetadata, SingularityS3Uploader> metadataToUploader;
  private final Map<SingularityS3Uploader, Long> uploaderLastHadFilesAt;
  private final Lock runLock;
  private final ExecutorService executorService;
  private final FileSystem fileSystem;
  private final Set<SingularityS3Uploader> expiring;
  private final SingularityS3UploaderMetrics metrics;
  private final JsonObjectFileHelper jsonObjectFileHelper;
  private final ProcessUtils processUtils;
  private final String hostname;
  private final SingularityRunnerExceptionNotifier exceptionNotifier;

  private ScheduledFuture<?> future;

  @Inject
  public SingularityS3UploaderDriver(SingularityRunnerBaseConfiguration baseConfiguration, SingularityS3UploaderConfiguration configuration, SingularityS3Configuration s3Configuration,
      SingularityS3UploaderMetrics metrics, JsonObjectFileHelper jsonObjectFileHelper, @Named(SingularityRunnerBaseModule.HOST_NAME_PROPERTY) String hostname, SingularityRunnerExceptionNotifier exceptionNotifier) {
    super(configuration.getPollForShutDownMillis(), Paths.get(baseConfiguration.getS3UploaderMetadataDirectory()), ImmutableList.of(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE));

    this.baseConfiguration = baseConfiguration;
    this.s3Configuration = s3Configuration;
    this.metrics = metrics;

    this.fileSystem = FileSystems.getDefault();

    this.jsonObjectFileHelper = jsonObjectFileHelper;
    this.configuration = configuration;

    this.metadataToUploader = Maps.newHashMap();
    this.uploaderLastHadFilesAt = Maps.newHashMap();
    this.expiring = Sets.newHashSet();

    this.metrics.setExpiringCollection(expiring);

    this.runLock = new ReentrantLock();

    this.processUtils = new ProcessUtils(LOG);

    this.executorService = JavaUtils.newFixedTimingOutThreadPool(configuration.getExecutorMaxUploadThreads(), TimeUnit.SECONDS.toMillis(30), "SingularityS3Uploader-%d");
    this.scheduler = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityS3Driver-%d").build());
    this.hostname = hostname;
    this.exceptionNotifier = exceptionNotifier;
  }

  private void readInitialFiles() throws IOException {
    final long start = System.currentTimeMillis();
    LOG.info("Scanning for metadata files (*{}) in {}", baseConfiguration.getS3UploaderMetadataSuffix(), baseConfiguration.getS3UploaderMetadataDirectory());

    int foundFiles = 0;

    for (Path file : JavaUtils.iterable(Paths.get(baseConfiguration.getS3UploaderMetadataDirectory()))) {
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
    if (!configuration.getS3AccessKey().or(s3Configuration.getS3AccessKey()).isPresent()) {
      throw new MissingConfigException("s3AccessKey not set in any s3 configs!");
    }
    if (!configuration.getS3SecretKey().or(s3Configuration.getS3SecretKey()).isPresent()) {
      throw new MissingConfigException("s3SecretKey not set in any s3 configs!");
    }

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
          exceptionNotifier.notify(String.format("Error checking uploads (%s)", t.getMessage()), t, Collections.<String, String>emptyMap());
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

    if (future != null) {
      future.cancel(false);
    }

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
    final Map<SingularityS3Uploader, Boolean> finishing = Maps.newHashMapWithExpectedSize(metadataToUploader.size());

    for (final SingularityS3Uploader uploader : metadataToUploader.values()) {
      final boolean isFinished = isFinished(uploader);
      // do this here so we run at least once with isFinished = true
      finishing.put(uploader, isFinished);

      futures.put(uploader, executorService.submit(new Callable<Integer>() {

        @Override
        public Integer call() {

          Integer returnValue = 0;
          try {
            returnValue = uploader.upload(filesToUpload, isFinished);
          } catch (Throwable t) {
            metrics.error();
            LOG.error("Error while processing uploader {}", uploader, t);
            exceptionNotifier.notify(String.format("Error processing uploader (%s)", t.getMessage()), t, ImmutableMap.of("metadataPath", uploader.getMetadataPath().toString()));
          }
          return returnValue;
        }
      }));
    }

    LOG.info("Waiting on {} future(s)", futures.size());
    int totesUploads = 0;

    final long now = System.currentTimeMillis();
    final Set<SingularityS3Uploader> expiredUploaders = Sets.newHashSetWithExpectedSize(metadataToUploader.size());

    for (Entry<SingularityS3Uploader, Future<Integer>> uploaderToFuture : futures.entrySet()) {
      final SingularityS3Uploader uploader = uploaderToFuture.getKey();
      try {
        final int foundFiles = uploaderToFuture.getValue().get();
        final boolean isFinished = finishing.get(uploader);

        if (foundFiles == 0) {
          if (shouldExpire(uploader, isFinished)) {
            LOG.info("Expiring {}", uploader);
            expiredUploaders.add(uploader);
          }
        } else {
          LOG.trace("Updating uploader {} last expire time", uploader);
          uploaderLastHadFilesAt.put(uploader, now);
        }

        totesUploads += foundFiles;
      } catch (Throwable t) {
        metrics.error();
        LOG.error("Waiting on future", t);
        exceptionNotifier.notify(String.format("Error waiting on uploader future (%s)", t.getMessage()), t, ImmutableMap.of("metadataPath", uploader.getMetadataPath().toString()));
      }
    }

    for (SingularityS3Uploader expiredUploader : expiredUploaders) {
      metrics.getUploaderCounter().dec();

      metadataToUploader.remove(expiredUploader.getUploadMetadata());
      uploaderLastHadFilesAt.remove(expiredUploader);
      expiring.remove(expiredUploader);

      try {
        Closeables.close(expiredUploader, true);

        LOG.debug("Deleting expired uploader {}", expiredUploader.getMetadataPath());
        Files.delete(expiredUploader.getMetadataPath());
      } catch (NoSuchFileException nfe) {
        LOG.warn("File {} was alrady deleted");
      } catch (IOException e) {
        LOG.warn("Couldn't delete {}", expiredUploader.getMetadataPath(), e);
        exceptionNotifier.notify("Could not delete metadata file", e, ImmutableMap.of("metadataPath", expiredUploader.getMetadataPath().toString()));
      }
    }

    return totesUploads;
  }

  private boolean shouldExpire(SingularityS3Uploader uploader, boolean isFinished) {
    if (isFinished) {
      return true;
    }

    if (uploader.getUploadMetadata().getFinishedAfterMillisWithoutNewFile().isPresent()) {
      if (uploader.getUploadMetadata().getFinishedAfterMillisWithoutNewFile().get() < 0) {
        LOG.trace("{} never expires", uploader);
        return false;
      }
    }

    final long durationSinceLastFile = System.currentTimeMillis() - uploaderLastHadFilesAt.get(uploader);

    final long expireAfterMillis = uploader.getUploadMetadata().getFinishedAfterMillisWithoutNewFile().or(configuration.getStopCheckingAfterMillisWithoutNewFile());

    if (durationSinceLastFile > expireAfterMillis) {
      return true;
    } else {
      LOG.trace("Not expiring uploader {}, duration {} (max {}), isFinished: {})", uploader, JavaUtils.durationFromMillis(durationSinceLastFile), JavaUtils.durationFromMillis(expireAfterMillis), isFinished);
    }

    return false;
  }

  private boolean isFinished(SingularityS3Uploader uploader) {
    if (expiring.contains(uploader)) {
      return true;
    }

    if (uploader.getUploadMetadata().getPid().isPresent()) {
      if (!processUtils.doesProcessExist(uploader.getUploadMetadata().getPid().get())) {
        LOG.info("Pid {} not present - expiring uploader {}", uploader.getUploadMetadata().getPid().get(), uploader);
        expiring.add(uploader);
        return true;
      }
    }

    return false;
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

      Optional<AWSCredentials> bucketCreds = Optional.absent();

      if (configuration.getS3BucketCredentials().containsKey(metadata.getS3Bucket())) {
        bucketCreds = Optional.of(configuration.getS3BucketCredentials().get(metadata.getS3Bucket()).toAWSCredentials());
      }

      final AWSCredentials defaultCredentials = new AWSCredentials(configuration.getS3AccessKey().or(s3Configuration.getS3AccessKey()).get(), configuration.getS3SecretKey().or(s3Configuration.getS3SecretKey()).get());

      SingularityS3Uploader uploader = new SingularityS3Uploader(bucketCreds.or(defaultCredentials), metadata, fileSystem, metrics, filename, configuration, hostname, exceptionNotifier);

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

    try {
      if (isStopped()) {
        LOG.warn("Driver is stopped, ignoring file watch event for {}", filename);
        return false;
      }

      final Path fullPath = Paths.get(baseConfiguration.getS3UploaderMetadataDirectory()).resolve(filename);

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
    try {
      return jsonObjectFileHelper.read(filename, LOG, S3UploadMetadata.class);
    } catch (NoSuchFileException nsfe) {
      LOG.warn("Tried to read {}, but it doesn't exist!", filename);
      return Optional.absent();
    }
  }

  private boolean isS3MetadataFile(Path filename) {
    if (!filename.toString().endsWith(baseConfiguration.getS3UploaderMetadataSuffix())) {
      LOG.trace("Ignoring a file {} without {} suffix", filename, baseConfiguration.getS3UploaderMetadataSuffix());
      return false;
    }

    return true;
  }

}
