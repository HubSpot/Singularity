package com.hubspot.singularity.s3uploader;

import org.immutables.value.Value.Immutable;

import com.amazonaws.auth.AWSCredentials;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.immutables.style.HubSpotStyle;

@Immutable
@HubSpotStyle
public interface SingularityS3CredentialsIF extends AWSCredentials {
  @Override
  @JsonProperty("accessKey")
  String getAWSAccessKeyId();

  @Override
  @JsonProperty("secretKey")
  String getAWSSecretKey();

}
