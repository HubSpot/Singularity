package com.hubspot.singularity.mesos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Singleton;

import org.apache.mesos.v1.Protos.Attribute;
import org.apache.mesos.v1.Protos.Offer;

import com.google.inject.Inject;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.helpers.MesosUtils;

@Singleton
public class SingularitySlaveAndRackHelper {

  private final String rackIdAttributeKey;
  private final String defaultRackId;

  private final SingularityConfiguration configuration;

  @Inject
  public SingularitySlaveAndRackHelper(SingularityConfiguration configuration) {
    this.configuration = configuration;

    MesosConfiguration mesosConfiguration = configuration.getMesosConfiguration();

    this.rackIdAttributeKey = mesosConfiguration.getRackIdAttributeKey();
    this.defaultRackId = mesosConfiguration.getDefaultRackId();

  }

  public String getMaybeTruncatedHost(String hostname) {
    if (configuration.getCommonHostnameSuffixToOmit().isPresent()) {
      if (hostname.endsWith(configuration.getCommonHostnameSuffixToOmit().get())) {
        hostname = hostname.substring(0, hostname.length() - configuration.getCommonHostnameSuffixToOmit().get().length());
      }
    }
    return hostname;
  }

  public String getMaybeTruncatedHost(Offer offer) {
    return getMaybeTruncatedHost(offer.getHostname());
  }

  public String getRackId(Map<String, String> attributes) {
    String rackId = attributes.get(rackIdAttributeKey);

    if (rackId != null) {
      return rackId;
    }

    return defaultRackId;
  }

  public Optional<String> getRackId(Offer offer) {
    for (Attribute attribute : offer.getAttributesList()) {
      if (attribute.getName().equals(rackIdAttributeKey)) {
        return Optional.of(attribute.getText().getValue());
      }
    }

    return Optional.empty();
  }

  public String getRackIdOrDefault(Offer offer) {
    return getRackId(offer).orElse(defaultRackId);
  }

  public Map<String, String> getTextAttributes(Map<String, String> attributes) {
    Map<String, String> textAttributes = new HashMap<>(attributes);
    if (textAttributes.containsKey(rackIdAttributeKey)) {
      textAttributes.remove(rackIdAttributeKey);
    }
    return textAttributes;
  }

  public Map<String, String> getTextAttributes(Offer offer) {
    Map<String, String> textAttributes = new HashMap<>();
    for (Attribute attribute : offer.getAttributesList()) {
      if (!attribute.getName().equals(rackIdAttributeKey)) {
        if (attribute.hasText()) {
          textAttributes.put(attribute.getName(), attribute.getText().getValue());
        } else if (attribute.hasScalar()) {
          textAttributes.put(attribute.getName(), Double.toString(attribute.getScalar().getValue()));
        } else if (attribute.hasRanges()) {
          textAttributes.put(attribute.getName(), attribute.getRanges().getRangeList().toString());
        }
      }
    }
    return textAttributes;
  }

  public Map<String, String> getReservedSlaveAttributes(Offer offer) {
    Map<String, String> reservedAttributes = new HashMap<>();
    Map<String, String> offerTextAttributes = getTextAttributes(offer);
    for (Map.Entry<String, List<String>> entry : configuration.getReserveSlavesWithAttributes().entrySet()) {
      for (String attr : entry.getValue()) {
        if (offerTextAttributes.containsKey(entry.getKey()) && offerTextAttributes.get(entry.getKey()).equals(attr)) {
          reservedAttributes.put(entry.getKey(), attr);
        }
      }
    }
    return reservedAttributes;
  }

  public boolean containsAllAttributes(Map<String, String> attributes, Map<String, String> otherAttributes) {
    for (Map.Entry<String, String> entry : otherAttributes.entrySet()) {
      if (!(attributes.containsKey(entry.getKey()) && attributes.get(entry.getKey()).equals(entry.getValue()))) {
        return false;
      }
    }
    return true;
  }

  public boolean containsAtLeastOneMatchingAttribute(Map<String, String> attributes, Map<String, String> otherAttributes) {
    for (Map.Entry<String, String> entry : otherAttributes.entrySet()) {
      if ((attributes.containsKey(entry.getKey()) && attributes.get(entry.getKey()).equals(entry.getValue()))) {
        return true;
      }
    }
    return false;
  }

  public enum CpuMemoryPreference {
    AVERAGE,
    HIGH_MEMORY,
    HIGH_CPU
  }

  public CpuMemoryPreference getCpuMemoryPreferenceForRequest(RequestUtilization requestUtilization) {
    double cpuMemoryRatioForRequest = getCpuMemoryRatioForRequest(requestUtilization);

    if (cpuMemoryRatioForRequest > configuration.getHighCpuSlaveCutOff()) {
      return CpuMemoryPreference.HIGH_CPU;
    } else if (cpuMemoryRatioForRequest < configuration.getHighMemorySlaveCutOff()) {
      return CpuMemoryPreference.HIGH_MEMORY;
    }
    return CpuMemoryPreference.AVERAGE;
  }

  public CpuMemoryPreference getCpuMemoryPreferenceForSlave(SingularityOfferHolder offerHolder) {
    double cpuMemoryRatioForSlave = getCpuMemoryRatioForSlave(offerHolder);
    if (cpuMemoryRatioForSlave > configuration.getHighCpuSlaveCutOff()) {
      return CpuMemoryPreference.HIGH_CPU;
    } else if (cpuMemoryRatioForSlave < configuration.getHighMemorySlaveCutOff()) {
      return CpuMemoryPreference.HIGH_MEMORY;
    }
    return CpuMemoryPreference.AVERAGE;
  }

  private double getCpuMemoryRatioForSlave(SingularityOfferHolder offerHolder) {
    double memoryGB = MesosUtils.getMemory(offerHolder.getCurrentResources(), Optional.empty()) / 1024;
    double cpus = MesosUtils.getNumCpus(offerHolder.getCurrentResources(), Optional.empty());
    return cpus/memoryGB;
  }

  private double getCpuMemoryRatioForRequest(RequestUtilization requestUtilization) {
    double cpuUsageForRequest = getEstimatedCpuUsageForRequest(requestUtilization);
    double memUsageGBForRequest = requestUtilization.getAvgMemBytesUsed() / 1024 / 1024;
    return cpuUsageForRequest/memUsageGBForRequest;
  }

  public double getEstimatedCpuUsageForRequest(RequestUtilization requestUtilization) {
    // To account for cpu bursts, tend towards max usage if the app is consistently over-utilizing cpu, tend towards avg if it is over-utilized in short bursts
    return (requestUtilization.getMaxCpuUsed() - requestUtilization.getAvgCpuUsed()) * requestUtilization.getCpuBurstRating() + requestUtilization.getAvgCpuUsed();
  }

}
