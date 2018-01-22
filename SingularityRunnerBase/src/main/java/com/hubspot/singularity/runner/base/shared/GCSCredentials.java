package com.hubspot.singularity.runner.base.shared;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GCSCredentials {
  private final GCSCredentialsType type;
  private final String clientId;

  // UserCredentials specific fields
  private final String clientSecret;
  private final String refreshToken;
  private final String grantType;

  // ServiceAccountCredentials specific fields
  private final String clientEmail;
  private final String privateKey;
  private final String privateKeyId;
  private final String projectId;
  private final Collection<String> scopes;

  @JsonCreator

  public GCSCredentials(@JsonProperty("type") GCSCredentialsType type,
                        @JsonProperty("clientId") String clientId,
                        @JsonProperty("clientSecret") String clientSecret,
                        @JsonProperty("refreshToken") String refreshToken,
                        @JsonProperty("grantType") String grantType,
                        @JsonProperty("clientEmail") String clientEmail,
                        @JsonProperty("privateKey") String privateKey,
                        @JsonProperty("privateKeyId") String privateKeyId,
                        @JsonProperty("projectId") String projectId,
                        @JsonProperty("scopes") Collection<String> scopes) {
    this.type = type;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.refreshToken = refreshToken;
    this.grantType = grantType;
    this.clientEmail = clientEmail;
    this.privateKey = privateKey;
    this.privateKeyId = privateKeyId;
    this.projectId = projectId;
    this.scopes = scopes;
  }

  public GCSCredentialsType getType() {
    return type;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public String getGrantType() {
    return grantType;
  }

  public String getClientEmail() {
    return clientEmail;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public String getPrivateKeyId() {
    return privateKeyId;
  }

  public String getProjectId() {
    return projectId;
  }

  public Collection<String> getScopes() {
    return scopes;
  }

  @Override
  public String toString() {
    return "GCSCredentials{" +
        "type=" + type +
        ", clientId='" + clientId + '\'' +
        ", clientSecret='" + clientSecret + '\'' +
        ", refreshToken='" + refreshToken + '\'' +
        ", grantType='" + grantType + '\'' +
        ", clientEmail='" + clientEmail + '\'' +
        ", privateKey='" + privateKey + '\'' +
        ", privateKeyId='" + privateKeyId + '\'' +
        ", projectId='" + projectId + '\'' +
        ", scopes=" + scopes +
        '}';
  }
}
