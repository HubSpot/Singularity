package com.hubspot.singularity.data.zkmigrations;

import java.util.HashMap;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.transcoders.JsonTranscoder;
import com.hubspot.singularity.data.transcoders.Transcoder;

public class SingularityRequestTypeMigration extends ZkDataMigration {
    private static final Logger LOG = LoggerFactory.getLogger(SingularityRequestTypeMigration.class);

    private final RequestManager requestManager;
    private final CuratorFramework curator;
    private final Transcoder<OldSingularityRequestWithState> oldSingularityRequestTranscoder;

    @Inject
    public SingularityRequestTypeMigration(ObjectMapper objectMapper, CuratorFramework curator, RequestManager requestManager) {
        super(9);

        this.curator = curator;
        this.requestManager = requestManager;
        this.oldSingularityRequestTranscoder = new JsonTranscoder<>(objectMapper, OldSingularityRequestWithState.class);

    }

    @Override
    public void applyMigration() {
        LOG.info("Starting migration to ensure all Requests have a value for requestType field");

        final long start = System.currentTimeMillis();
        int num = 0;

        for (String requestId : requestManager.getAllRequestIds()) {
            try {
                OldSingularityRequestWithState requestWithState = oldSingularityRequestTranscoder.fromBytes(curator.getData().forPath("/requests/all/" + requestId));

                if (requestWithState.getRequest().getOriginalRequestType().isPresent()) {
                    LOG.info("Skipping {}, requestType is present ({})", requestId, requestWithState.getRequest().getOriginalRequestType().get());
                    continue;
                }

                LOG.info("Saving request {} with requestType {}", requestId, requestWithState.getRequest().getRequestType());
                curator.setData().forPath("/requests/all/" + requestId, oldSingularityRequestTranscoder.toBytes(requestWithState));
                num++;
            } catch (Throwable t) {
                LOG.error("Failed to read {}", requestId, t);
                throw Throwables.propagate(t);
            }
        }

        LOG.info("Applied {} in {}", num, JavaUtils.duration(start));
    }

    static class OldSingularityRequest {

        private final String id;
        private final Optional<RequestType> originalRequestType;
        private final RequestType requestType;

        private final Optional<String> schedule;
        private final Optional<Boolean> daemon;
        private final Optional<Boolean> loadBalanced;

        private final Map<String, Object> unknownFields = new HashMap<>();

        @JsonCreator
        public OldSingularityRequest(@JsonProperty("id") String id,
            @JsonProperty("requestType") Optional<RequestType> originalRequestType,
            @JsonProperty("schedule") Optional<String> schedule,
            @JsonProperty("daemon") Optional<Boolean> daemon,
            @JsonProperty("loadBalanced") Optional<Boolean> loadBalanced) {
            this.id = id;
            this.schedule = schedule;
            this.daemon = daemon;
            this.loadBalanced = loadBalanced;
            this.originalRequestType = originalRequestType == null ? Optional.<RequestType>absent() : originalRequestType;
            this.requestType = this.originalRequestType.or(RequestType.fromDaemonAndScheduleAndLoadBalanced(schedule, daemon, loadBalanced));
        }

        @JsonAnySetter
        public void setUnknownField(String name, Object value) {
            unknownFields.put(name, value);
        }

        @JsonAnyGetter
        public Map<String, Object> getUnknownFields() {
            return unknownFields;
        }

        public String getId() {
            return id;
        }

        public RequestType getRequestType() {
            return requestType;
        }

        public Optional<String> getSchedule() {
            return schedule;
        }

        public Optional<Boolean> getDaemon() {
            return daemon;
        }

        public Optional<Boolean> getLoadBalanced() {
            return loadBalanced;
        }

        @JsonIgnore
        public Optional<RequestType> getOriginalRequestType() {
            return originalRequestType;
        }
    }

    static class OldSingularityRequestWithState {
        private final OldSingularityRequest request;
        private final RequestState state;
        private final long timestamp;

        @JsonCreator
        public OldSingularityRequestWithState(@JsonProperty("request") OldSingularityRequest request,
            @JsonProperty("state") RequestState state,
            @JsonProperty("timestamp") long timestamp) {
            this.request = request;
            this.state = state;
            this.timestamp = timestamp;
        }

        public OldSingularityRequest getRequest() {
            return request;
        }

        public RequestState getState() {
            return state;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
