package com.hubspot.singularity.s3.base.config;

import static com.hubspot.mesos.JavaUtils.obfuscateValue;

import java.util.Objects;

import com.amazonaws.auth.BasicAWSCredentials;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.runner.base.jackson.Obfuscate;

public class SingularityS3Credentials {
  private final String accessKey;
  private final String secretKey;

  @JsonCreator
  public SingularityS3Credentials(@JsonProperty("accessKey") String accessKey,
                                  @JsonProperty("secretKey") String secretKey) {
    this.accessKey = accessKey;
    this.secretKey = secretKey;
  }

  @Obfuscate
  public String getAccessKey() {
    return accessKey;
  }

  @Obfuscate
  public String getSecretKey() {
    return secretKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityS3Credentials that = (SingularityS3Credentials) o;
    return Objects.equals(accessKey, that.accessKey) &&
            Objects.equals(secretKey, that.secretKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessKey, secretKey);
  }

  @Override
  public String toString() {
    return "SingularityS3Credentials[" +
            "accessKey='" + obfuscateValue(accessKey) + '\'' +
            ", secretKey='" + obfuscateValue(secretKey) + '\'' +
            ']';
  }

  @JsonIgnore
  public BasicAWSCredentials toAWSCredentials() {
    return new BasicAWSCredentials(accessKey, secretKey);
  }
}
