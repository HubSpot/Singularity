package com.hubspot.singularity.proxy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequestGroup;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.client.SingularityClient;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.hubspot.singularity.config.DataCenter;
import com.hubspot.singularity.exceptions.DataCenterNotFoundException;

import io.dropwizard.lifecycle.Managed;

public class DataCenterLocator implements Managed {
  private static final Logger LOG = LoggerFactory.getLogger(DataCenterLocator.class);

  private final ClusterCoordinatorConfiguration configuration;
  private final Map<String, DataCenter> dataCenters;
  private final Map<String, SingularityClient> clients;

  private final Random random = new Random();

  private final Map<String, Set<String>> requestIdsByDataCenter = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> requestGroupsByDataCenter = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> slaveIdsByDataCenter = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> hostnamesByDataCenter = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> rackIdsByDataCenter = new ConcurrentHashMap<>();


  @Inject
  public DataCenterLocator(ClusterCoordinatorConfiguration configuration, Map<String, SingularityClient> clients) {
    this.configuration = configuration;
    this.clients = clients;

    ImmutableMap.Builder<String, DataCenter> builder = ImmutableMap.builder();
    configuration.getDataCenters().forEach((dc) -> builder.put(dc.getName(), dc));
    this.dataCenters = builder.build();
  }

  String getHost(DataCenter dataCenter) {
    return dataCenter.getHosts().get(random.nextInt(dataCenter.getHosts().size()));
  }

  DataCenter getDataCenterForRequest(String requestId, boolean isGetRequest) {
    for (Map.Entry<String, DataCenter> entry : dataCenters.entrySet()) {
      if (requestIdsByDataCenter.get(entry.getKey()).contains(requestId)) {
        return entry.getValue();
      }
    }

    // Not found, try each one
    for (Map.Entry<String, DataCenter> entry : dataCenters.entrySet()) {
      SingularityClient client = clients.get(entry.getKey());
      Optional<SingularityRequestParent> maybeRequest = client.getSingularityRequest(requestId);
      if (maybeRequest.isPresent()) {
        requestIdsByDataCenter.get(entry.getKey()).add(maybeRequest.get().getRequest().getId());
        return entry.getValue();
      }
    }
    throw new DataCenterNotFoundException(String.format("Could not find requestId '%s' in any data center", requestId), isGetRequest ? 404 :500);
  }

  Optional<DataCenter> getMaybeDataCenterForRequest(String requestId, boolean isGetRequest) {
    try {
      return Optional.of(getDataCenterForRequest(requestId, isGetRequest));
    } catch (DataCenterNotFoundException nfe) {
      return Optional.absent();
    }
  }

  DataCenter getDataCenterForRequestGroup(String requestGroupId, boolean isGetRequest) {
    for (Map.Entry<String, DataCenter> entry : dataCenters.entrySet()) {
      if (requestGroupsByDataCenter.get(entry.getKey()).contains(requestGroupId)) {
        return entry.getValue();
      }
    }

    // Not found, try each one
    for (Map.Entry<String, DataCenter> entry : dataCenters.entrySet()) {
      SingularityClient client = clients.get(entry.getKey());
      Optional<SingularityRequestGroup> maybeRequestGroup = client.getRequestGroup(requestGroupId);
      if (maybeRequestGroup.isPresent()) {
        requestGroupsByDataCenter.get(entry.getKey()).add(maybeRequestGroup.get().getId());
        return entry.getValue();
      }
    }
    throw new DataCenterNotFoundException(String.format("Could not find requestGroupId '%s' in any data center", requestGroupId), isGetRequest ? 404 :500);
  }

   DataCenter getDataCenterForSlaveId(String slaveId, boolean isGetRequest) {
     for (Map.Entry<String, DataCenter> entry : dataCenters.entrySet()) {
       if (slaveIdsByDataCenter.get(entry.getKey()).contains(slaveId)) {
         return entry.getValue();
       }
     }

     // Not found, try each one
     for (Map.Entry<String, DataCenter> entry : dataCenters.entrySet()) {
       SingularityClient client = clients.get(entry.getKey());
       Optional<SingularitySlave> maybeSlave = client.getSlave(slaveId);
       if (maybeSlave.isPresent()) {
         slaveIdsByDataCenter.get(entry.getKey()).add(maybeSlave.get().getId());
         hostnamesByDataCenter.get(entry.getKey()).add(maybeSlave.get().getHost());
         rackIdsByDataCenter.get(entry.getKey()).add(maybeSlave.get().getRackId());
         return entry.getValue();
       }
     }
     throw new DataCenterNotFoundException(String.format("Could not find slaveId '%s' in any data center", slaveId), isGetRequest ? 404 :500);
  }

