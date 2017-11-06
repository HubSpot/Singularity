package com.hubspot.mesos;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.Attribute;
import org.apache.mesos.v1.Protos.ExecutorID;
import org.apache.mesos.v1.Protos.FrameworkID;
import org.apache.mesos.v1.Protos.Image;
import org.apache.mesos.v1.Protos.Image.Appc;
import org.apache.mesos.v1.Protos.Image.Docker;
import org.apache.mesos.v1.Protos.Label;
import org.apache.mesos.v1.Protos.Labels;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.apache.mesos.v1.Protos.Parameter;
import org.apache.mesos.v1.Protos.Resource;
import org.apache.mesos.v1.Protos.Resource.DiskInfo;
import org.apache.mesos.v1.Protos.Resource.DiskInfo.Persistence;
import org.apache.mesos.v1.Protos.Resource.ReservationInfo;
import org.apache.mesos.v1.Protos.TaskInfo;
import org.apache.mesos.v1.Protos.URL;
import org.apache.mesos.v1.Protos.Value.Range;
import org.apache.mesos.v1.Protos.Value.Ranges;
import org.apache.mesos.v1.Protos.Value.Set;
import org.apache.mesos.v1.Protos.Volume;
import org.apache.mesos.v1.Protos.Volume.Source;
import org.apache.mesos.v1.Protos.Volume.Source.DockerVolume;

import com.google.common.base.Optional;
import com.hubspot.mesos.protos.MesosAddress;
import com.hubspot.mesos.protos.MesosAppcImage;
import com.hubspot.mesos.protos.MesosAttributeObject;
import com.hubspot.mesos.protos.MesosAttributeType;
import com.hubspot.mesos.protos.MesosCredential;
import com.hubspot.mesos.protos.MesosDiskInfo;
import com.hubspot.mesos.protos.MesosDiskPersistence;
import com.hubspot.mesos.protos.MesosDiskSource;
import com.hubspot.mesos.protos.MesosDiskSourceType;
import com.hubspot.mesos.protos.MesosDockerImage;
import com.hubspot.mesos.protos.MesosDockerVolume;
import com.hubspot.mesos.protos.MesosDoubleValue;
import com.hubspot.mesos.protos.MesosImage;
import com.hubspot.mesos.protos.MesosImageType;
import com.hubspot.mesos.protos.MesosMount;
import com.hubspot.mesos.protos.MesosParameter;
import com.hubspot.mesos.protos.MesosOfferObject;
import com.hubspot.mesos.protos.MesosRange;
import com.hubspot.mesos.protos.MesosRanges;
import com.hubspot.mesos.protos.MesosReservationInfo;
import com.hubspot.mesos.protos.MesosResourceObject;
import com.hubspot.mesos.protos.MesosSandboxPath;
import com.hubspot.mesos.protos.MesosSandboxPathType;
import com.hubspot.mesos.protos.MesosSet;
import com.hubspot.mesos.protos.MesosStringValue;
import com.hubspot.mesos.protos.MesosTaskObject;
import com.hubspot.mesos.protos.MesosURL;
import com.hubspot.mesos.protos.MesosVolume;
import com.hubspot.mesos.protos.MesosVolumeMode;
import com.hubspot.mesos.protos.MesosVolumeSource;
import com.hubspot.mesos.protos.MesosVolumeSourceType;

public class MesosProtosUtils {
  public static MesosTaskObject taskFromProtos(TaskInfo taskInfo) {
    return new MesosTaskObject(
        new MesosStringValue(taskInfo.getTaskId().getValue()),
        taskInfo.hasExecutor() ? Optional.of(taskInfo.getExecutor()) : Optional.absent(),
        taskInfo.hasLabels() ? Optional.of(taskInfo.getLabels()) : Optional.absent(),
        new MesosStringValue(taskInfo.getAgentId().getValue()),
        null,
        taskInfo.getResourcesList(),
        taskInfo.hasCommand() ? Optional.of(taskInfo.getCommand()) : Optional.absent(),
        taskInfo.hasContainer() ? Optional.of(taskInfo.getContainer()) : Optional.absent(),
        taskInfo.hasDiscovery() ? Optional.of(taskInfo.getDiscovery()) : Optional.absent(),
        taskInfo.hasHealthCheck() ? Optional.of(taskInfo.getHealthCheck()) : Optional.absent(),
        taskInfo.hasKillPolicy() ? Optional.of(taskInfo.getKillPolicy()) : Optional.absent(),
        taskInfo.getName()
    );
  }

