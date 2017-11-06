package com.hubspot.mesos.protos;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * Mirrors the mesos Offer object, with the exception that slaveId can be read into agentId
 */
public class MesosOfferObject {
  private final List<MesosAttributeObject> attributes;
  private final List<MesosStringValue> executorIds;
  private final MesosURL url;
  private final MesosStringValue agentId;
  private final MesosStringValue slaveId;
  private final MesosStringValue frameworkId;
  private final String hostname;
  private final List<MesosResourceObject> resources;
  private final MesosStringValue id;

  @JsonCreator
  public MesosOfferObject(@JsonProperty("attributes") List<MesosAttributeObject> attributes,
                          @JsonProperty("executorIds") List<MesosStringValue> executorIds,
                          @JsonProperty("url") MesosURL url,
                          @JsonProperty("agentId") MesosStringValue agentId,
                          @JsonProperty("slaveId") MesosStringValue slaveId,
                          @JsonProperty("frameworkId") MesosStringValue frameworkId,
                          @JsonProperty("hostname") String hostname,
                          @JsonProperty("resources") List<MesosResourceObject> resources,
                          @JsonProperty("id") MesosStringValue id) {
    this.attributes = attributes;
    this.executorIds = executorIds;
    this.url = url;
    this.agentId = agentId != null ? agentId : slaveId;
    this.slaveId = agentId != null ? agentId : slaveId;
    this.frameworkId = frameworkId;
    this.hostname = hostname;
    this.resources = resources;
    this.id = id;
  }

  public MesosOfferObject sizeOptimized() {
    return new MesosOfferObject(attributes, Collections.emptyList(), url, agentId, null, frameworkId, hostname, Collections.emptyList(), id);
  }

  public List<MesosAttributeObject> getAttributes() {
    return attributes;
  }

  public List<MesosStringValue> getExecutorIds() {
    return executorIds;
  }

  public MesosURL getUrl() {
    return url;
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

  public List<MesosResourceObject> getResources() {
    return resources;
  }

  public MesosStringValue getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MesosOfferObject that = (MesosOfferObject) o;

    if (attributes != null ? !attributes.equals(that.attributes) : that.attributes != null) {
      return false;
    }
    if (executorIds != null ? !executorIds.equals(that.executorIds) : that.executorIds != null) {
      return false;
    }
    if (url != null ? !url.equals(that.url) : that.url != null) {
      return false;
    }
    if (agentId != null ? !agentId.equals(that.agentId) : that.agentId != null) {
      return false;
    }
    if (frameworkId != null ? !frameworkId.equals(that.frameworkId) : that.frameworkId != null) {
      return false;
    }
    if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null) {
      return false;
    }
    if (resources != null ? !resources.equals(that.resources) : that.resources != null) {
      return false;
    }
    return id != null ? id.equals(that.id) : that.id == null;
  }

  @Override
  public int hashCode() {
    int result = attributes != null ? attributes.hashCode() : 0;
    result = 31 * result + (executorIds != null ? executorIds.hashCode() : 0);
    result = 31 * result + (url != null ? url.hashCode() : 0);
    result = 31 * result + (agentId != null ? agentId.hashCode() : 0);
    result = 31 * result + (frameworkId != null ? frameworkId.hashCode() : 0);
    result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
    result = 31 * result + (resources != null ? resources.hashCode() : 0);
    result = 31 * result + (id != null ? id.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "SingularityMesosOfferObject{" +
        "attributes=" + attributes +
        ", executorIds=" + executorIds +
        ", url=" + url +
        ", agentId=" + agentId +
        ", frameworkId=" + frameworkId +
        ", hostname='" + hostname + '\'' +
        ", resources=" + resources +
        ", id=" + id +
        '}';
  }
}
