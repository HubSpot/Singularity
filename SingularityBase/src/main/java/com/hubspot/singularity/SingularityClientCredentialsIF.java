package com.hubspot.singularity;

import org.immutables.value.Value.Immutable;

import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public interface SingularityClientCredentialsIF {
  String getHeaderName();

  String getToken();
}
