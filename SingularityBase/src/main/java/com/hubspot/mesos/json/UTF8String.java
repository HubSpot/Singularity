package com.hubspot.mesos.json;

import com.google.common.base.Charsets;

import java.util.Arrays;

public class UTF8String {
  private final byte[] data;

  private final int offset;
  private final int length;

  public UTF8String(byte[] data) {
    this(data, 0, data.length);
  }

  public UTF8String(byte[] data, int offset, int length) {
    this.data = data;
    this.offset = offset;
    this.length = length;
  }

  public byte[] getData() {
    return data;
  }

  public int getOffset() {
    return offset;
  }

  public int getLength() {
    return length;
  }

  public byte get(int index) {
    return data[index + offset];
  }

  @Override public String toString() {
    return new String(data, offset, length, Charsets.UTF_8);
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UTF8String that = (UTF8String) o;

    if (offset != that.offset) {
      return false;
    }
    if (length != that.length) {
      return false;
    }
    return Arrays.equals(data, that.data);

  }

  @Override public int hashCode() {
    int result = Arrays.hashCode(data);
    result = 31 * result + offset;
    result = 31 * result + length;
    return result;
  }
}
