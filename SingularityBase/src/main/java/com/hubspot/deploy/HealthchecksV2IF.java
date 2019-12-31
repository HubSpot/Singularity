package com.hubspot.deploy;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.hubspot.immutables.style.HubSpotStyle;

@Immutable
@HubSpotStyle
public interface HealthchecksV2IF {
  List<HealthcheckConfig> getHealthchecks();
}
