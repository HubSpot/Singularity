import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import rootComponent from '../../rootComponent';

import {
  FetchRequestsInState,
  RemoveRequest,
  UnpauseRequest,
  RunRequest,
  ScaleRequest,
  BounceRequest,
  FetchRequestRun
} from '../../actions/api/requests';
import { FetchRequestRunHistory } from '../../actions/api/history';
import { FetchTaskFiles } from '../../actions/api/sandbox';
import { refresh } from '../../actions/ui/requests';

import UITable from '../common/table/UITable';
import RequestFilters from './RequestFilters';
import * as Cols from './Columns';

import filterSelector from '../../selectors/requests/filterSelector';

import Utils from '../../utils';
import Loader from "../common/Loader";

class RequestsPage extends Component {

  static propTypes = {
    requestsInState: PropTypes.array,
    fetchFilter: PropTypes.func,
    removeRequest: PropTypes.func,
    unpauseRequest: PropTypes.func,
    runNow: PropTypes.func,
    fetchRun: PropTypes.func,
    fetchRunHistory: PropTypes.func,
    fetchTaskFiles: PropTypes.func,
    scaleRequest: PropTypes.func,
    bounceRequest: PropTypes.func,
    params: PropTypes.object,
    router: PropTypes.object,
    filter: PropTypes.shape({
      state: PropTypes.string,
      subFilter: PropTypes.array,
      searchFilter: PropTypes.string
    }).isRequired,
    requestUtilizations: PropTypes.array
  };

  constructor(props) {
    super(props);
    this.state = {
      loading: false
    };
  }

  handleFilterChange(filter) {
    const lastFilterState = this.props.filter.state;
    this.setState({
      loading: lastFilterState !== filter.state
    });

    const subFilter = filter.subFilter.length === RequestFilters.REQUEST_TYPES.length ? 'all' : filter.subFilter.join(',');
    this.props.router.push(`/requests/${filter.state}/${subFilter}/${filter.searchFilter}`);

    if (lastFilterState !== filter.state) {
      this.props.fetchFilter(filter.state).then(() => {
        this.setState({
          loading: false
        });
      });
    }
  }

  getColumns() {
    switch (this.props.filter.state) {
      case 'pending':
        return [Cols.RequestId, Cols.PendingType];
      case 'cleanup':
        return [Cols.RequestId, Cols.CleaningUser, Cols.CleaningTimestamp, Cols.CleanupType];
      case 'noDeploy':
        return [
          Cols.Starred,
          Cols.RequestId,
          Cols.Type,
          Cols.State,
          Cols.Instances,
          Cols.Schedule,
          Cols.Actions
        ];
      default:
        return [
          Cols.Starred,
          Cols.RequestId,
          Cols.Type,
          Cols.State,
          Cols.Instances,
          Cols.DeployId,
          Cols.DeployUser,
          Cols.LastDeploy,
          Cols.Schedule,
          Cols.Actions
        ];
    }
  }

  render() {
    const displayRequests = filterSelector({requestsInState: this.props.requestsInState, filter: this.props.filter, requestUtilizations: this.props.requestUtilizations});

    let table;
    if (this.state.loading) {
      table = <Loader />;
    } else if (!displayRequests.length) {
      table = <div className="empty-table-message"><p>No matching requests</p></div>;
    } else {
      table = (
        <UITable
          ref="table"
          data={displayRequests}
          keyGetter={(request) => (request.request ? request.request.id : request.requestId)}
        >
          {this.getColumns()}
        </UITable>
      );
    }

    return (
      <div>
        <RequestFilters
          filter={this.props.filter}
          onFilterChange={(filter) => this.handleFilterChange(filter)}
          displayRequestTypeFilters={!_.contains(['pending', 'cleaning'], this.props.filter.state)}
        />
        {table}
      </div>
    );
  }
}

function mapStateToProps(state, ownProps) {
  const requestsInState = state.api.requestsInState.data;
  const modifiedRequests = requestsInState.map((request) => {
    const hasActiveDeploy = !!(request.activeDeploy || (request.requestDeployState && request.requestDeployState.activeDeploy));
    return {
      ...request,
      hasActiveDeploy,
      canBeRunNow: request.state === 'ACTIVE' && _.contains(['SCHEDULED', 'ON_DEMAND'], request.request.requestType) && hasActiveDeploy,
      canBeScaled: _.contains(['ACTIVE', 'SYSTEM_COOLDOWN'], request.state) && hasActiveDeploy && _.contains(['WORKER', 'SERVICE'], request.request.requestType),
      id: request.request ? request.request.id : request.requestId
    };
  });
  const filter = {
    state: ownProps.params.state || 'all',
    subFilter: !ownProps.params.subFilter || ownProps.params.subFilter === 'all' ? RequestFilters.REQUEST_TYPES : ownProps.params.subFilter.split(','),
    searchFilter: ownProps.params.searchFilter || ''
  };
  const statusCode = Utils.maybe(state, ['api', 'requestsInState', 'statusCode']);

  return {
    pathname: ownProps.location.pathname,
    notFound: statusCode === 404,
    requestsInState: modifiedRequests,
    requestUtilizations: state.api.utilization.data.requestUtilizations,
    filter
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchFilter: (state) => dispatch(FetchRequestsInState.trigger(state === 'cleaning' ? 'cleanup' : state, true)),
    removeRequest: (requestid, data) => dispatch(RemoveRequest.trigger(requestid, data)),
    unpauseRequest: (requestId, data) => dispatch(UnpauseRequest.trigger(requestId, data)),
    runNow: (requestId, data) => dispatch(RunRequest.trigger(requestId, data)),
    fetchRun: (...args) => dispatch(FetchRequestRun.trigger(...args)),
    fetchRunHistory: (...args) => dispatch(FetchRequestRunHistory.trigger(...args)),
    fetchTaskFiles: (...args) => {
      return dispatch(FetchTaskFiles.trigger(...args));
    },
    scaleRequest: (requestId, data) => dispatch(ScaleRequest.trigger(requestId, data)),
    bounceRequest: (requestId, data) => dispatch(BounceRequest.trigger(requestId, data))
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(withRouter(RequestsPage), (props) => refresh(props.params.state || 'all')));
