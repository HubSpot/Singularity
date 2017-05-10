package com.hubspot.singularity;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
@JsonDeserialize(as = SingularityClientCredentials.class)
public interface SingularityClientCredentialsIF {
  String getHeaderName();

  String getToken();
}
