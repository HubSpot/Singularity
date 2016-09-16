package com.hubspot.singularity.helpers;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.recur.RecurrenceRule.Part;
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class RFC5545Schedule {
  public static final int MAX_ITERATIONS = 1000000;
  private final RecurrenceRule recurrenceRule;
  private final org.joda.time.DateTime dtStart;

  public RFC5545Schedule(String schedule) throws InvalidRecurrenceRuleException {
    // DTSTART is RFC5545 but NOT in the recur string, but its a nice to have? :)
    Pattern pattern = Pattern.compile("DTSTART=([0-9]{8}T[0-9]{6})");
    Matcher matcher = pattern.matcher(schedule);

    if (matcher.find()) {
      DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss");
      this.dtStart = formatter.parseDateTime(matcher.group(1));
      this.recurrenceRule = new RecurrenceRule(matcher.replaceAll("").replace("RRULE:", ""));
    } else {
      this.recurrenceRule = new RecurrenceRule(schedule);
      this.dtStart = org.joda.time.DateTime.now().withSecondOfMinute(0);
    }
  }

  public org.joda.time.DateTime getStartDateTime() {
    return dtStart;
  }

  public Date getNextValidTime() {
    final long now = System.currentTimeMillis();
    DateTime startDateTime = new DateTime(dtStart.getYear(), (dtStart.getMonthOfYear() - 1), dtStart.getDayOfMonth(),
      dtStart.getHourOfDay(), dtStart.getMinuteOfHour(), dtStart.getSecondOfMinute());
    RecurrenceRuleIterator timeIterator = recurrenceRule.iterator(startDateTime);

    int count = 0;
    while (timeIterator.hasNext() && (count < MAX_ITERATIONS || (recurrenceRule.hasPart(Part.COUNT) && count < recurrenceRule.getCount()))) {
      count ++;
      long nextRunAtTimestamp = timeIterator.nextMillis();
      if (nextRunAtTimestamp >= now) {
        return new Date(nextRunAtTimestamp);
      }
    }
    return null;
  }

  public RecurrenceRule getRecurrenceRule() {
    return recurrenceRule;
  }
}
