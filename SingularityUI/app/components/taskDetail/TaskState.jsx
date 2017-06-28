import React, { PropTypes } from 'react';
import Utils from '../../utils';
import TaskStatus from './TaskStatus';

const labelText = (status, currentState, cleanupType) => {
  if (status === TaskStatus.NEVER_RAN) {
    return 'Task aborted';
  } else if (cleanupType) {
    return `${Utils.humanizeText(currentState)} (${Utils.humanizeText(cleanupType)})`;
  }

  return Utils.humanizeText(currentState);
};

const labelClass = (status, currentState) => {
  if (status === TaskStatus.NEVER_RAN) {
    return 'info';
  }

  return Utils.getLabelClassFromTaskState(currentState);
};

const TaskState = ({status, updates, cleanupType}) => {
  if (updates) {
    const currentState = _.last(updates).taskState;
    return (
      <div className="col-xs-6 task-state-header">
        <h1>
          <span className={`label label-${labelClass(status, currentState)} task-state-header-label`}>
            {labelText(status, currentState, cleanupType)}
          </span>
        </h1>
      </div>
    );
  }

  return null;
};

TaskState.propTypes = {
  status: PropTypes.oneOf([TaskStatus.RUNNING, TaskStatus.STOPPED, TaskStatus.NEVER_RAN]),
  updates: PropTypes.arrayOf(PropTypes.shape({
    taskState: PropTypes.string
  })),
  cleanupType: PropTypes.string,
};

export default TaskState;
