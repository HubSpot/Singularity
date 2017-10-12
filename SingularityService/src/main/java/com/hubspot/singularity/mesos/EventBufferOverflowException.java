package com.hubspot.singularity.mesos;

class EventBufferOverflowException extends Exception {
  EventBufferOverflowException(String message) {
    super(message);
  }
}
