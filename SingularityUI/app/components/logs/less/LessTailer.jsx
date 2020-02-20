import React, { Component, PropTypes } from 'react';

import TaskLessTerminal from './TaskLessTerminal';
import Utils from '../../../utils';

class LessTailer extends Component {
  render() {
    const task = Utils.getTaskDataFromTaskId(this.props.taskId);
    const host = task.host.replace(/_/g, '-');

    return (
      <TaskLessTerminal
        host={host}
        port={window.config.lessTerminalPort}
        task={this.props.taskId}
        path={this.props.path}
      />
    );
  }
}

LessTailer.propTypes = {
  taskId: PropTypes.string.isRequired,
  path: PropTypes.string.isRequired,
};

LessTailer.defaultProps = {
};

export default LessTailer;
