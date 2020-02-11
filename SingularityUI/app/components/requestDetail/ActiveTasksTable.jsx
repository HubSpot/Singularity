import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { Link } from 'react-router';
import { Button } from 'react-bootstrap';

import Section from '../common/Section';

import Utils from '../../utils';

import UITable from '../common/table/UITable';
import {
  Health,
  LoadBalancerState,
  InstanceNumberWithHostname,
  Host,
  LastTaskState,
  DeployId,
  UpdatedAt,
  StartedAt,
  RunId,
  LogLinkAndActions
} from '../tasks/Columns';

import { FetchTaskHistoryForRequest } from '../../actions/api/history';

import TaskStateBreakdown from './TaskStateBreakdown';

const ActiveTasksTable = ({request, requestId, tasksAPI, healthyTaskIds, cleaningTaskIds,loadBalancedTaskIds, killedTaskIds, fetchTaskHistoryForRequest}) => {
  const tasks = tasksAPI ? tasksAPI.data : [];
  const emptyTableMessage = (Utils.api.isFirstLoad(tasksAPI)
    ? <p>Loading...</p>
    : <p>No active tasks</p>
  );

  let maybeAggregateTailButton;
  if (tasks.length > 1) {
    maybeAggregateTailButton = (
      <Link to={`request/${requestId}/tail/${config.runningTaskLogPath}`}>
        <Button className="pull-right">
          View Aggregate Logs
        </Button>
      </Link>
    );
  }

  const tasksWithHealth = _.map(tasks, (task) => {
    let health;
    if (_.contains(healthyTaskIds, task.taskId.id)) {
      health = 'healthy';
    } else if (_.contains(cleaningTaskIds, task.taskId.id)) {
      health = 'cleaning';
    } else if (_.contains(killedTaskIds, task.taskId.id)) {
      health = 'terminating'
    } else {
      health = 'not yet healthy'
    }
    return {
      ...task,
      health: health
      activeInLb: _.contains(loadBalancedTaskIds, task.taskId.id)
    }
  });
  const title = <span>Running instances {maybeAggregateTailButton}</span>;

  return (
    <Section id="running-instances" title={title}>
      { localStorage.enableTaskStateBreakdown ? <TaskStateBreakdown requestId={requestId} /> : null }
      <UITable
        data={tasksWithHealth}
        keyGetter={(task) => task.taskId.id}
        emptyTableMessage={emptyTableMessage}
        triggerOnDataSizeChange={fetchTaskHistoryForRequest}
      >
        {Health}
        {request.request.loadBalanced && LoadBalancerState}
        {InstanceNumberWithHostname}
        {LastTaskState}
        {DeployId}
        {StartedAt}
        {UpdatedAt}
        {!Utils.request.isLongRunning(request) && RunId}
        {LogLinkAndActions(config.runningTaskLogPath, Utils.maybe(request, ['request', 'requestType'], 'UNKNOWN'))}
      </UITable>
    </Section>
  );
};

ActiveTasksTable.propTypes = {
  requestId: PropTypes.string.isRequired,
  tasksAPI: PropTypes.object.isRequired,
  healthyTaskIds: PropTypes.array.isRequired,
  cleaningTaskIds: PropTypes.array.isRequired,
  loadBalancedTaskIds: PropTypes.array.isRequired,
  killedTaskIds: PropTypes.array.isRequired,
  fetchTaskHistoryForRequest: PropTypes.func.isRequired
};

const mapStateToProps = (state, ownProps) => {
  const request = Utils.maybe(state.api.request, [ownProps.requestId, 'data'])
  return {
    request: request,
  tasksAPI: Utils.maybe(
    state.api.activeTasksForRequest,
    [ownProps.requestId]
  ),
  healthyTaskIds: _.map(Utils.maybe(request, ['taskIds', 'healthy'], []), (task) => {
    return task.id;
  }),
  cleaningTaskIds: _.map(Utils.maybe(request, ['taskIds', 'cleaning'], []), (task) => {
    return task.id;
  }),
  loadBalancedTaskIds: _.map(Utils.maybe(request, ['taskIds', 'loadBalanced'], []), (task) => {
    return task.id;
  }),
  killedTaskIds: _.map(Utils.maybe(request, ['taskIds', 'killed'], []), (task) => {
    return task.id;
  })
}};

const mapDispatchToProps = (dispatch, ownProps) => ({
  fetchTaskHistoryForRequest: () => dispatch(FetchTaskHistoryForRequest.trigger(ownProps.requestId, ownProps.taskHistoryPageSize, ownProps.taskHistoryPage))
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ActiveTasksTable);
