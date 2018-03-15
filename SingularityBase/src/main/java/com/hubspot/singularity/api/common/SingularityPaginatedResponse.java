package com.hubspot.singularity.api.common;

import java.util.List;
import java.util.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Holds a page of responses and metadata")
public class SingularityPaginatedResponse<Q> {

  private final Optional<Integer> dataCount;
  private final Optional<Integer> pageCount;
  private final int page;
  private final List<Q> objects;

  public SingularityPaginatedResponse(final Optional<Integer> dataCount, final Optional<Integer> pageCount, final Optional<Integer> page, final List<Q> objects) {
    this.dataCount = dataCount;
    this.pageCount = pageCount;
    this.page = page.orElse(1);
    this.objects = objects;
  }

  @Schema(description = "The total number of rows on all pages", nullable = true)
  public Optional<Integer> getDataCount() {
    return dataCount;
  }

  @Schema(description = "The total number of pages")
  public Optional<Integer> getPageCount() {
    return pageCount;
  }

  @Schema(description = "Current page number")
  public int getPage() {
    return page;
  }

  @Schema(description = "Data for this page")
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
