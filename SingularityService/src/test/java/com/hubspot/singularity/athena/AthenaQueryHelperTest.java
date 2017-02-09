package com.hubspot.singularity.athena;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.Assert;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hubspot.singularity.OrderDirection;

public class AthenaQueryHelperTest {

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
    String expectedQuery = "CREATE EXTERNAL TABLE IF NOT EXISTS database.json_formatted_table (`timestamp` TIMESTAMP, `ip` STRING, `hostname` STRING) PARTITIONED BY (`requestId` string, `year` string) ROW FORMAT SERDE 'org.apache.hive.hcatalog.data.JsonSerDe' with serdeproperties ( 'paths'='timestamp,ip,hostname' ) LOCATION 's3://my-bucket-name/prefix';";
    Assert.assertEquals(expectedQuery, AthenaQueryHelper.createTableQuery("database", table));
  }

  @Test
  public void testQuerySql() {
    AthenaQuery query = new AthenaQuery(
        "SELECT",
        "my-table",
        ImmutableList.of("month", "timestamp"),
        ImmutableList.of(
            new AthenaQueryField("requestId", ComparisonOperator.EQUAL, "test-request-id", AthenaFieldType.STRING),
            new AthenaQueryField("month", ComparisonOperator.EQUAL, "12", AthenaFieldType.STRING)
        ),
        Optional.of(OrderDirection.DESC),
        Optional.of("timestamp"),
        Optional.of(100)
    );
    String expected = "SELECT month,timestamp FROM database.my-table WHERE requestId = 'test-request-id' and month = '12' ORDER BY timestamp DESC LIMIT 100;";
    Assert.assertEquals(expected, AthenaQueryHelper.generateSelectQuerySql("database", query));
  }

  @Test
  public void testPartitionGeneration() {
    long end = 1485907201000L; // Feb 1, 2017 00:00:01
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

    Map<List<AthenaPartitionWithValue>, String> partitionsWithStatements = AthenaQueryHelper.getPartitionsWithStatements(table, start, end);
    Assert.assertEquals(11, partitionsWithStatements.size());

    List<AthenaPartitionWithValue> partitionExampleStart = ImmutableList.of(
        new AthenaPartitionWithValue(AthenaPartitionType.REQUESTID, "test-request-id"),
        new AthenaPartitionWithValue(AthenaPartitionType.YEAR, "2017"),
        new AthenaPartitionWithValue(AthenaPartitionType.MONTH, "01"),
        new AthenaPartitionWithValue(AthenaPartitionType.DAY, "22")
    );

    Assert.assertEquals("(requestId='test-request-id',year='2017',month='01',day='22') location 's3://my-bucket-name/prefix/test-request-id/2017/01/22'",
        partitionsWithStatements.get(partitionExampleStart));

    List<AthenaPartitionWithValue> partitionExampleEnd = ImmutableList.of(
        new AthenaPartitionWithValue(AthenaPartitionType.REQUESTID, "test-request-id"),
        new AthenaPartitionWithValue(AthenaPartitionType.YEAR, "2017"),
        new AthenaPartitionWithValue(AthenaPartitionType.MONTH, "02"),
        new AthenaPartitionWithValue(AthenaPartitionType.DAY, "01")
    );

    Assert.assertEquals("(requestId='test-request-id',year='2017',month='02',day='01') location 's3://my-bucket-name/prefix/test-request-id/2017/02/01'",
        partitionsWithStatements.get(partitionExampleEnd));
  }

  @Test
  public void testAddPartitionGenerate() {
    String generatedStatement = "(requestId='test-request-id',year='2017',month='01',day='30') location 's3://my-bucket-name/prefix/test-request-id/2017/01/30'";
    Assert.assertEquals(AthenaQueryHelper.addPartitionQuery("database", "my_table", generatedStatement),
        "ALTER TABLE database.my_table ADD PARTITION (requestId='test-request-id',year='2017',month='01',day='30') location 's3://my-bucket-name/prefix/test-request-id/2017/01/30';");
  }

  @Test
  public void testPartitionTimeFromQueryEquals() throws Exception {
    List<AthenaQueryField> partitionFieldsQueried = ImmutableList.of(
        new AthenaQueryField("requestId", ComparisonOperator.EQUAL, "test-request-id", AthenaFieldType.STRING),
        new AthenaQueryField("year", ComparisonOperator.EQUAL, "2017", AthenaFieldType.STRING),
        new AthenaQueryField("month", ComparisonOperator.EQUAL, "02", AthenaFieldType.STRING),
        new AthenaQueryField("day", ComparisonOperator.EQUAL, "02", AthenaFieldType.STRING)
    );

    Assert.assertEquals(1485993600000L, AthenaQueryHelper.getTimeFromPartitionFields(partitionFieldsQueried, true));
    Assert.assertEquals(1486079999999L, AthenaQueryHelper.getTimeFromPartitionFields(partitionFieldsQueried, false));
  }

  @Test
  public void testPartitionTimeFromQueryConditions() throws Exception {
    List<AthenaQueryField> partitionFieldsQueried = ImmutableList.of(
        new AthenaQueryField("requestId", ComparisonOperator.EQUAL, "test-request-id", AthenaFieldType.STRING),
        new AthenaQueryField("year", ComparisonOperator.EQUAL, "2017", AthenaFieldType.STRING),
        new AthenaQueryField("month", ComparisonOperator.EQUAL, "02", AthenaFieldType.STRING),
        new AthenaQueryField("day", ComparisonOperator.GREATER_THAN, "01", AthenaFieldType.STRING),
        new AthenaQueryField("day", ComparisonOperator.LESS_THAN, "08", AthenaFieldType.STRING)
    );

    Assert.assertEquals(1485907200000L, AthenaQueryHelper.getTimeFromPartitionFields(partitionFieldsQueried, true));
    Assert.assertEquals(1486598399999L, AthenaQueryHelper.getTimeFromPartitionFields(partitionFieldsQueried, false));
  }

  @Test
  public void testPrefixParse() {
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
    List<AthenaPartitionWithValue> partitions = ImmutableList.of(
        new AthenaPartitionWithValue(AthenaPartitionType.REQUESTID, "my-test-request"),
        new AthenaPartitionWithValue(AthenaPartitionType.YEAR, "2017")
    );

    Assert.assertEquals("prefix/my-test-request/2017", AthenaQueryHelper.getPrefix(table, partitions));
  }
}