  public static MesosOfferObject offerFromProtos(Offer offer) {
    return new MesosOfferObject(
        attributesFromProtos(offer.getAttributesList()),
        executorIdsFromProtos(offer.getExecutorIdsList()),
        urlFromProtos(offer.getUrl()),
        agentIdFromProtos(offer.getAgentId()),
        null,
        frameworkIdFromProtos(offer.getFrameworkId()),
        offer.getHostname(),
        resourcesFromProtos(offer.getResourcesList()),
        offerIdFromProtos(offer.getId())
    );
  }

  public static List<MesosStringValue> executorIdsFromProtos(List<ExecutorID> executorIDSProtos) {
    List<MesosStringValue> executorIds = new ArrayList<>();
    for (ExecutorID executorID : executorIDSProtos) {
      if (executorID.hasValue()) {
        executorIds.add(new MesosStringValue(executorID.getValue()));
      }
    }
    return executorIds;
  }

  public static MesosURL urlFromProtos(URL url) {
    return new MesosURL(
        url.hasScheme() ? Optional.of(url.getScheme()) : Optional.absent(),
        url.hasPath() ? Optional.of(url.getPath()) : Optional.absent(),
        url.hasAddress() ?
            Optional.of(new MesosAddress(
                url.getAddress().hasHostname() ? Optional.of(url.getAddress().getHostname()) : Optional.absent(),
                url.getAddress().hasIp() ? Optional.of(url.getAddress().getIp()) : Optional.absent(),
                url.getAddress().hasPort() ? Optional.of(url.getAddress().getPort()) : Optional.absent()
            )):
            Optional.absent(),
        url.hasFragment() ? Optional.of(url.getFragment()) : Optional.absent(),
        url.getQueryList().stream().map((p) -> parameterFromProtos(p)).collect(Collectors.toList())
    );
  }

  public static MesosParameter parameterFromProtos(Parameter parameterProtos) {
    return new MesosParameter(
        parameterProtos.hasKey() ? Optional.of(parameterProtos.getKey()) : Optional.absent(),
        parameterProtos.hasValue() ? Optional.of(parameterProtos.getValue()) : Optional.absent()
    );
  }

  public static MesosStringValue agentIdFromProtos(AgentID agentID) {
    return new MesosStringValue(agentID.getValue());
  }

  public static MesosStringValue frameworkIdFromProtos(FrameworkID frameworkID) {
    return new MesosStringValue(frameworkID.getValue());
  }

  public static MesosStringValue offerIdFromProtos(OfferID offerID) {
    return new MesosStringValue(offerID.getValue());
  }

  public static List<MesosResourceObject> resourcesFromProtos(List<Resource> resourcesProtos) {
    return resourcesProtos.stream()
        .map((resource) ->
          new MesosResourceObject(
              resource.hasName() ? Optional.of(resource.getName()) : Optional.absent(),
              resource.hasDisk() ? Optional.of(diskInfoFromProtos(resource.getDisk())) : Optional.absent(),
              resource.hasScalar() && resource.getScalar().hasValue() ? Optional.of(new MesosDoubleValue(Optional.of(resource.getScalar().getValue()))) : Optional.absent(),
              resource.hasRanges() ? Optional.of(rangesFromProtos(resource.getRanges())) : Optional.absent(),
              resource.hasSet() ? Optional.of(setFromProtos(resource.getSet())) : Optional.absent(),
              resource.hasReservation() ? Optional.of(reservationInfoFromProtos(resource.getReservation())) : Optional.absent(),
              Optional.of(resource.hasRevocable()),
              resource.hasRole() ? Optional.of(resource.getRole()) : Optional.absent(),
              Optional.of(resource.hasShared()),
              resource.hasType() ? Optional.of(MesosAttributeType.valueOf(resource.getType().name())) : Optional.absent()
          )
        )
        .collect(Collectors.toList());
  }

