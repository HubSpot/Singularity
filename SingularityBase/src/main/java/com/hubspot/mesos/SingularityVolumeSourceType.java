package com.hubspot.mesos;

import com.google.common.annotations.Beta;

import io.swagger.v3.oas.annotations.media.Schema;

@Beta
@Schema
public enum SingularityVolumeSourceType {
    UNKNOWN, DOCKER_VOLUME
    /*, SANDBOX_PATH, SECRET unimplemented */
}
