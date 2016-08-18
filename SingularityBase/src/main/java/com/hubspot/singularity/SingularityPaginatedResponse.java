package com.hubspot.singularity;

import com.google.common.base.Optional;

import java.util.List;

public class SingularityPaginatedResponse<Q> {

  private final Optional<Integer> dataCount;
  private final Optional<Integer> pageCount;
  private final Optional<Integer> page;
  private final List<Q> data;

  public SingularityPaginatedResponse(final Optional<Integer> dataCount, final Optional<Integer> pageCount, final Optional<Integer> page, final List<Q> data) {
    this.dataCount = dataCount;
    this.pageCount = pageCount;
    this.page = page;
    this.data = data;
  }

  public Optional<Integer> getDataCount() {
    return dataCount;
  }

  public Optional<Integer> getPageCount() {
    return pageCount;
  }

  public Optional<Integer> getPage() {
    return page;
  }

  public List<Q> getData() {
    return data;
  }
}
