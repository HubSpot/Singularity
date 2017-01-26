package com.hubspot.singularity.athena;

import org.junit.Test;
import org.junit.Assert;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hubspot.singularity.OrderDirection;

public class AthenaQueryBuilderTest {

  @Test
  public void testTableCreationSql() {
    AthenaTable table = new AthenaTable(
        "json-formatted-table",
        ImmutableList.of(
            new AthenaField("timestamp", "string"),
            new AthenaField("ip", "string"),
            new AthenaField("hostname", "string")
        ),
        ImmutableList.of(
            new AthenaField("year", "string"),
            new AthenaField("month", "string"),
            new AthenaField("day", "string")
        ),
        "serde 'org.apache.hive.hcatalog.data.JsonSerDe'",
        "s3://my-bucket-name/prefix"
    );
    String expectedQuery = "CREATE EXTERNAL TABLE IF NOT EXISTS json-formatted-table (timestamp string, ip string, hostname string) PARTITIONED BY (year string, month string, day string) ROW FORMAT serde 'org.apache.hive.hcatalog.data.JsonSerDe') LOCATION 's3://my-bucket-name/prefix';";
    Assert.assertEquals(expectedQuery, AthenaQueryBuilder.createTableQuery(table));
  }

  @Test
  public void testQuerySql() {
    AthenaQuery query = new AthenaQuery(
        "SELECT",
        "my-table",
        ImmutableList.of("month", "timestamp"),
        ImmutableList.of(
            new AthenaQueryField("month", ComparisonOperator.EQUAL, "12")
        ),
        Optional.of(OrderDirection.DESC),
        Optional.of("timestamp"),
        Optional.of(100)
    );
    String expected = "SELECT month,timestamp FROM my-table WHERE month = '12' ORDER BY timestamp DESC LIMIT 100;";
    Assert.assertEquals(expected, AthenaQueryBuilder.generateSelectQuerySql(query));
  }
}
