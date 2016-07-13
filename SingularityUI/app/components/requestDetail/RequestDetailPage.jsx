import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';

import * as RefreshActions from '../../actions/ui/refresh';

import { FetchRequest } from '../../actions/api/requests';
import {
  FetchActiveTasksForRequest,
  FetchTaskHistoryForRequest,
  FetchDeploysForRequest,
  FetchRequestHistory
} from '../../actions/api/history';
import { FetchTaskCleanups } from '../../actions/api/tasks';

import RequestHeader from './RequestHeader';
import RequestExpiringActions from './RequestExpiringActions';
import ActiveTasksTable from './ActiveTasksTable';
import TaskHistoryTable from './TaskHistoryTable';
import DeployHistoryTable from './DeployHistoryTable';
import RequestHistoryTable from './RequestHistoryTable';

class RequestDetailPage extends Component {
  componentDidMount() {
    this.props.refresh();
  }

  componentWillUnmount() {
    this.props.cancelRefresh();
  }

  render() {
    const { requestId } = this.props.params;
    return (
      <div>
        <RequestHeader requestId={requestId} />
        <RequestExpiringActions requestId={requestId} />
        <ActiveTasksTable requestId={requestId} />
        <TaskHistoryTable requestId={requestId} />
        <DeployHistoryTable requestId={requestId} />
        <RequestHistoryTable requestId={requestId} />
      </div>
    );
  }
}

RequestDetailPage.propTypes = {
  params: PropTypes.object.isRequired,
  refresh: PropTypes.func.isRequired,
  cancelRefresh: PropTypes.func.isRequired
};

const mapDispatchToProps = (dispatch, ownProps) => {
  const refreshActions = [
    FetchRequest.trigger(ownProps.params.requestId),
    FetchActiveTasksForRequest.trigger(ownProps.params.requestId),
    FetchTaskCleanups.trigger()
  ];
  return {
    refresh: () => dispatch(RefreshActions.BeginAutoRefresh(
      'RequestDetailPage',
      refreshActions,
      5000
    )),
    cancelRefresh: () => dispatch(
      RefreshActions.CancelAutoRefresh('RequestDetailPage')
    ),
    fetchRequest: (requestId) => dispatch(FetchRequest.trigger(requestId)),
    fetchActiveTasksForRequest: (requestId) => dispatch(FetchActiveTasksForRequest.trigger(requestId)),
    fetchTaskCleanups: () => dispatch(FetchTaskCleanups.trigger()),
    fetchTaskHistoryForRequest: (requestId, count, page) => dispatch(FetchTaskHistoryForRequest.trigger(requestId, count, page)),
    fetchDeploysForRequest: (requestId, count, page) => dispatch(FetchDeploysForRequest.trigger(requestId, count, page)),
    fetchRequestHistory: (requestId, count, page) => dispatch(FetchRequestHistory.trigger(requestId, count, page)),
  };
};

function refresh(props) {
  props.fetchRequest(props.params.requestId);
  props.fetchActiveTasksForRequest(props.params.requestId);
  props.fetchTaskCleanups();
  props.fetchTaskHistoryForRequest(props.params.requestId, 5, 1);
  props.fetchDeploysForRequest(props.params.requestId, 5, 1);
  props.fetchRequestHistory(props.params.requestId, 5, 1);
}

export default connect(
  null,
  mapDispatchToProps
)(rootComponent(RequestDetailPage, (props) => props.params.requestId, refresh, false));
