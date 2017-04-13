package com.hubspot.mesos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;

public class CounterMap<K> {

  private final Map<K, Counter> map;

  public CounterMap() {
    this.map = new HashMap<>();
  }

  public CounterMap(int initialCapacity) {
    this.map = new HashMap<>(initialCapacity);
  }

  public Map<K, Long> toCountMap() {
    Map<K, Long> result = Maps.newHashMapWithExpectedSize(map.size());

    for (Entry<K, Counter> entry : map.entrySet()) {
      result.put(entry.getKey(), entry.getValue().count);
    }

    return result;
  }

  public static final class Counter {
    private long count;

    private Counter() {
      count = 0;
    }

    public long getCount() {
      return count;
    }

    @Override
    public String toString() {
      return "Counter [count=" + count + "]";
    }
  }

  public void incr(K key, long amount) {
    Counter c = map.computeIfAbsent(key, k -> new Counter());

    c.count += amount;
  }

  public void incr(K key) {
    incr(key, 1);
  }

  public void decr(K key) {
    Counter c = map.computeIfAbsent(key, k -> new Counter());

    c.count--;
  }

  public long getCount(K key) {
    Counter c = map.get(key);

    if (c == null) {
      return 0;
    }

    return c.count;
  }

  public Collection<K> getKeys() {
    return map.keySet();
  }

  public void clear() {
    map.clear();
  }

  public List<Entry<K, Counter>> asSortedEntryList() {
    List<Entry<K, Counter>> entries = new ArrayList<>(map.size());

    for (Entry<K, Counter> entry : map.entrySet()) {
      entries.add(entry);
    }

    Collections.sort(entries, new Comparator<Entry<K, Counter>>() {

      @Override
      public int compare(Entry<K, Counter> o1, Entry<K, Counter> o2) {
        return Longs.compare(o2.getValue().getCount(), o1.getValue().getCount());
      }

    });

    return entries;
  }

  @Override
  public String toString() {
    return "CounterMap{" +
        "map=" + map +
        '}';
  }
}
