package com.hubspot.singularity.s3downloader.server;

public interface DownloadListener {

  public void notifyDownloadFinished(SingularityS3DownloaderAsyncHandler handler);

}
