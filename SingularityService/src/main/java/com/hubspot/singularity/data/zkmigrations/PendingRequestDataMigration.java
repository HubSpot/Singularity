package com.hubspot.singularity.data.zkmigrations;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.transcoders.Transcoder;

public class PendingRequestDataMigration extends ZkDataMigration {
  private static final Logger LOG = LoggerFactory.getLogger(PendingRequestDataMigration.class);

  private final RequestManager requestManager;
  private final CuratorFramework curator;
  private final Transcoder<SingularityPendingRequest> requestTranscoder;


  @Inject
  public PendingRequestDataMigration(RequestManager requestManager,
                                     CuratorFramework curator,
                                     Transcoder<SingularityPendingRequest> requestTranscoder) {
    super(10);

    this.requestManager = requestManager;
    this.curator = curator;
    this.requestTranscoder = requestTranscoder;
  }

  @Override
  public void applyMigration() {
    String basePath = "/requests/pending";
    LOG.warn("Starting migration to re-write pending request paths");
    long start = System.currentTimeMillis();
    int rewrittenPaths = 0;

    try {
      if (curator.checkExists().forPath(basePath) == null) {
        return;
      }
    } catch (Exception exn) {
      LOG.error("Could not check existence of pending request path", exn);
      throw new RuntimeException(exn);
    }

    try {
      List<String> childPaths = curator.getChildren()
          .forPath(basePath);

      for (String childPath : childPaths) {
        SingularityPendingRequest pendingRequest = requestTranscoder.fromBytes(curator.getData()
            .forPath(String.format("%s/%s", basePath, childPath)));
        if (pendingRequest.getPendingType() == PendingType.IMMEDIATE) {
          String rewrittenPath = new SingularityDeployKey(pendingRequest.getRequestId(), pendingRequest.getDeployId())
              .getId();
          LOG.warn("Rewriting path {} to {}", childPath, String.format("%s%s", rewrittenPath, pendingRequest.getTimestamp()));
          requestManager.addToPendingQueue(pendingRequest);
          curator.delete()
              .forPath(String.format("%s/%s", basePath, childPath));
          rewrittenPaths += 1;
        } else {
          LOG.warn("Not rewriting path {}, already correct", childPath);
        }
      }
    } catch (Exception exn) {
      LOG.error("Connection to Zookeeper failed while running migration", exn);
      throw new RuntimeException(exn);
    }

    LOG.warn("Applied PendingRequestDataMigration to {} requests in {}", rewrittenPaths, JavaUtils.duration(start));
  }
}
