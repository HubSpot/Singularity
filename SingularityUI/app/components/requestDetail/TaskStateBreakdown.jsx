import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { ProgressBar } from 'react-bootstrap';

import { FetchActiveTasksForRequest } from '../../actions/api/history';

import Utils from '../../utils';

const taskStateOrder = [
  'TASK_LAUNCHED',
  'TASK_STAGING',
  'TASK_STARTING',
  'TASK_RUNNING',
  'TASK_CLEANING',
  'TASK_KILLING',
  'TASK_FINISHED',
  'TASK_FAILED',
  'TASK_KILLED',
  'TASK_LOST',
  'TASK_LOST_WHILE_DOWN',
  'TASK_ERROR'
];

const taskStateProps = (s) => {
  switch (s) {
    case 'TASK_LAUNCHED':
      return { bsStyle: 'info', label: 'launched', striped: true};
    case 'TASK_STAGING':
      return { bsStyle: 'info', label: 'staging', striped: true};
    case 'TASK_STARTING':
      return { bsStyle: 'info', label: 'starting', striped: true, active: true};
    case 'TASK_RUNNING':
      return { bsStyle: 'success', label: 'running'};
    case 'TASK_CLEANING':
      return { bsStyle: 'warning', label: 'cleaning', striped: true};
    case 'TASK_KILLING':
      return { bsStyle: 'warning', label: 'killing', striped: true, active: true};
    case 'TASK_FINISHED':
      return { bsStyle: 'success', label: 'finished'};
    case 'TASK_FAILED':
      return { bsStyle: 'danger', label: 'failed'};
    case 'TASK_KILLED':
      return { bsStyle: 'danger', label: 'killed'};
    case 'TASK_LOST':
    case 'TASK_LOST_WHILE_DOWN':
      return { bsStyle: 'danger', label: 'lost'};
    case 'TASK_ERROR':
      return { bsStyle: 'danger', label: 'error'};
    default:
      return { bsStyle: 'danger', label: 'unknown'};
  }
};

const TaskStateBreakdown = ({activeTasksForRequest, refresh}) => {
  if (!activeTasksForRequest) {
    return null;
  }

  const instanceBreakdown = Utils.request.instanceBreakdown(activeTasksForRequest);
  const totalInstances = taskStateOrder.reduce((last, cur) => (
    last + (instanceBreakdown[cur] || 0)
  ), 0);

  if (totalInstances === 0) {
    return null;
  }

  const taskStateProgressBars = taskStateOrder.map((s) => (
    <ProgressBar key={s} now={100 * (instanceBreakdown[s] / totalInstances)} {...taskStateProps(s)} />
  ));


  return (
    <ProgressBar onClick={refresh}>
      {taskStateProgressBars}
    </ProgressBar>
  );
};

TaskStateBreakdown.propTypes = {
  requestId: PropTypes.string.isRequired,
  activeTasksForRequest: PropTypes.arrayOf(PropTypes.object).isRequired,
  refresh: PropTypes.func.isRequired
};

const mapStateToProps = (state, ownProps) => ({
  activeTasksForRequest: Utils.maybe(state.api, ['activeTasksForRequest', ownProps.requestId, 'data'])
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  refresh: () => dispatch(FetchActiveTasksForRequest.trigger(ownProps.requestId))
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(TaskStateBreakdown);
