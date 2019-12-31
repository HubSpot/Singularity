package com.hubspot.deploy;

import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.hubspot.immutables.style.HubSpotStyle;

@Immutable
@HubSpotStyle
public interface HealthcheckResultIF {
  String getName();
  HealthCheckStatus getStatus();
  Optional<String> getMessage();
}
