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

public class SingularityPendingRequestWithRunIdMigration extends ZkDataMigration {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityPendingRequestWithRunIdMigration.class);

  private final RequestManager requestManager;
  private final CuratorFramework curator;
  private final Transcoder<SingularityPendingRequest> requestTranscoder;

  @Inject
  public SingularityPendingRequestWithRunIdMigration(RequestManager requestManager,
                                                     CuratorFramework curator,
                                                     Transcoder<SingularityPendingRequest> requestTranscoder) {
    super(11);

    this.requestManager = requestManager;
    this.curator = curator;
    this.requestTranscoder = requestTranscoder;
  }

  @Override
  public void applyMigration() {
    String basePath = "/requests/pending";
    LOG.warn("Starting migration to rewrite one-off pending request paths to include run IDs");

    long start = System.currentTimeMillis();
    int rewrittenPaths = 0;

    try {
      if (curator.checkExists().forPath(basePath) == null) {
        LOG.error("Unable to run migration because pending requests base path doesn't exist!");
        return;
      }
    } catch (Exception exn) {
      LOG.error("Could not check existence of pending request path", exn);
      throw new RuntimeException(exn);
    }

    try {
      List<String> childPaths = curator.getChildren()
          .forPath(basePath);

      for (String originalBasename : childPaths) {
        SingularityPendingRequest pendingRequest = requestTranscoder.fromBytes(curator.getData()
            .forPath(String.format("%s/%s", basePath, originalBasename)));
        if (pendingRequest.getPendingType() == PendingType.IMMEDIATE || pendingRequest.getPendingType() == PendingType.ONEOFF) {
          String deployKey = new SingularityDeployKey(pendingRequest.getRequestId(), pendingRequest.getDeployId()).getId();
          String rewrittenBasename = String.format("%s%s%s", deployKey, pendingRequest.getTimestamp(), pendingRequest.getRunId().or(""));
          if (originalBasename.equals(rewrittenBasename)) {
            LOG.warn("Not rewriting znode {}, because it had no runId and was therefore already correct", originalBasename);
          } else {
            LOG.warn("Rewriting znode {} to {}", originalBasename, rewrittenBasename);
            requestManager.addToPendingQueue(pendingRequest);
            curator.delete()
                .forPath(String.format("%s/%s", basePath, originalBasename));
            rewrittenPaths += 1;
          }
        } else {
          LOG.warn("Not rewriting znode {}, already correct", originalBasename);
        }
      }



    } catch (Exception exn) {
      LOG.error("Connect to Zookeeper failed while running migration", exn);
      throw new RuntimeException(exn);
    }

    LOG.warn("Applied PendingRequestDataMigration to {} requests in {}", rewrittenPaths, JavaUtils.duration(start));
  }
}
