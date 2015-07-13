package com.hubspot.singularity.mesos;

import java.util.Map;

import javax.inject.Singleton;

import org.apache.mesos.Protos.Attribute;
import org.apache.mesos.Protos.Offer;

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

  public String getRackId(Offer offer) {
    for (Attribute attribute : offer.getAttributesList()) {
      if (attribute.getName().equals(rackIdAttributeKey)) {
        return attribute.getText().getValue();
      }
    }

    return defaultRackId;
  }

}
