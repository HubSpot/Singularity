import React, { Component, PropTypes } from 'react';

import Utils from '../../../utils';
import WsTerminal from './WsTerminal';

class TaskLessTerminal extends Component {
  getArguments() {
    if (this.props.offset >= 1) {
      return [`command=${encodeURIComponent(`+${this.props.offset}`)}`, `command=${this.props.path}`].join('&');
    }

    return `command=${this.props.path}`;
  }

  render() {
    const task = Utils.getTaskDataFromTaskId(this.props.taskId);

    // hyphenated hosts appear to have been converted to underscores
    const host = task.host.replace(/_/g, '-');

    return (
      <WsTerminal
        url={`wss://${host}:${window.config.lessTerminalPort}/api/v1/tasks/${this.props.taskId}/exec/less?${this.getArguments()}`}
        protocols={['Bearer', Utils.getAuthToken()]}
        onClose={this.props.onClose}
      />
    );
  }
}

TaskLessTerminal.propTypes = {
  taskId: PropTypes.string.isRequired,
  path: PropTypes.string.isRequired,
  offset: PropTypes.number,

  onClose: PropTypes.func
};

TaskLessTerminal.defaultProps = {
};

export default TaskLessTerminal;
