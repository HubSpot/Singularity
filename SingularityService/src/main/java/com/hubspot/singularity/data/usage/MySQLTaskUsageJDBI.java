package com.hubspot.singularity.data.usage;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import com.hubspot.singularity.SingularityTaskUsage;

public interface MySQLTaskUsageJDBI extends TaskUsageJDBI {
  @SqlUpdate("DELETE FROM taskUsage WHERE taskId = :taskId")
  void deleteTaskUsage(@Bind("taskId") String taskId);

  @SqlUpdate("DELETE FROM taskUsage WHERE taskId = :taskId AND timestamp = :timestamp")
  void deleteSpecificTaskUsage(@Bind("taskId") String taskId, @Bind("timestamp") long timestamp);

  @SqlUpdate("INSERT INTO taskUsage (" + FIELDS + ") VALUES (" + FIELD_VALUES + ")")
  void saveSpecificTaskUsage(@Bind("requestId") String requestId, @Bind("taskId") String taskId, @Bind("memoryTotalBytes") long memoryTotalBytes,
                                             @Bind("timestamp") long timestamp, @Bind("cpuSeconds") double cpuSeconds, @Bind("diskTotalBytes") long diskTotalBytes, @Bind("cpusNrPeriods") long cpusNrPeriods,
                                             @Bind("cpusNrThrottled") long cpusNrThrottled, @Bind("cpusThrottledTimeSecs") double cpusThrottledTimeSecs);

  @SqlQuery("SELECT " + FIELDS + " FROM taskUsage WHERE taskId = :taskId")
  List<SingularityTaskUsage> getTaskUsage(@Bind("taskId") String taskId);

  @SqlQuery("SELECT DISTINCT taskId as id FROM taskUsage")
  List<String> getUniqueTaskIds();

  @SqlQuery("SELECT DISTINCT timestamp FROM taskUsage WHERE taskId = :taskId")
  List<Long> getUsageTimestampsForTask(@Bind("taskId") String taskId);

  @SqlQuery("SELECT COUNT(DISTINCT taskId) FROM taskUsage")
  int countTasksWithUsage();
}
