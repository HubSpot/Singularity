package com.hubspot.mesos.json;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosMasterSlaveObject {

  private final String id;
  private final String pid;
  private final String hostname;
  private final Map<String, String> attributes;
  private final long registeredTime;
  private final MesosResourcesObject resources;
  private final MesosResourcesObject usedResources;
  private final MesosResourcesObject offeredResources;
  private final MesosResourcesObject reservedResources;
  private final MesosResourcesObject unreservedResources;
  private final String version;
  private final boolean active;

  @JsonCreator
  public MesosMasterSlaveObject(@JsonProperty("id") String id, @JsonProperty("pid") String pid, @JsonProperty("hostname") String hostname, @JsonProperty("registered_time") long registeredTime,
      @JsonProperty("resources") MesosResourcesObject resources, @JsonProperty("attributes") Map<String, String> attributes, @JsonProperty("used_resources") MesosResourcesObject usedResources,
      @JsonProperty("offered_resources") MesosResourcesObject offeredResources, @JsonProperty("reserved_resources") MesosResourcesObject reservedResources, @JsonProperty("unreserved_resources") MesosResourcesObject unreservedResources,
      @JsonProperty("version") String version, @JsonProperty("active") boolean active) {
    this.id = id;
    this.pid = pid;
    this.hostname = hostname;
    this.registeredTime = registeredTime;
    this.resources = resources;
    this.attributes = attributes;
    this.usedResources = usedResources;
    this.offeredResources = offeredResources;
    this.reservedResources = reservedResources;
    this.unreservedResources = unreservedResources;
    this.version = version;
    this.active = active;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public String getId() {
    return id;
  }

  public String getPid() {
    return pid;
  }

  public String getHostname() {
    return hostname;
  }

  public long getRegisteredTime() {
    return registeredTime;
  }

  public MesosResourcesObject getResources() {
    return resources;
  }

  public MesosResourcesObject getUsedResources() {
    return usedResources;
  }

  public MesosResourcesObject getOfferedResources() {
    return offeredResources;
  }

  public MesosResourcesObject getReservedResources() {
    return reservedResources;
  }

  public MesosResourcesObject getUnreservedResources() {
    return unreservedResources;
  }

  public String getVersion() {
    return version;
  }

  public boolean isActive() {
    return active;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MesosMasterSlaveObject that = (MesosMasterSlaveObject) o;
    return registeredTime == that.registeredTime &&
        active == that.active &&
        Objects.equals(id, that.id) &&
        Objects.equals(pid, that.pid) &&
        Objects.equals(hostname, that.hostname) &&
        Objects.equals(attributes, that.attributes) &&
        Objects.equals(resources, that.resources) &&
        Objects.equals(usedResources, that.usedResources) &&
        Objects.equals(offeredResources, that.offeredResources) &&
        Objects.equals(reservedResources, that.reservedResources) &&
        Objects.equals(unreservedResources, that.unreservedResources) &&
        Objects.equals(version, that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, pid, hostname, attributes, registeredTime, resources, usedResources, offeredResources, reservedResources, unreservedResources, version, active);
  }

  @Override
  public String toString() {
    return "MesosMasterSlaveObject{" +
        "id='" + id + '\'' +
        ", pid='" + pid + '\'' +
        ", hostname='" + hostname + '\'' +
        ", attributes=" + attributes +
        ", registeredTime=" + registeredTime +
        ", resources=" + resources +
        ", usedResources=" + usedResources +
        ", offeredResources=" + offeredResources +
        ", reservedResources=" + reservedResources +
        ", unreservedResources=" + unreservedResources +
        ", version='" + version + '\'' +
        ", active=" + active +
        '}';
  }
}
