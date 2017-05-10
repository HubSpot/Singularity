package com.hubspot.deploy;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.singularity.HealthcheckProtocol;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
public interface HealthcheckOptionsIF {
  @NotNull
  @ApiModelProperty(required=true, value="Healthcheck uri to hit")
  String getUri();

  @ApiModelProperty(required=false, value="Perform healthcheck on this dynamically allocated port (e.g. 0 for first port), defaults to first port")
  Optional<Integer> getPortIndex();

  @ApiModelProperty(required=false, value="Perform healthcheck on this port (portIndex cannot also be used when using this setting)")
  Optional<Long> getPortNumber();

  @ApiModelProperty(required=false, value="Healthcheck protocol - HTTP or HTTPS")
  Optional<HealthcheckProtocol> getProtocol();

  @ApiModelProperty(required=false, value="Consider the task unhealthy/failed if the app has not started responding to healthchecks in this amount of time")
  Optional<Integer> getStartupTimeoutSeconds();

  @ApiModelProperty(required=false, value="Wait this long before issuing the first healthcheck")
  Optional<Integer> getStartupDelaySeconds();

  @ApiModelProperty(required=false, value="Time to wait after a failed healthcheck to try again in seconds.")
  Optional<Integer> getStartupIntervalSeconds();

  @ApiModelProperty(required=false, value="Time to wait after a valid but failed healthcheck response to try again in seconds.")
  Optional<Integer> getIntervalSeconds();

  @ApiModelProperty(required=false, value="Single healthcheck HTTP timeout in seconds.")
  Optional<Integer> getResponseTimeoutSeconds();

  @ApiModelProperty(required=false, value="Maximum number of times to retry an individual healthcheck before failing the deploy.")
  Optional<Integer> getMaxRetries();

  @ApiModelProperty(required=false, value="Fail the healthcheck with no further retries if one of these status codes is returned")
  List<Integer> getFailureStatusCodes();
}
