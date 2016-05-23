package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityPriorityRequestParent;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class PriorityManager extends CuratorAsyncManager {
    private static final String PRIORITY_ROOT = "/priority";
    private static final String PRIORITY_KILL = PRIORITY_ROOT + "/kill";
    private static final String PRIORITY_FREEZE = PRIORITY_ROOT + "/freeze";

    private final Transcoder<SingularityPriorityRequestParent> priorityRequestParentTranscoder;

    @Inject
    public PriorityManager(CuratorFramework curator, SingularityConfiguration configuration,
        MetricRegistry metricRegistry, Transcoder<SingularityPriorityRequestParent> priorityRequestParentTranscoder) {
        super(curator, configuration, metricRegistry);
        this.priorityRequestParentTranscoder = priorityRequestParentTranscoder;
    }

    public Optional<SingularityPriorityRequestParent> getActivePriorityKill() {
        return getData(PRIORITY_KILL, priorityRequestParentTranscoder);
    }

    public SingularityCreateResult createPriorityKill(SingularityPriorityRequestParent priorityKill) {
        return save(PRIORITY_KILL, priorityKill, priorityRequestParentTranscoder);
    }

    public SingularityDeleteResult deleteActivePriorityKill() {
        return delete(PRIORITY_KILL);
    }

    public Optional<SingularityPriorityRequestParent> getActivePriorityFreeze() {
        return getData(PRIORITY_FREEZE, priorityRequestParentTranscoder);
    }

    public SingularityCreateResult createPriorityFreeze(SingularityPriorityRequestParent priorityFreeze) {
        return save(PRIORITY_FREEZE, priorityFreeze, priorityRequestParentTranscoder);
    }

    public SingularityDeleteResult deleteActivePriorityFreeze() {
        return delete(PRIORITY_FREEZE);
    }
}
