package com.hubspot.deploy;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;

import com.hubspot.immutables.style.HubSpotStyle;

@Immutable
@HubSpotStyle
public interface HealthcheckConfigIF {
  @Default
  default String getName() {
    return "default";
  }

  @Default
  default boolean isEnabled() {
    return true;
  }

  @Default
  default int getIntervalSeconds() {
    return 5;
  }

  @Default
  default int getMaxRetries() {
    return 20;
  }

  @Derived
  default int getTimeout() {
    return getIntervalSeconds() * getMaxRetries();
  }

  @Derived
  default String getResultFilePath() {
    return String.format("./healthcheck-result-%s.json", getName());
  }
}
