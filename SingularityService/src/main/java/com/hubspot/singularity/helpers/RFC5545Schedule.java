package com.hubspot.singularity.helpers;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class RFC5545Schedule {

  RecurrenceRule recurrenceRule;
  org.joda.time.DateTime dtStart;

  public RFC5545Schedule(String schedule) throws InvalidRecurrenceRuleException {
    this.recurrenceRule = new RecurrenceRule(schedule);

    if (this.recurrenceRule.isInfinite()) {
      // set limit at 2100-01-01 00:00:00
      this.recurrenceRule.setUntil(new DateTime(2100, 1, 1, 0, 0, 0));
    }

    // DTSTART is RFC5545 but NOT in the recur string, but its a nice to have? :)
    Pattern pattern = Pattern.compile("DTSTART=([0-9]{8}T[0-9]{6})");
    Matcher matcher = pattern.matcher(schedule);

    if (matcher.find()) {
      DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss");
      this.dtStart = formatter.parseDateTime(matcher.group(1));
    } else {
      this.dtStart = org.joda.time.DateTime.now();
      this.dtStart = dtStart.withSecondOfMinute(0);
    }
  }

  public org.joda.time.DateTime getStartDateTime() {
    return this.dtStart;
  }

  public Date getNextValidTime()
  {
    final long now = System.currentTimeMillis();
    DateTime startDateTime = new DateTime(this.dtStart.getYear(), (this.dtStart.getMonthOfYear() - 1), this.dtStart.getDayOfMonth(),
        this.dtStart.getHourOfDay(), this.dtStart.getMinuteOfHour(), this.dtStart.getSecondOfMinute());
    RecurrenceRuleIterator timeIterator = this.recurrenceRule.iterator(startDateTime);

    long nextRunAtTimestamp = 0;
    while (timeIterator.hasNext()) {
      nextRunAtTimestamp = timeIterator.nextMillis();
      if (nextRunAtTimestamp >= now) {
        break;
      } else {
        nextRunAtTimestamp = 0;
      }
    }

    if(nextRunAtTimestamp > 0) {
      return new Date(nextRunAtTimestamp);
    }
    else {
      return null;
    }
  }
}
