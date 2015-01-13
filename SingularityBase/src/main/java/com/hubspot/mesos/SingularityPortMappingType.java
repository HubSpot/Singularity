package com.hubspot.mesos;

public enum SingularityPortMappingType {
  LITERAL,     // value == port number
  FROM_OFFER   // value == index of ports resource in offer
}
