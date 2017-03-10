package com.hubspot.singularity;

import java.util.List;

import com.google.common.base.Optional;

public class SingularityPaginatedResponse<Q> {

  private final Optional<Integer> dataCount;
  private final Optional<Integer> pageCount;
  private final int page;
  private final List<Q> objects;

  public SingularityPaginatedResponse(final Optional<Integer> dataCount, final Optional<Integer> pageCount, final Optional<Integer> page, final List<Q> objects) {
    this.dataCount = dataCount;
    this.pageCount = pageCount;
    this.page = page.or(1);
    this.objects = objects;
  }

  public Optional<Integer> getDataCount() {
    return dataCount;
  }

  public Optional<Integer> getPageCount() {
    return pageCount;
  }

  public int getPage() {
    return page;
  }

  public List<Q> getObjects() {
    return objects;
  }

  @Override
  public String toString() {
    return "SingularityPaginatedResponse{" +
        "dataCount=" + dataCount +
        ", pageCount=" + pageCount +
        ", page=" + page +
        ", objects=" + objects +
        '}';
  }
}
