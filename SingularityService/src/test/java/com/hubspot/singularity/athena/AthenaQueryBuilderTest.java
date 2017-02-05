package com.hubspot.singularity.athena;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.Assert;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hubspot.singularity.OrderDirection;

public class AthenaQueryBuilderTest {

  @Test
  public void testTableCreationSql() {
    AthenaTable table = new AthenaTable(
        "json_formatted_table",
        AthenaDataFormat.HIVE_JSON,
        ImmutableList.of(
            new AthenaField("timestamp", AthenaFieldType.TIMESTAMP, "Timestamp"),
            new AthenaField("ip", AthenaFieldType.STRING, "IP Address"),
            new AthenaField("hostname", AthenaFieldType.STRING, "Hostname")
        ),
        ImmutableList.of(
            AthenaPartitionType.REQUESTID,
            AthenaPartitionType.YEAR
        ),
        ImmutableList.of("test-request-id"),
        "s3://my-bucket-name/prefix"
    );
    String expectedQuery = "CREATE EXTERNAL TABLE IF NOT EXISTS json_formatted_table (`timestamp` TIMESTAMP, `ip` STRING, `hostname` STRING) PARTITIONED BY (`requestId` string, `year` int) ROW FORMAT SERDE 'org.apache.hive.hcatalog.data.JsonSerDe' with serdeproperties ( 'paths'='`timestamp`,`ip`,`hostname`' ) LOCATION 's3://my-bucket-name/prefix';";
    Assert.assertEquals(expectedQuery, AthenaQueryBuilder.createTableQuery(table));
  }

  @Test
  public void testQuerySql() {
    AthenaQuery query = new AthenaQuery(
        "SELECT",
        "my-table",
        ImmutableList.of("month", "timestamp"),
        ImmutableList.of(
            new AthenaQueryField("requestId", ComparisonOperator.EQUAL, "test-request-id"),
            new AthenaQueryField("month", ComparisonOperator.EQUAL, "12")
        ),
        Optional.of(OrderDirection.DESC),
        Optional.of("timestamp"),
        Optional.of(100)
    );
    String expected = "SELECT month,timestamp FROM my-table WHERE requestId = 'test-request-id' and month = '12' ORDER BY timestamp DESC LIMIT 100;";
    Assert.assertEquals(expected, AthenaQueryBuilder.generateSelectQuerySql(query));
  }

  @Test
  public void testPartitionGeneration() {
    long end = 1485907200000L; // Feb 1, 2017 00:00:00
    long start = 1485043200000L; // Jan 22, 2017 00:00:00
    AthenaTable table = new AthenaTable(
        "json-formatted-table",
        AthenaDataFormat.HIVE_JSON,
        Collections.<AthenaField>emptyList(),
        ImmutableList.of(
            AthenaPartitionType.REQUESTID,
            AthenaPartitionType.YEAR,
            AthenaPartitionType.MONTH,
            AthenaPartitionType.DAY
        ),
        ImmutableList.of("test-request-id"),
        "s3://my-bucket-name/prefix"
    );

    Map<List<AthenaPartitionWithValue>, String> partitionsWithStatements = AthenaQueryBuilder.getPartitionsWithStatements(table, start, end);
    Assert.assertEquals(10, partitionsWithStatements.size());

    List<AthenaPartitionWithValue> partitionExample = ImmutableList.of(
        new AthenaPartitionWithValue(AthenaPartitionType.REQUESTID, "test-request-id"),
        new AthenaPartitionWithValue(AthenaPartitionType.YEAR, "2017"),
        new AthenaPartitionWithValue(AthenaPartitionType.MONTH, "01"),
        new AthenaPartitionWithValue(AthenaPartitionType.DAY, "30")
    );

    Assert.assertEquals("(requestId='test-request-id',year='2017',month='01',day='30') location 's3://my-bucket-name/prefix/test-request-id/2017/01/30'",
        partitionsWithStatements.get(partitionExample));
  }

  @Test
  public void testAddPartitionGenerate() {
    String generatedStatement = "(requestId='test-request-id',year='2017',month='01',day='30') location 's3://my-bucket-name/prefix/test-request-id/2017/01/30'";
    Assert.assertEquals(AthenaQueryBuilder.addPartitionQuery("database", "my_table", generatedStatement),
        "ALTER TABLE database.my_table ADD PARTITION (requestId='test-request-id',year='2017',month='01',day='30') location 's3://my-bucket-name/prefix/test-request-id/2017/01/30';");
  }
}
