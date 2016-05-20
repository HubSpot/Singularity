package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityPriorityKillRequestParent;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class PriorityManager extends CuratorAsyncManager {
    private static final String PRIORITY_ROOT = "/priority";
    private static final String PRIORITY_KILL = PRIORITY_ROOT + "/kill";

    private final Transcoder<SingularityPriorityKillRequestParent> priorityKillTranscoder;

    @Inject
    public PriorityManager(CuratorFramework curator, SingularityConfiguration configuration,
        MetricRegistry metricRegistry, Transcoder<SingularityPriorityKillRequestParent> priorityKillTranscoder) {
        super(curator, configuration, metricRegistry);
        this.priorityKillTranscoder = priorityKillTranscoder;
    }

    public Optional<SingularityPriorityKillRequestParent> getPriorityKill() {
        return getData(PRIORITY_KILL, priorityKillTranscoder);
    }

    public SingularityCreateResult createPriorityKill(SingularityPriorityKillRequestParent priorityKill) {
        return save(PRIORITY_KILL, priorityKill, priorityKillTranscoder);
    }

    public SingularityDeleteResult deletePriorityKill() {
        return delete(PRIORITY_KILL);
    }
}
