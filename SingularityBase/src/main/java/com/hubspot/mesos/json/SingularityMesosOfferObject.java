package com.hubspot.mesos.json;

import java.util.Collections;
import java.util.List;

import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.Attribute;
import org.apache.mesos.v1.Protos.ExecutorID;
import org.apache.mesos.v1.Protos.FrameworkID;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.apache.mesos.v1.Protos.Resource;
import org.apache.mesos.v1.Protos.URL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * Mirrors the mesos Offer object, with the exception that slaveId can be read into agentId
 */
public class SingularityMesosOfferObject {
  private final List<Attribute> attributes;
  private final List<ExecutorID> executorIds;
  private final URL url;
  private final AgentID agentId;
  private final AgentID slaveId;
  private final FrameworkID frameworkId;
  private final String hostname;
  private final List<Resource> resources;
  private final OfferID id;

  public static final SingularityMesosOfferObject fromProtos(Offer offer) {
    return new SingularityMesosOfferObject(
        offer.getAttributesList(),
        offer.getExecutorIdsList(),
        offer.getUrl(),
        offer.getAgentId(),
        null,
        offer.getFrameworkId(),
        offer.getHostname(),
        offer.getResourcesList(),
        offer.getId()
    );
  }

  @JsonCreator
  public SingularityMesosOfferObject(@JsonProperty("attributes") List<Attribute> attributes,
                                     @JsonProperty("executorIds") List<ExecutorID> executorIds,
                                     @JsonProperty("url") URL url,
                                     @JsonProperty("agentId") AgentID agentId,
                                     @JsonProperty("slaveId") AgentID slaveId,
                                     @JsonProperty("frameworkId") FrameworkID frameworkId,
                                     @JsonProperty("hostname") String hostname,
                                     @JsonProperty("resources") List<Resource> resources,
                                     @JsonProperty("id") OfferID id) {
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

  public SingularityMesosOfferObject sizeOptimized() {
    return new SingularityMesosOfferObject(attributes, Collections.emptyList(), url, agentId, null, frameworkId, hostname, Collections.emptyList(), id);
  }

  public List<Attribute> getAttributes() {
    return attributes;
  }

  public List<ExecutorID> getExecutorIds() {
    return executorIds;
  }

  public URL getUrl() {
    return url;
  }

  public AgentID getAgentId() {
    return agentId;
  }

  public AgentID getSlaveId() {
    return slaveId;
  }

  public FrameworkID getFrameworkId() {
    return frameworkId;
  }

  public String getHostname() {
    return hostname;
  }

  public List<Resource> getResources() {
    return resources;
  }

  public OfferID getId() {
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

    SingularityMesosOfferObject that = (SingularityMesosOfferObject) o;

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
