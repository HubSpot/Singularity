package com.hubspot.singularity.executor.task;

import com.hubspot.deploy.S3Artifact;
import java.util.List;

public interface LocalDownloadServiceFetcher {
  void downloadFiles(
    List<? extends S3Artifact> s3Artifacts,
    SingularityExecutorTask task
  )
    throws InterruptedException;

  default void start() {}

  default void stop() {}
}
