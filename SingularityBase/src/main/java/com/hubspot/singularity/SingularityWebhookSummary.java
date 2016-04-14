package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;

public class SingularityWebhookSummary {

    private final SingularityWebhook webhook;
    private final List<SingularityDeployUpdate> queuedDeployUpdates;
    private final List<SingularityRequestHistory> queuedReqeustUpdates;
    private final List<SingularityTaskHistoryUpdate> queuedTaskUpdates;

    @JsonCreator
    public SingularityWebhookSummary(@JsonProperty("webhook") SingularityWebhook webhook,
      @JsonProperty("queuedDeployUpdates") List<SingularityDeployUpdate> queuedDeployUpdates, @JsonProperty("queuedRequestUpdates") List<SingularityRequestHistory> queuedReqeustUpdates,
      @JsonProperty("queuedTaskUpdates") List<SingularityTaskHistoryUpdate> queuedTaskUpdates) {
        this.webhook = webhook;
        this.queuedDeployUpdates = queuedDeployUpdates;
        this.queuedReqeustUpdates = queuedReqeustUpdates;
        this.queuedTaskUpdates = queuedTaskUpdates;
    }

    @ApiModelProperty(required=true, value="Described webhook.")
    public SingularityWebhook getWebhook() {
        return webhook;
    }

    @ApiModelProperty(required=true, value="Queued deploy updates for described webhook.")
    public List<SingularityDeployUpdate> getQueuedDeployUpdates() {
        return queuedDeployUpdates;
    }

    @ApiModelProperty(required=true, value="Queued request updates for described webhook.")
    public List<SingularityRequestHistory> getQueuedReqeustUpdates() {
        return queuedReqeustUpdates;
    }

    @ApiModelProperty(required=true, value="Queued task updates for described webhook.")
    public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdates() {
        return queuedTaskUpdates;
    }

    @Override
    public String toString() {
        return "SingularityWebhookSummary [webhook=" + webhook + ", queuedDeployUpdates=" + queuedDeployUpdates + ", queuedReqeustUpdates=" + queuedReqeustUpdates + ", queuedTaskUpdates=" + queuedTaskUpdates + "]";
    }
}
