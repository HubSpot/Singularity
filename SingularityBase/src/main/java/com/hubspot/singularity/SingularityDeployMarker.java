package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SingularityDeployMarker extends SingularityJsonObject {

  private final String requestId;
  
  private final String deployId;

  private final long timestamp;
  private final Optional<String> user;
  
  @JsonCreator
  public SingularityDeployMarker(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("timestamp") long timestamp, @JsonProperty("user") Optional<String> user) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.timestamp = timestamp;
    this.user = user;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((deployId == null) ? 0 : deployId.hashCode());
    result = prime * result + ((requestId == null) ? 0 : requestId.hashCode());
    result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
    result = prime * result + ((user == null) ? 0 : user.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SingularityDeployMarker other = (SingularityDeployMarker) obj;
    if (deployId == null) {
      if (other.deployId != null)
        return false;
    } else if (!deployId.equals(other.deployId))
      return false;
    if (requestId == null) {
      if (other.requestId != null)
        return false;
    } else if (!requestId.equals(other.requestId))
      return false;
    if (timestamp != other.timestamp)
      return false;
    if (user == null) {
      if (other.user != null)
        return false;
    } else if (!user.equals(other.user))
      return false;
    return true;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getDeployId() {
    return deployId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Optional<String> getUser() {
    return user;
  }
  
  public static SingularityDeployMarker fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityDeployMarker.class);
  }

}
