package com.hubspot.singularity.data;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.data.Stat;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityEmailType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.StringTranscoder;

@Singleton
public class MetadataManager extends CuratorManager {

  private static final String ROOT_PATH = "/metadata";
  private static final String ZK_DATA_VERSION_PATH = ZKPaths.makePath(ROOT_PATH, "ZK_DATA_VERSION");
  private static final String MAIL_HISTORY_PATH = ROOT_PATH + "/mails";
  private static final String MAIL_HISTORY_RECORDS_KEY = "timestamps";
  private static final String MAIL_IN_COOLDOWN_MARKER_KEY = "COOLDOWN_ACTIVE";

  @Inject
  public MetadataManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry) {
    super(curator, configuration, metricRegistry);
  }

  private String getMailRecordPathForRequest(String requestId) {
    return ZKPaths.makePath(MAIL_HISTORY_PATH, requestId);
  }

  private String getMailRecordPathForRequestAndType(String requestId, String emailType) {
    return ZKPaths.makePath(getMailRecordPathForRequest(requestId), emailType);
  }

  private String getMailRecordTimestampPath(String requestId, String emailType) {
    return ZKPaths.makePath(getMailRecordPathForRequestAndType(requestId, emailType), MAIL_HISTORY_RECORDS_KEY);
  }

  private String getMailRecordCooldownPath(String requestId, String emailType) {
    return ZKPaths.makePath(getMailRecordPathForRequestAndType(requestId, emailType), MAIL_IN_COOLDOWN_MARKER_KEY);
  }

  private String getMailRecordPathForRequestAndTypeAndTime(String requestId, String emailType, String mailRecordTimestamp) {
    return ZKPaths.makePath(getMailRecordTimestampPath(requestId, emailType), mailRecordTimestamp);
  }

  public Optional<String> getZkDataVersion() {
    return getStringData(ZK_DATA_VERSION_PATH);
  }

  public void setZkDataVersion(String newVersion) {
    save(ZK_DATA_VERSION_PATH, Optional.of(newVersion.getBytes(UTF_8)));
  }

  public void saveMailRecord(SingularityRequest request, SingularityEmailType emailType) {
    create(getMailRecordPathForRequestAndTypeAndTime(request.getId(), emailType.name(), Long.toString(System.currentTimeMillis())));
  }

  public List<String> getMailRecords(String requestId, String emailType) {
    return getChildren(getMailRecordTimestampPath(requestId, emailType));
  }

  public List<String> getRequestsWithMailRecords() {
    return getChildren(MAIL_HISTORY_PATH);
  }

  public List<String> getEmailTypesWithMailRecords(String requestId) {
    return getChildren(getMailRecordPathForRequest(requestId));
  }

  public Optional<String> getMailCooldownMarker(String requestId, String emailType) {
    return getData(getMailRecordCooldownPath(requestId, emailType), StringTranscoder.INSTANCE);
  }

  public void cooldownMail(String requestId, String emailType) {
    create(getMailRecordCooldownPath(requestId, emailType), Long.toString(System.currentTimeMillis()), StringTranscoder.INSTANCE);
  }

  public void removeMailCooldown(String requestId, String emailType) {
    delete(getMailRecordCooldownPath(requestId, emailType));
  }

  public void deleteMailRecord(String requestId, String emailType, String mailRecordTimestamp) {
    delete(getMailRecordPathForRequestAndTypeAndTime(requestId, emailType, mailRecordTimestamp));
  }

  public void purgeStaleRequests(List<String> activeRequestIds, long deleteBeforeTime) {
    final List<String> requestIds = getChildren(MAIL_HISTORY_PATH);
    for (String requestId : requestIds) {
      if (!activeRequestIds.contains(requestId)) {
        String path = getMailRecordPathForRequest(requestId);
        Optional<Stat> maybeStat = checkExists(ZKPaths.makePath(path, SingularityEmailType.REQUEST_REMOVED.name()));
        if (maybeStat.isPresent() && maybeStat.get().getMtime() < deleteBeforeTime) {
          delete(path);
        }
      }
    }
  }
}
