package com.hubspot.singularity.data.usage;

import java.util.List;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

import com.hubspot.singularity.SingularityTaskUsage;

public abstract class PostgresTaskUsageJDBI extends TaskUsageJDBI {
  @SqlUpdate("DELETE FROM taskUsage WHERE taskId = :taskId")
  public abstract void deleteTaskUsage(@Bind("taskId") String taskId);

  @SqlUpdate("DELETE FROM taskUsage WHERE taskId = :taskId AND timestamp = :timestamp")
  public abstract void deleteSpecificTaskUsage(@Bind("taskId") String taskId, @Bind("timestamp") long timestamp);

  @SqlUpdate("INSERT INTO taskUsage (" + FIELDS + ") VALUES (" + FIELD_VALUES + ")")
  public abstract void saveSpecificTaskUsage(@Bind("requestId") String requestId, @Bind("taskId") String taskId, @Bind("memoryTotalBytes") long memoryTotalBytes,
                                             @Bind("timestamp") long timestamp, @Bind("cpuSeconds") double cpuSeconds, @Bind("diskTotalBytes") long diskTotalBytes, @Bind("cpusNrPeriods") long cpusNrPeriods,
                                             @Bind("cpusNrThrottled") long cpusNrThrottled, @Bind("cpusThrottledTimeSecs") double cpusThrottledTimeSecs);

  @SqlQuery("SELECT " + FIELDS + "FROM taskUsage WHERE taskId = :taskId")
  public abstract List<SingularityTaskUsage> getTaskUsage(@Bind("taskId") String taskId);

  @SqlQuery("SELECT COUNT(DISTINCT taskId) FROM taskUsage")
  public abstract int countTasksWithUsage();
}
