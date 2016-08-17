package com.hubspot.singularity;

import java.util.List;

public class SingularityPaginatedResponse<Q> {

  private final int dataCount;
  private final int pageCount;
  private final int page;
  private final List<Q> data;

  public SingularityPaginatedResponse(final int dataCount, final int pageCount, final int page, final List<Q> data) {
    this.dataCount = dataCount;
    this.pageCount = pageCount;
    this.page = page;
    this.data = data;
  }

  public int getDataCount() {
    return dataCount;
  }

  public int getPageCount() {
    return pageCount;
  }

  public int getPage() {
    return page;
  }

  public List<Q> getData() {
    return data;
  }
}
