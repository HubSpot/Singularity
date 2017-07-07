package com.hubspot.singularity.proxy;

import javax.ws.rs.WebApplicationException;

public class NotImplemenedException extends WebApplicationException {
  NotImplemenedException() {
    super("Not Implemented", 500);
  }
}
