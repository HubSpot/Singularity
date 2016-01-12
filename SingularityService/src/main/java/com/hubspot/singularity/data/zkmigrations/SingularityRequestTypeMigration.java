package com.hubspot.singularity.data.zkmigrations;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.data.RequestManager;

public class SingularityRequestTypeMigration extends ZkDataMigration {
    private final RequestManager requestManager;

    @Inject
    public SingularityRequestTypeMigration(RequestManager requestManager) {
        super(9);
        this.requestManager = requestManager;
    }

    @Override
    public void applyMigration() {
        for (SingularityRequestWithState requestWithState : requestManager.getRequests()) {
            if (!requestWithState.getRequest().getDaemon().isPresent()) {
                continue;
            }

            final SingularityRequest requestWithoutDaemon = requestWithState.getRequest().toBuilder()
                .setDaemon(Optional.<Boolean>absent())
                .build();

            requestManager.update(requestWithoutDaemon, System.currentTimeMillis(), Optional.of("migration"), Optional.of("SingularityRequestTypeMigration"));
        }
    }
}
