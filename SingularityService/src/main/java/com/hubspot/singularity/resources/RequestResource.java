package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityRequestValidator;

@Path("/request")
@Produces({ MediaType.APPLICATION_JSON })
public class RequestResource {

  private final RequestManager requestManager;
  
  @Inject
  public RequestResource(RequestManager requestManager) {
    this.requestManager = requestManager;
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequest submit(SingularityRequest request) {
    SingularityRequestValidator validator = new SingularityRequestValidator(request);
    request = validator.buildValidRequest();
    
    requestManager.persistRequest(request);
  
    requestManager.addToPendingQueue(request.getName());
  
    return request;
  }

  @GET
  public List<SingularityRequest> getKnownRequests() {
    return requestManager.getKnownRequests();
  }
  
  @DELETE
  @Path("/{requestName}")
  public Optional<SingularityRequest> getHistoryForTask(@PathParam("requestName") String requestName) {
    return requestManager.deleteRequest(requestName);
  }

}