  public static MesosDiskInfo diskInfoFromProtos(DiskInfo diskInfoProtos) {
    return new MesosDiskInfo(
        diskInfoProtos.hasSource() ? Optional.of(diskSourceFromProtos(diskInfoProtos.getSource())) : Optional.absent(),
        diskInfoProtos.hasVolume() ? Optional.of(volumeFromProtos(diskInfoProtos.getVolume())) : Optional.absent(),
        diskInfoProtos.hasPersistence() ? Optional.of(diskPersistenceFromProtos(diskInfoProtos.getPersistence())) : Optional.absent()
    );
  }

  public static MesosDiskPersistence diskPersistenceFromProtos(Persistence persistenceProtos) {
    return new MesosDiskPersistence(
        persistenceProtos.hasId() ? Optional.of(persistenceProtos.getId()) : Optional.absent(),
        persistenceProtos.hasPrincipal() ? Optional.of(persistenceProtos.getPrincipal()) : Optional.absent()
    );
  }

  public static MesosDiskSource diskSourceFromProtos(DiskInfo.Source sourceProtos) {
    return new MesosDiskSource(
        sourceProtos.hasMount() ?
            Optional.of(new MesosMount(sourceProtos.getMount().hasRoot() ? Optional.of(sourceProtos.getMount().getRoot()) : Optional.absent())) :
            Optional.absent(),
        sourceProtos.hasPath() ?
            Optional.of(new MesosMount(sourceProtos.getPath().hasRoot() ? Optional.of(sourceProtos.getPath().getRoot()) : Optional.absent())) :
            Optional.absent(),
        sourceProtos.hasType() ? Optional.of(MesosDiskSourceType.valueOf(sourceProtos.getType().name())) : Optional.absent()
    );
  }

  public static MesosVolume volumeFromProtos(Volume volumeProtos) {
    return new MesosVolume(
        volumeProtos.hasContainerPath() ? Optional.of(volumeProtos.getContainerPath()) : Optional.absent(),
        volumeProtos.hasHostPath() ? Optional.of(volumeProtos.getHostPath()) : Optional.absent(),
        volumeProtos.hasMode() ? Optional.of(MesosVolumeMode.valueOf(volumeProtos.getMode().name())) : Optional.absent(),
        volumeProtos.hasSource() ? Optional.of(volumeSourceFromProtos(volumeProtos.getSource())) : Optional.absent(),
        volumeProtos.hasImage() ? Optional.of(imageFromProtos(volumeProtos.getImage())) : Optional.absent()
    );
  }

  public static MesosVolumeSource volumeSourceFromProtos(Source sourceProtos) {
    return new MesosVolumeSource(
        sourceProtos.hasType() ? Optional.of(MesosVolumeSourceType.valueOf(sourceProtos.getType().name())) : Optional.absent(),
        sourceProtos.hasSandboxPath() ? Optional.of(new MesosSandboxPath(
            sourceProtos.getSandboxPath().hasPath() ? Optional.of(sourceProtos.getSandboxPath().getPath()) : Optional.absent(),
            sourceProtos.getSandboxPath().hasType() ? Optional.of(MesosSandboxPathType.valueOf(sourceProtos.getSandboxPath().getType().name())) : Optional.absent()
        )) : Optional.absent(),
        sourceProtos.hasDockerVolume() ? Optional.of(dockerVolumeFromProtos(sourceProtos.getDockerVolume())) : Optional.absent()
    );
  }

  public static MesosDockerVolume dockerVolumeFromProtos(DockerVolume dockerVolumeProtos) {
    return new MesosDockerVolume(
        dockerVolumeProtos.hasDriver() ? Optional.of(dockerVolumeProtos.getDriver()) : Optional.absent(),
        dockerVolumeProtos.hasName() ? Optional.of(dockerVolumeProtos.getName()) : Optional.absent(),
        dockerVolumeProtos.hasDriverOptions() ?
            Optional.of(dockerVolumeProtos.getDriverOptions().getParameterList().stream().map((p) -> parameterFromProtos(p)).collect(Collectors.toList())) :
            Optional.absent()
    );
  }