  DataCenter getDataCenterForSlaveHostname(String hostname, boolean isGetRequest) {
    for (Map.Entry<String, DataCenter> entry : dataCenters.entrySet()) {
      if (hostnamesByDataCenter.get(entry.getKey()).contains(hostname)) {
        return entry.getValue();
      }
    }

    // Not found, try each one
    for (Map.Entry<String, DataCenter> entry : dataCenters.entrySet()) {
      SingularityClient client = clients.get(entry.getKey());
      Collection<SingularitySlave> slaves = client.getSlaves(Optional.absent());
      for (SingularitySlave slave : slaves) {
        if (slave.getHost().equals(hostname)) {
          slaveIdsByDataCenter.get(entry.getKey()).add(slave.getId());
          hostnamesByDataCenter.get(entry.getKey()).add(slave.getHost());
          rackIdsByDataCenter.get(entry.getKey()).add(slave.getRackId());
          return entry.getValue();
        }
      }
    }
    throw new DataCenterNotFoundException(String.format("Could not find slave with hostname '%s' in any data center", hostname), isGetRequest ? 404 :500);
  }

  DataCenter getDataCenterForRackId(String rackId, boolean isGetRequest) {
    for (Map.Entry<String, DataCenter> entry : dataCenters.entrySet()) {
      if (rackIdsByDataCenter.get(entry.getKey()).contains(rackId)) {
        return entry.getValue();
      }
    }

    // Not found, try each one
    for (Map.Entry<String, DataCenter> entry : dataCenters.entrySet()) {
      SingularityClient client = clients.get(entry.getKey());
      Collection<SingularitySlave> slaves = client.getSlaves(Optional.absent());
      for (SingularitySlave slave : slaves) {
        if (slave.getRackId().equals(rackId)) {
          slaveIdsByDataCenter.get(entry.getKey()).add(slave.getId());
          hostnamesByDataCenter.get(entry.getKey()).add(slave.getHost());
          rackIdsByDataCenter.get(entry.getKey()).add(slave.getRackId());
          return entry.getValue();
        }
      }
    }
    throw new DataCenterNotFoundException(String.format("Could not find rack with id '%s' in any data center", rackId), isGetRequest ? 404 :500);
  }

  DataCenter getDataCenter(String name) {
    if (dataCenters.containsKey(name)) {
      return dataCenters.get(name);
    } else {
      throw new DataCenterNotFoundException(String.format("No known data center with name: %s", name), 404);
    }
  }

  @Override
  public void start() {
    loadData();
  }

  private void loadData() {
    configuration.getDataCenters().forEach((dc) -> {
      SingularityClient singularityClient = clients.get(dc.getName());

      Collection<SingularityRequestParent> requestParents = singularityClient.getSingularityRequests();
      requestIdsByDataCenter.put(dc.getName(), requestParents.stream().map((r) -> r.getRequest().getId()).collect(Collectors.toSet()));
      LOG.info("Loaded {} requests for data center {}", requestParents.size(), dc.getName());

      Collection<SingularitySlave> slaves = singularityClient.getSlaves(Optional.absent());
      Set<String> rackIds = slaves.stream().map(SingularitySlave::getRackId).collect(Collectors.toSet());
      rackIdsByDataCenter.put(dc.getName(), new HashSet<>(rackIds));
      slaveIdsByDataCenter.put(dc.getName(), slaves.stream().map(SingularitySlave::getId).collect(Collectors.toSet()));
      hostnamesByDataCenter.put(dc.getName(), slaves.stream().map(SingularitySlave::getHost).collect(Collectors.toSet()));
      LOG.info("Loaded {} slaves for data center {}", slaves.size(), dc.getName());

      Collection<SingularityRequestGroup> requestGroups = singularityClient.getRequestGroups();
      requestGroupsByDataCenter.put(dc.getName(), requestGroups.stream().map(SingularityRequestGroup::getId).collect(Collectors.toSet()));
      LOG.info("Loaded {} request groups for data center {}", requestGroups.size(), dc.getName());
    });
  }

  @Override
  public void stop() {}
}
