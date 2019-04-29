package com.hubspot.singularity.data.usage;

import java.util.Date;
import java.util.List;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

import com.hubspot.singularity.SingularityTaskUsage;

public abstract class MySQLTaskUsageJDBI extends TaskUsageJDBI {

  @SqlUpdate("DELETE FROM taskUsage WHERE requestId = :requestId AND taskId = :taskId")
  public abstract void deleteTaskUsage(@Bind("requestId") String requestId, @Bind("taskId") String taskId);

  @SqlUpdate("DELETE FROM taskUsage WHERE taskId = :taskId")
  public abstract void deleteTaskUsage(@Bind("taskId") String taskId);

  @SqlUpdate("DELETE FROM taskUsage WHERE requestId = :requestId AND taskId = :taskId AND timestamp = :timestamp")
  public abstract void deleteSpecificTaskUsage(@Bind("requestId") String requestId, @Bind("taskId") String taskId, @Bind("timestamp") Date timestamp);

  @SqlUpdate("INSERT INTO taskUsage (" + FILEDS + ") VALUES (" + FILED_VALUES + ")")
  public abstract void saveSpecificTaskUsage(@Bind("requestId") String requestId, @Bind("taskId") String taskId, @Bind("memoryTotalBytes") long memoryTotalBytes,
                                             @Bind("timestamp") Date timestamp, @Bind("cpuSeconds") double cpuSeconds, @Bind("diskTotalBytes") long diskTotalBytes, @Bind("cpusNrPeriods") long cpusNrPeriods,
                                             @Bind("cpusNrThrottled") long cpusNrThrottled, @Bind("cpusThrottledTimeSecs") double cpusThrottledTimeSecs);

  @SqlQuery("SELECT " + FILEDS + "FROM taskUsage WHERE requestId = :requestId AND taskId = :taskId")
  public abstract List<SingularityTaskUsage> getTaskUsage(@Bind("requestId") String requestId, @Bind("taskId") String taskId);

  @SqlQuery("SELECT COUNT(DISTINCT taskId) FROM taskUsage")
  public abstract int countTasksWithUsage();
}
