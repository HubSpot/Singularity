package com.hubspot.singularity.resources;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityManaged;

@Path("/test")
public class TestResource {

  private final SingularityAbort abort;
  private final SingularityManaged managed;

  @Inject
  public TestResource(SingularityManaged managed, SingularityAbort abort) {
    this.managed = managed;
    this.abort = abort;
  }
  
  @POST
  @Path("/leader")
  public void setLeader() {
    managed.isLeader();
  }
  
  @POST
  @Path("/notleader")
  public void setNotLeader() {
    managed.notLeader();
  }
 
  @POST
  @Path("/stop")
  public void stop() throws Exception {
    managed.stop();
  }
  
  @POST
  @Path("/abort")
  public void abort() {
    abort.abort();
  }
  
  @POST
  @Path("/start")
  public void start() throws Exception {
    managed.start();
  }
  
}
