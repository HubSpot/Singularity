package com.hubspot.singularity.resources;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.mesos.SingularityDriver;
import com.hubspot.singularity.mesos.SingularityScheduler;
import org.apache.mesos.Protos;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

@Path("/task")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
public class TaskResource {
  private final ExecutorService executorService;
  private final String master;

  @Inject
  public TaskResource(ExecutorService executorService, @Named("singularity.master") String master) {
    this.executorService = executorService;
    this.master = master;
  }

  @POST
  public void submit(SingularityRequest request) {
    Protos.Value.Scalar s = Protos.Value.Scalar.newBuilder().setValue(1).build();
    executorService.submit(new SingularityDriver(master, ImmutableList.of(Protos.Resource.newBuilder().setType(Protos.Value.Type.SCALAR).setName("cpus").setScalar(s).build()), Protos.CommandInfo.newBuilder().setValue(request.getCommand()).build()));
  }

}
