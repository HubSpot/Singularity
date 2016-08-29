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

const taskStateProps = (state, numberInState, total) => {
  const numberString = `${numberInState}/${total}`;
  switch (state) {
    case 'TASK_LAUNCHED':
      return { bsStyle: 'info', label: `launched ${numberString}`, striped: true};
    case 'TASK_STAGING':
      return { bsStyle: 'info', label: `staging ${numberString}`, striped: true};
    case 'TASK_STARTING':
      return { bsStyle: 'info', label: `starting ${numberString}`, striped: true, active: true};
    case 'TASK_RUNNING':
      return { bsStyle: 'success', label: `running ${numberString}`};
    case 'TASK_CLEANING':
      return { bsStyle: 'warning', label: `cleaning ${numberString}`, striped: true, active: true};
    case 'TASK_KILLING':
      return { bsStyle: 'danger', label: `killing ${numberString}`, striped: true, active: true};
    case 'TASK_FINISHED':
      return { bsStyle: 'success', label: `finished ${numberString}`};
    case 'TASK_FAILED':
      return { bsStyle: 'danger', label: `failed ${numberString}`};
    case 'TASK_KILLED':
      return { bsStyle: 'danger', label: `killed ${numberString}`};
    case 'TASK_LOST':
      return { bsStyle: 'danger', label: `lost ${numberString}`};
    case 'TASK_LOST_WHILE_DOWN':
      return { bsStyle: 'danger', label: `singularity lost ${numberString}`};
    case 'TASK_ERROR':
      return { bsStyle: 'danger', label: `error ${numberString}`};
    default:
      return { bsStyle: 'danger', label: `unknown ${numberString}`};
  }
};

const TaskStateBreakdown = ({activeTasksForRequest, refresh}) => {
  if (!activeTasksForRequest) {
    return null;
  }

  const instanceBreakdown = Utils.task.instanceBreakdown(activeTasksForRequest);
  const totalInstances = taskStateOrder.reduce((last, current) => (
    last + (instanceBreakdown[current] || 0)
  ), 0);

  if (totalInstances === 0) {
    return null;
  }

  const taskStateProgressBars = taskStateOrder.map((state) => {
    const percentage = 100 * (instanceBreakdown[state] / totalInstances);
    const progressProps = taskStateProps(
      state,
      instanceBreakdown[state],
      totalInstances
    );
    return <ProgressBar key={state} now={percentage} {...progressProps} />;
  });


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
