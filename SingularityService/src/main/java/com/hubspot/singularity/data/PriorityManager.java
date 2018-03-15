package com.hubspot.singularity.data;

import java.util.Map;
import java.util.Optional;

import org.apache.curator.framework.CuratorFramework;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.api.common.SingularityCreateResult;
import com.hubspot.singularity.api.common.SingularityDeleteResult;
import com.hubspot.singularity.api.disasters.SingularityPriorityFreezeParent;
import com.hubspot.singularity.api.request.RequestType;
import com.hubspot.singularity.api.request.SingularityRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class PriorityManager extends CuratorAsyncManager {
    private static final String PRIORITY_ROOT = "/priority";
    private static final String PRIORITY_KILL = PRIORITY_ROOT + "/kill";
    private static final String PRIORITY_FREEZE = PRIORITY_ROOT + "/freeze";

    private final Transcoder<SingularityPriorityFreezeParent> priorityFreezeParentTranscoder;

    private final Map<RequestType, Double> defaultTaskPriorityLevelForRequestType;
    private final double defaultTaskPriorityLevel;

    @Inject
    public PriorityManager(CuratorFramework curator, SingularityConfiguration configuration,
        MetricRegistry metricRegistry, Transcoder<SingularityPriorityFreezeParent> priorityFreezeParentTranscoder) {
        super(curator, configuration, metricRegistry);
        this.priorityFreezeParentTranscoder = priorityFreezeParentTranscoder;
        this.defaultTaskPriorityLevelForRequestType = configuration.getDefaultTaskPriorityLevelForRequestType();
        this.defaultTaskPriorityLevel = configuration.getDefaultTaskPriorityLevel();
    }

    public boolean checkPriorityKillExists() {
        return checkExists(PRIORITY_KILL).isPresent();
    }

    public SingularityCreateResult setPriorityKill() {
        return create(PRIORITY_KILL);
    }

    public SingularityDeleteResult clearPriorityKill() {
        return delete(PRIORITY_KILL);
    }

    public Optional<SingularityPriorityFreezeParent> getActivePriorityFreeze() {
        return getData(PRIORITY_FREEZE, priorityFreezeParentTranscoder);
    }

    public SingularityCreateResult createPriorityFreeze(SingularityPriorityFreezeParent priorityFreeze) {
        return save(PRIORITY_FREEZE, priorityFreeze, priorityFreezeParentTranscoder);
    }

    public SingularityDeleteResult deleteActivePriorityFreeze() {
        return delete(PRIORITY_FREEZE);
    }

    public double getTaskPriorityLevelForRequest(SingularityRequest request) {
        return request.getTaskPriorityLevel().orElse(Optional.ofNullable(defaultTaskPriorityLevelForRequestType.get(request.getRequestType())).orElse(defaultTaskPriorityLevel));
    }
}
