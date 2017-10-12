package com.hubspot.singularity.mesos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.apache.mesos.v1.Protos.Attribute;
import org.apache.mesos.v1.Protos.Offer;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;

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

    return Optional.absent();
  }

  public String getRackIdOrDefault(Offer offer) {
    return getRackId(offer).or(defaultRackId);
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

  public boolean hasRequiredAttributes(Map<String, String> attributes, Map<String, String> requiredAttributes) {
    for (Map.Entry<String, String> entry : requiredAttributes.entrySet()) {
      if (!(attributes.containsKey(entry.getKey()) && attributes.get(entry.getKey()).equals(entry.getValue()))) {
        return false;
      }
    }
    return true;
  }

}