  public static MesosImage imageFromProtos(Image imageProtos) {
    return new MesosImage(
        imageProtos.hasType() ? Optional.of(MesosImageType.valueOf(imageProtos.getType().name())) : Optional.absent(),
        imageProtos.hasCached() ? Optional.of(imageProtos.getCached()) : Optional.absent(),
        imageProtos.hasDocker() ? Optional.of(dockerImageFromProtos(imageProtos.getDocker())) : Optional.absent(),
        imageProtos.hasAppc() ? Optional.of(appcImageFromProtos(imageProtos.getAppc())) : Optional.absent()
    );
  }

  public static MesosDockerImage dockerImageFromProtos(Docker dockerProtos) {
    return new MesosDockerImage(
        dockerProtos.hasName() ? Optional.of(dockerProtos.getName()) : Optional.absent(),
        dockerProtos.hasCredential() ?
            Optional.of(new MesosCredential(
                dockerProtos.getCredential().hasPrincipal() ? Optional.of(dockerProtos.getCredential().getPrincipal()) : Optional.absent(),
                dockerProtos.getCredential().hasSecret() ? Optional.of(dockerProtos.getCredential().getSecret()) : Optional.absent()
            )) :
            Optional.absent()
    );
  }

  public static MesosAppcImage appcImageFromProtos(Appc appcProtos) {
    return new MesosAppcImage(
        appcProtos.hasId() ? Optional.of(appcProtos.getId()) : Optional.absent(),
        appcProtos.hasName() ? Optional.of(appcProtos.getName()) : Optional.absent(),
        appcProtos.hasLabels() ? Optional.of(parametersFromLabelsProtos(appcProtos.getLabels())) : Optional.absent()
    );
  }

  public static List<MesosParameter> parametersFromLabelsProtos(Labels labels) {
    List<MesosParameter> parameters = new ArrayList<>();
    for (Label label : labels.getLabelsList()) {
      parameters.add(new MesosParameter(
          label.hasKey() ? Optional.of(label.getKey()) : Optional.absent(),
          label.hasValue() ? Optional.of(label.getValue()) : Optional.absent()
      ));
    }
    return parameters;
  }

  public static MesosReservationInfo reservationInfoFromProtos(ReservationInfo reservationInfoProtos) {
    List<MesosParameter> parameters = new ArrayList<>();
    if (reservationInfoProtos.hasLabels()) {
      reservationInfoProtos.getLabels().getLabelsList().forEach((label) -> {
        parameters.add(
            new MesosParameter(
                label.hasKey() ? Optional.of(label.getKey()) : Optional.absent(),
                label.hasValue() ? Optional.of(label.getValue()) : Optional.absent()
            )
        );
      });
    }
    return new MesosReservationInfo(Optional.of(parameters));
  }

  public static List<MesosAttributeObject> attributesFromProtos(List<Attribute> protosAttributes) {
    List<MesosAttributeObject> attributes = new ArrayList<>();
    for (Attribute attribute : protosAttributes) {
      attributes.add(new MesosAttributeObject(
          attribute.hasName() ? Optional.of(attribute.getName()) : Optional.absent(),
          attribute.hasType() ? Optional.of(MesosAttributeType.valueOf(attribute.getType().name())) : Optional.absent(),
          attribute.hasText() && attribute.getText().hasValue() ?
              Optional.of(new MesosStringValue(attribute.getText().getValue())) :
              Optional.absent(),
          attribute.hasScalar() && attribute.getScalar().hasValue() ?
              Optional.of(new MesosDoubleValue(Optional.of(attribute.getScalar().getValue()))) :
              Optional.absent(),
          attribute.hasRanges() ? Optional.of(rangesFromProtos(attribute.getRanges())) : Optional.absent(),
          attribute.hasSet() ? Optional.of(setFromProtos(attribute.getSet())) : Optional.absent()
      ));
    }
    return attributes;
  }

  public static MesosRanges rangesFromProtos(Ranges rangesProtos) {
    List<MesosRange> rangesList = new ArrayList<>();
    for (Range range : rangesProtos.getRangeList()) {
      rangesList.add(new MesosRange(
          range.hasBegin() ? Optional.of(range.getBegin()) : Optional.absent(),
          range.hasEnd() ? Optional.of(range.getEnd()) : Optional.absent()
      ));
    }
    return new MesosRanges(rangesList);
  }

  public static MesosSet setFromProtos(Set set) {
    return new MesosSet(set.getItemList());
  }
}
