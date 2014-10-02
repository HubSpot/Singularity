package com.hubspot.singularity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class SingularityJsonObject {

  @JsonIgnore
  public byte[] getAsBytes(ObjectMapper objectMapper) throws SingularityJsonException {
    try {
      return objectMapper.writeValueAsBytes(this);
    } catch (JsonProcessingException jpe) {
      throw new SingularityJsonException(jpe);
    }
  }

  @SuppressWarnings("serial")
  public static class SingularityJsonException extends RuntimeException {

    public SingularityJsonException(Throwable cause) {
      super(cause);
    }

  }

  public <T> Optional<List<T>> copyOfList(Optional<List<T>> list) {
    return list.isPresent() ? Optional.<List<T>> of(Lists.newArrayList(list.get())) : list;
  }

  public <K, V> Optional<Map<K, V>> copyOfMap(Optional<Map<K, V>> map) {
    return map.isPresent() ? Optional.<Map<K, V>> of(Maps.newHashMap(map.get())) : map;
  }

}
