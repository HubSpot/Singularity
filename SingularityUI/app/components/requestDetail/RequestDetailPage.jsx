import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';

import * as RefreshActions from '../../actions/ui/refresh';

import { FetchRequest } from '../../actions/api/requests';
import {
  FetchActiveTasksForRequest,
  FetchTaskHistoryForRequestWithMetaData,
  FetchDeploysForRequestWithMetaData,
  FetchRequestHistoryWithMetaData
} from '../../actions/api/history';
import {
  FetchScheduledTasksForRequest,
  FetchTaskCleanups
} from '../../actions/api/tasks';

import RequestHeader from './RequestHeader';
import RequestExpiringActions from './RequestExpiringActions';
import ActiveTasksTable from './ActiveTasksTable';
import PendingTasksTable from './PendingTasksTable';
import TaskHistoryTable from './TaskHistoryTable';
import DeployHistoryTable from './DeployHistoryTable';
import RequestHistoryTable from './RequestHistoryTable';

import Utils from '../../utils';

function refresh(props) {
  props.fetchRequest(props.params.requestId);
  props.fetchActiveTasksForRequest(props.params.requestId);
  props.fetchTaskCleanups();
  props.fetchTaskHistoryForRequestWithMetaData(props.params.requestId, 5, 1);
  props.fetchDeploysForRequestWithMetaData(props.params.requestId, 5, 1);
  props.fetchRequestHistoryWithMetaData(props.params.requestId, 5, 1);
  props.fetchScheduledTasksForRequest(props.params.requestId);
}

class RequestDetailPage extends Component {
  componentDidMount() {
    this.props.refresh();
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.params !== this.props.params) {
      refresh(nextProps);
    }
  }

  componentWillUnmount() {
    this.props.cancelRefresh();
  }

  render() {
    const { requestId } = this.props.params;
    const { deleted } = this.props;
    return (
      <div>
        <RequestHeader requestId={requestId} showBreadcrumbs={this.props.showBreadcrumbs} deleted={this.props.deleted} />
        {deleted || <RequestExpiringActions requestId={requestId} />}
        {deleted || <ActiveTasksTable requestId={requestId} />}
        {deleted || <PendingTasksTable requestId={requestId} />}
        {deleted || <TaskHistoryTable requestId={requestId} />}
        {deleted || <DeployHistoryTable requestId={requestId} />}
        <RequestHistoryTable requestId={requestId} />
      </div>
    );
  }
}

RequestDetailPage.propTypes = {
  params: PropTypes.object.isRequired,
  refresh: PropTypes.func.isRequired,
  cancelRefresh: PropTypes.func.isRequired,
  deleted: PropTypes.bool,
  showBreadcrumbs: PropTypes.bool
};

const mapStateToProps = (state, ownProps) => {
  const statusCode = Utils.maybe(state, ['api', 'request', ownProps.params.requestId, 'statusCode']);
  const history = Utils.maybe(state, ['api', 'requestHistory', ownProps.params.requestId, 'data']);
  return {
    notFound: statusCode === 404 && _.isEmpty(history),
    deleted: statusCode === 404 && !_.isEmpty(history),
    pathname: ownProps.location.pathname
  };
};

const mapDispatchToProps = (dispatch, ownProps) => {
  const refreshActions = [
    FetchRequest.trigger(ownProps.params.requestId, true),
    FetchActiveTasksForRequest.trigger(ownProps.params.requestId),
    FetchScheduledTasksForRequest.trigger(ownProps.params.requestId),
    FetchTaskCleanups.trigger()
  ];
  return {
    refresh: () => {
      dispatch(RefreshActions.BeginAutoRefresh(
        `RequestDetailPage-${ownProps.index}`,
        refreshActions,
        5000
      ));
    },
    cancelRefresh: () => dispatch(
      RefreshActions.CancelAutoRefresh(`RequestDetailPage-${ownProps.index}`)
    ),
    fetchRequest: (requestId) => dispatch(FetchRequest.trigger(requestId, true)),
    fetchActiveTasksForRequest: (requestId) => dispatch(FetchActiveTasksForRequest.trigger(requestId)),
    fetchScheduledTasksForRequest: (requestId) => dispatch(FetchScheduledTasksForRequest.trigger(requestId)),
    fetchTaskCleanups: () => dispatch(FetchTaskCleanups.trigger()),
    fetchTaskHistoryForRequestWithMetaData: (requestId, count, page) => dispatch(FetchTaskHistoryForRequestWithMetaData.trigger(requestId, count, page)),
    fetchDeploysForRequestWithMetaData: (requestId, count, page) => dispatch(FetchDeploysForRequestWithMetaData.trigger(requestId, count, page)),
    fetchRequestHistoryWithMetaData: (requestId, count, page) => dispatch(FetchRequestHistoryWithMetaData.trigger(requestId, count, page))
  };
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(rootComponent(RequestDetailPage, (props) => props.params.requestId, refresh, false));
