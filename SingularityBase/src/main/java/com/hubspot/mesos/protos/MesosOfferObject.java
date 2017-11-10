package com.hubspot.mesos.protos;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * Mirrors the mesos Offer object, with the exception that slaveId can be read into agentId
 */
public class MesosOfferObject {
  private final MesosStringValue agentId;
  private final MesosStringValue slaveId;
  private final MesosStringValue frameworkId;
  private final String hostname;
  private final MesosStringValue id;
  private final Map<String, Object> allOtherFields;

  @JsonCreator
  public MesosOfferObject(@JsonProperty("agentId") MesosStringValue agentId,
                          @JsonProperty("slaveId") MesosStringValue slaveId,
                          @JsonProperty("frameworkId") MesosStringValue frameworkId,
                          @JsonProperty("hostname") String hostname,
                          @JsonProperty("id") MesosStringValue id) {
    this.agentId = agentId != null ? agentId : slaveId;
    this.slaveId = agentId != null ? agentId : slaveId;
    this.frameworkId = frameworkId;
    this.hostname = hostname;
    this.id = id;
    this.allOtherFields = new HashMap<>();
  }

  @JsonIgnore
  public MesosOfferObject sizeOptimized() {
    MesosOfferObject optimized = new MesosOfferObject(agentId, null, frameworkId, hostname, id);
    optimized.setAllOtherFields("url", allOtherFields.get("url"));
    return optimized;
  }

  public MesosStringValue getAgentId() {
    return agentId;
  }

  public MesosStringValue getSlaveId() {
    return slaveId;
  }

  public MesosStringValue getFrameworkId() {
    return frameworkId;
  }

  public String getHostname() {
    return hostname;
  }

  public MesosStringValue getId() {
    return id;
  }

  // Unknown fields
  @JsonAnyGetter
  public Map<String, Object> getAllOtherFields() {
    return allOtherFields;
  }

  @JsonAnySetter
  public void setAllOtherFields(String name, Object value) {
    allOtherFields.put(name, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosOfferObject) {
      final MesosOfferObject that = (MesosOfferObject) obj;
      return Objects.equals(this.agentId, that.agentId) &&
          Objects.equals(this.slaveId, that.slaveId) &&
          Objects.equals(this.frameworkId, that.frameworkId) &&
          Objects.equals(this.hostname, that.hostname) &&
          Objects.equals(this.id, that.id) &&
          Objects.equals(this.allOtherFields, that.allOtherFields);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(agentId, slaveId, frameworkId, hostname, id, allOtherFields);
  }

  @Override
  public String toString() {
    return "MesosOfferObject{" +
        "agentId=" + agentId +
        ", slaveId=" + slaveId +
        ", frameworkId=" + frameworkId +
        ", hostname='" + hostname + '\'' +
        ", id=" + id +
        ", allOtherFields=" + allOtherFields +
        '}';
  }
}
