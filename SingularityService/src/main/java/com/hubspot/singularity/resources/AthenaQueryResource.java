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
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityS3Log;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.athena.AthenaQuery;
import com.hubspot.singularity.athena.AthenaQueryBuilder;
import com.hubspot.singularity.athena.AthenaQueryResult;
import com.hubspot.singularity.athena.AthenaTable;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.data.AthenaQueryManager;
import com.hubspot.singularity.data.RequestManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path(AthenaQueryResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Query logs in s3 using aws athena", value=AthenaQueryResource.PATH)
public class AthenaQueryResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/athena";

  private final AthenaQueryManager queryManager;
  private final RequestManager requestManager;
  private final Optional<SingularityUser> user;
  private final SingularityAuthorizationHelper authorizationHelper;

  @Inject
  public AthenaQueryResource(AthenaQueryManager queryManager, RequestManager requestManager, Optional<SingularityUser> user, SingularityAuthorizationHelper authorizationHelper) {
    this.queryManager = queryManager;
    this.requestManager = requestManager;
    this.user = user;
    this.authorizationHelper = authorizationHelper;
  }

  @POST
  @Path("/table")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Create a new table in athena", response=AthenaTable.class)
  @ApiResponses({
    @ApiResponse(code=400, message="Provided table data is invalid"),
    @ApiResponse(code=403, message="User or Provided AWS keys are not allowed access"),
  })
  public AthenaTable createAthenaTable(AthenaTable table) throws Exception {
    authorizationHelper.checkAdminAuthorization(user);
    return queryManager.createTableThrows(table);
  }

  @GET
  @Path("/tables")
  @ApiOperation(value="List current athena tables and their metadata", response=AthenaTable.class, responseContainer="List")
  public List<AthenaTable> getAthenaTables() {
    authorizationHelper.checkAdminAuthorization(user);
    return queryManager.getTables();
  }

  @DELETE
  @Path("/table/{name}")
  @ApiOperation(value="Delete an athena table")
  public void dropAthenaTable(@PathParam("name") String name) throws Exception {
    authorizationHelper.checkAdminAuthorization(user);
    queryManager.deleteTable(name);
  }

  @POST
  @Path("/table/update-partitions")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Update all partitions for a table in athena", response=AthenaTable.class)
  @ApiResponses({
      @ApiResponse(code=404, message="Provided table not found"),
      @ApiResponse(code=403, message="User or Provided AWS keys are not allowed access"),
  })
  public Optional<AthenaTable> createAthenaTable(String tableName) throws Exception {
    authorizationHelper.checkAdminAuthorization(user);
    return queryManager.updatePartitions(tableName);
  }

  @POST
  @Path("/query/raw")
  @ApiOperation(value="Start a new athena query", response= AthenaQueryResult.class)
  @ApiResponses({
      @ApiResponse(code = 400, message = "Provided query is invalid"),
  })
  public AthenaQueryResult runRawQuery(String query) {
    authorizationHelper.checkAdminAuthorization(user);
    return queryManager.runQueryAsync(user, query);
  }

  @POST
  @Path("/query")
  @ApiOperation(value="Start a new athena query", response= AthenaQueryResult.class)
  @ApiResponses({
      @ApiResponse(code = 400, message = "Provided query is invalid"),
  })
  public AthenaQueryResult queryAthena(AthenaQuery query) {
    Optional<SingularityRequestWithState> maybeRequest = requestManager.getRequest(query.getRequestId());
    if (maybeRequest.isPresent()) {
      authorizationHelper.checkForAuthorization(maybeRequest.get().getRequest(), user, SingularityAuthorizationScope.READ);
    } else {
      authorizationHelper.checkAdminAuthorization(user);
    }
    return queryManager.runQueryAsync(user, AthenaQueryBuilder.generateSelectQuerySql(query));
  }

  @GET
  @Path("/query/history")
  @ApiOperation(value="Get recent queries for the current user")
  public List<AthenaQueryResult> getQueryHistory() {
    return queryManager.getQueriesForUser(user);
  }

  @GET
  @Path("/query/{id}")
  @ApiOperation(value="Get the result of a query by id", response=AthenaQueryResult.class)
  public Optional<AthenaQueryResult> getQueryResult(@PathParam("id") String id) {
    return queryManager.getQueryResult(user, id);
  }

  @GET
  @Path("/query/{id}/s3object")
  @ApiOperation(value="Get the download information for query results in S3")
  @ApiResponses({
      @ApiResponse(code = 404, message = "Provided query id or s3 result object does not exist"),
  })
  public Optional<SingularityS3Log> getQueryResultFromS3(@PathParam("id") String id) {
    // TODO - similar to s3 log resource
    return Optional.absent();
  }
}
