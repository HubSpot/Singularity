package com.hubspot.singularity.swagger;

import java.io.Serializable;
import java.util.Comparator;

import com.github.kongchen.swagger.docgen.mustache.MustacheApi;

public class ApiSortComparator implements Comparator<MustacheApi>,Serializable {
  @Override
  public int compare(MustacheApi o1, MustacheApi o2) {
    return o2.getPath().compareTo(o1.getPath());
  }
}
