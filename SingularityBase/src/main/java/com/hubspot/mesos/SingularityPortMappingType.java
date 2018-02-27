package com.hubspot.mesos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum SingularityPortMappingType {
  LITERAL,     // value == port number
  FROM_OFFER   // value == index of ports resource in offer
}
