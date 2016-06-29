import React from 'react';
import { connect } from 'react-redux';

import { FetchAction } from '../../actions/api/requests';
import { RemoveAction, UnpauseAction, RunAction, ScaleAction, BounceAction, FetchRunAction, FetchRunHistoryAction } from '../../actions/api/request';
import { FetchAction as FetchTaskFiles } from '../../actions/api/taskFiles';

import UITable from '../common/table/UITable';
import RequestFilters from './RequestFilters';
import * as Cols from './Columns';

import Utils from '../../utils';

class RequestsPage extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      filter: {
        state: props.state,
        subFilter: props.subFilter === 'all' ? RequestFilters.REQUEST_TYPES : props.subFilter.split(','),
        searchFilter: props.searchFilter
      },
      loading: false
    }
  }

  componentDidMount() {
    if (this.props.requests.length) {
      Utils.fixTableColumns($(this.refs.table.getTableDOMNode()));
    }
  }

  handleFilterChange(filter) {
    const lastFilterState = this.state.filter.state;
    console.log(filter, this.state.filter)
    this.setState({
      loading: lastFilterState !== filter.state && !_.contains(['activeDeploy', 'noDeploy'], filter.state),
      filter: filter
    });

    const subFilter = filter.subFilter.length == RequestFilters.REQUEST_TYPES.length ? 'all' : filter.subFilter.join(',');
    this.props.updateFilters(filter.state, subFilter, filter.searchFilter);
    app.router.navigate(`/requests/${filter.state}/${subFilter}/${filter.searchFilter}`);

    if (lastFilterState !== filter.state && !_.contains(['activeDeploy', 'noDeploy'], filter.state)) {
      this.props.fetchFilter(filter.state).then(() => {
        this.setState({
          loading: false
        });
      });
    }
  }

  getColumns() {
    switch(this.state.filter.subFilter) {
      case RequestFilters.REQUEST_TYPES.ALL:
      default:
        return [
          Cols.Starred(),
          Cols.RequestId,
          Cols.Type,
          Cols.State,
          Cols.Instances,
          Cols.DeployId,
          Cols.DeployUser,
          Cols.LastDeploy,
          Cols.Schedule,
          Cols.Actions(this.props.removeRequest, this.props.unpauseRequest, this.props.runNow, this.props.fetchRun, this.props.fetchRunHistory, this.props.fetchTaskFiles, this.props.scaleRequest, this.props.bounceRequest)
        ];
    }
  }

  render() {
    const displayRequests = this.props.requests;
    // console.log(this.state.filter);

    let table;
    if (this.state.loading) {
      table = <div className="page-loader fixed"></div>;
    }
    else if (!displayRequests.length) {
      table = <div className="empty-table-message"><p>No matching requests</p></div>;
    } else {
      table = (
        <UITable
          ref="table"
          data={displayRequests}
          keyGetter={(r) => r.request.id}
        >
          {this.getColumns()}
        </UITable>
      );
    }

    return (
      <div>
        <RequestFilters filter={this.state.filter} onFilterChange={(filter) => this.handleFilterChange(filter)} displayRequestTypeFilters />
        {table}
      </div>
    );
  }
}

function mapStateToProps(state, ownProps) {
  const requests = state.api.requests.data;
  _.each(requests, (r) => {
    let hasActiveDeploy = !!(r.activeDeploy || (r.requestDeployState && r.requestDeployState.activeDeploy));
    r.canBeRunNow = r.state === 'ACTIVE' && _.contains(['SCHEDULED', 'ON_DEMAND'], r.request.requestType) && hasActiveDeploy;
    r.canBeScaled = _.contains(['ACTIVE', 'SYSTEM_COOLDOWN'], r.state) && hasActiveDeploy && _.contains(['WORKER', 'SERVICE'], r.request.requestType);
  });

  return {
    requests
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchFilter: (state) => dispatch(FetchAction.trigger(state)),
    removeRequest: (requestid, data) => dispatch(RemoveAction.trigger(requestid, data)),
    unpauseRequest: (requestId, data) => dispatch(UnpauseAction.trigger(requestId, data)),
    runNow: (requestId, data) => dispatch(RunAction.trigger(requestId, data)),
    fetchRun: (requestId, runId) => dispatch(FetchRunAction.trigger(requestId, runId)),
    fetchRunHistory: (requestId, runId) => dispatch(FetchRunHistoryAction.trigger(requestId, runId)),
    fetchTaskFiles: (taskId, path) => dispatch(FetchTaskFiles.trigger(taskId, path)),
    scaleRequest: (requestId, data) => dispatch(ScaleAction.trigger(requestId, data)),
    bounceRequest: (requestId, data) => dispatch(BounceAction.trigger(requestId, data))
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(RequestsPage);
