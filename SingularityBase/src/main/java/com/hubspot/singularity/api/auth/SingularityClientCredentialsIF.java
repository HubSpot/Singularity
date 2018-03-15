package com.hubspot.singularity.api.auth;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public interface SingularityClientCredentialsIF {
  String getHeaderName();

  String getToken();
}
