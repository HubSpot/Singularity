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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.BasicAWSCredentials;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import com.hubspot.singularity.runner.base.shared.SingularityUploaderType;
import com.hubspot.singularity.runner.base.shared.WatchServiceHelper;
import com.hubspot.singularity.s3.base.config.SingularityS3Configuration;
import com.hubspot.singularity.s3uploader.config.SingularityS3UploaderConfiguration;

public class SingularityS3UploaderDriver extends WatchServiceHelper implements SingularityDriver {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityS3UploaderDriver.class);

  private final SingularityRunnerBaseConfiguration baseConfiguration;
  private final SingularityS3Configuration s3Configuration;
  private final SingularityS3UploaderConfiguration configuration;
  private final ScheduledExecutorService scheduler;
  private final Map<S3UploadMetadata, SingularityUploader> metadataToUploader;
  private final Map<SingularityUploader, Long> uploaderLastHadFilesAt;
  private final Lock runLock;
  private final ExecutorService executorService;
  private final FileSystem fileSystem;
  private final Set<SingularityUploader> expiring;
  private final SingularityS3UploaderMetrics metrics;
  private final JsonObjectFileHelper jsonObjectFileHelper;
  private final ProcessUtils processUtils;
  private final String hostname;
  private final SingularityRunnerExceptionNotifier exceptionNotifier;

  private final List<S3UploadMetadata> immediateUploadMetadata;
  private final ReentrantLock lock;
  private final ConcurrentMap<SingularityUploader, Future<Integer>> immediateUploaders;

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

    this.immediateUploadMetadata = new ArrayList<>();
    this.lock = new ReentrantLock();
    this.immediateUploaders = Maps.newConcurrentMap();
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
    LOG.info("Gracefully shutting down Uploader, this may take a few moments...");

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
    if (metadataToUploader.isEmpty() && immediateUploaders.isEmpty()) {
      return 0;
    }

    int totesUploads = 0;

    // Check results of immediate uploaders
    List<SingularityUploader> toRetry = new ArrayList<>();
    List<SingularityUploader> toRemove = new ArrayList<>();
    for (Map.Entry<SingularityUploader, Future<Integer>> entry : immediateUploaders.entrySet()) {
      try {
        int uploadedFiles = entry.getValue().get();
        if (uploadedFiles != -1) {
          totesUploads += uploadedFiles;
          toRemove.add(entry.getKey());
        } else {
          toRetry.add(entry.getKey());
        }
      } catch (Throwable t) {
        metrics.error();
        LOG.error("Waiting on future", t);
        exceptionNotifier.notify(String.format("Error waiting on uploader future (%s)", t.getMessage()), t, ImmutableMap.of("metadataPath", entry.getKey().getMetadataPath().toString()));
        toRetry.add(entry.getKey());
      }
    }

    for (SingularityUploader uploader : toRemove) {
      metrics.getImmediateUploaderCounter().dec();
      immediateUploaders.remove(uploader);
      immediateUploadMetadata.remove(uploader.getUploadMetadata());
      expiring.remove(uploader);

      try {
        LOG.debug("Deleting finished immediate uploader {}", uploader.getMetadataPath());
        Files.delete(uploader.getMetadataPath());
      } catch (NoSuchFileException nfe) {
        LOG.warn("File {} was already deleted", nfe.getFile());
      } catch (IOException e) {
        LOG.warn("Couldn't delete {}", uploader.getMetadataPath(), e);
        exceptionNotifier.notify("Could not delete metadata file", e, ImmutableMap.of("metadataPath", uploader.getMetadataPath().toString()));
      }
    }

    for (SingularityUploader uploader : toRetry) {
      LOG.debug("Retrying immediate uploader {}", uploader);
      performImmediateUpload(uploader);
    }

    // Check regular uploaders
    int initialExpectedSize = Math.max(metadataToUploader.size(), 1);
    final Set<Path> filesToUpload = Collections.newSetFromMap(new ConcurrentHashMap<Path, Boolean>(initialExpectedSize * 2, 0.75f, initialExpectedSize));
    final Map<SingularityUploader, Future<Integer>> futures = Maps.newHashMapWithExpectedSize(initialExpectedSize);
    final Map<SingularityUploader, Boolean> finishing = Maps.newHashMapWithExpectedSize(initialExpectedSize);

    for (final SingularityUploader uploader : metadataToUploader.values()) {
      final boolean isFinished = isFinished(uploader);
      // do this here so we run at least once with isFinished = true
      finishing.put(uploader, isFinished);

      futures.put(uploader, executorService.submit(performUploadCallable(uploader, filesToUpload, isFinished, false)));
    }

    LOG.info("Waiting on {} future(s)", futures.size());

    final long now = System.currentTimeMillis();
    final Set<SingularityUploader> expiredUploaders = Sets.newHashSetWithExpectedSize(initialExpectedSize);

    for (Entry<SingularityUploader, Future<Integer>> uploaderToFuture : futures.entrySet()) {
      final SingularityUploader uploader = uploaderToFuture.getKey();
      try {
        final int foundFiles = uploaderToFuture.getValue().get();
        final boolean isFinished = finishing.get(uploader);

        if (foundFiles == 0 && shouldExpire(uploader, isFinished)) {
          LOG.info("Expiring {}", uploader);
          expiredUploaders.add(uploader);
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

    for (SingularityUploader expiredUploader : expiredUploaders) {
      metrics.getUploaderCounter().dec();

      metadataToUploader.remove(expiredUploader.getUploadMetadata());
      uploaderLastHadFilesAt.remove(expiredUploader);
      expiring.remove(expiredUploader);

      try {
        LOG.debug("Deleting expired uploader {}", expiredUploader.getMetadataPath());
        Files.delete(expiredUploader.getMetadataPath());
      } catch (NoSuchFileException nfe) {
        LOG.warn("File {} was already deleted", nfe.getFile());
      } catch (IOException e) {
        LOG.warn("Couldn't delete {}", expiredUploader.getMetadataPath(), e);
        exceptionNotifier.notify("Could not delete metadata file", e, ImmutableMap.of("metadataPath", expiredUploader.getMetadataPath().toString()));
      }
    }

    return totesUploads;
  }

  private Callable<Integer> performUploadCallable(final SingularityUploader uploader, final Set<Path> filesToUpload, final boolean finished, final boolean immediate) {
    return new Callable<Integer>() {
      @Override
      public Integer call() {

        Integer returnValue = 0;
        try {
          returnValue = uploader.upload(filesToUpload, finished);
        } catch (Throwable t) {
          metrics.error();
          LOG.error("Error while processing uploader {}", uploader, t);
          exceptionNotifier.notify(String.format("Error processing uploader (%s)", t.getMessage()), t, ImmutableMap.of("metadataPath", uploader.getMetadataPath().toString()));
          if (immediate) {
            return -1;
          }
        }
        return returnValue;
      }
    };
  }

  private void performImmediateUpload(final SingularityUploader uploader) {
    final Set<Path> filesToUpload = Collections
        .newSetFromMap(new ConcurrentHashMap<Path, Boolean>(Math.max(metadataToUploader.size(), 1) * 2, 0.75f, Math.max(metadataToUploader.size(), 1)));
    final boolean finished = isFinished(uploader);
    immediateUploaders.put(uploader, executorService.submit(performUploadCallable(uploader, filesToUpload, finished, true)));
  }

  private boolean shouldExpire(SingularityUploader uploader, boolean isFinished) {
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

  private boolean isFinished(SingularityUploader uploader) {
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

    SingularityUploader existingUploader = metadataToUploader.get(metadata);

    if (existingUploader != null) {
      if (metadata.getUploadImmediately().isPresent() && metadata.getUploadImmediately().get()) {
        LOG.debug("Existing metadata {} from {} changed to be immediate, forcing upload", metadata, filename);
        expiring.remove(existingUploader);
        if (canCreateImmediateUploader(metadata)) {
          metrics.getUploaderCounter().dec();
          metrics.getImmediateUploaderCounter().inc();

          metadataToUploader.remove(existingUploader.getUploadMetadata());
          uploaderLastHadFilesAt.remove(existingUploader);
          performImmediateUpload(existingUploader);
          return true;
        } else {
          return false;
        }
      } else if (existingUploader.getUploadMetadata().isFinished() == metadata.isFinished()) {
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
      Optional<BasicAWSCredentials> bucketCreds = Optional.absent();

      if (configuration.getS3BucketCredentials().containsKey(metadata.getS3Bucket())) {
        bucketCreds = Optional.of(configuration.getS3BucketCredentials().get(metadata.getS3Bucket()).toAWSCredentials());
      }

      final BasicAWSCredentials defaultCredentials = new BasicAWSCredentials(configuration.getS3AccessKey().or(s3Configuration.getS3AccessKey()).get(), configuration.getS3SecretKey().or(s3Configuration.getS3SecretKey()).get());

      final SingularityUploader uploader;

      if (metadata.getUploaderType() == SingularityUploaderType.S3) {
        uploader = new SingularityS3Uploader(bucketCreds.or(defaultCredentials), metadata, fileSystem, metrics, filename, configuration, hostname, exceptionNotifier);
      } else {
        uploader = new SingularityGCSUploader(metadata, fileSystem, metrics, filename, configuration, hostname, exceptionNotifier, jsonObjectFileHelper);
      }

      if (metadata.isFinished()) {
        expiring.add(uploader);
      }

      LOG.info("Created new uploader {}", uploader);

      if (metadata.getUploadImmediately().isPresent()
          && metadata.getUploadImmediately().get()) {
        if (canCreateImmediateUploader(metadata)) {
          metrics.getImmediateUploaderCounter().inc();
          this.performImmediateUpload(uploader);
          return true;
        } else {
          return false;
        }
      } else {
        metrics.getUploaderCounter().inc();
        metadataToUploader.put(metadata, uploader);
        uploaderLastHadFilesAt.put(uploader, System.currentTimeMillis());
        return true;
      }
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
        Optional<SingularityUploader> found = Iterables.tryFind(metadataToUploader.values(), new Predicate<SingularityUploader>() {
          @Override
          public boolean apply(SingularityUploader input) {
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

  private boolean canCreateImmediateUploader(S3UploadMetadata metadata) {
    try {
      if (lock.tryLock(400, TimeUnit.MILLISECONDS)) {
        if (this.immediateUploadMetadata.contains(metadata)) {
          LOG.debug("Already have an immediate uploader for metadata {}.", metadata);
          return false;
        } else {
          LOG.debug("Preparing to create new immediate uploader for metadata {}.", metadata);
          this.immediateUploadMetadata.add(metadata);
          return true;
        }
      } else {
        LOG.debug("Could not acquire lock to create an immediate uploader for metadata {}.", metadata);
        return false;
      }
    } catch (InterruptedException exn) {
      LOG.debug("Interrupted while waiting on a lock to create an immediate uploader for metadata {}.", metadata);
      return false;
    } finally {
      lock.unlock();
    }
  }
}
