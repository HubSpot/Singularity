import React, { Component, PropTypes } from 'react';

import Utils from '../../../utils';
import WsTerminal from './WsTerminal';

class LessTerminal extends Component {
  render() {
    const task = Utils.getTaskDataFromTaskId(this.props.taskId);
    const host = task.host.replace(/_/g, '-');

    return (
      <WsTerminal
        url={`wss://${host}:${window.config.lessTerminalPort}/api/v1/tasks/${this.props.taskId}/exec/less?command=${this.props.path}`}
        protocols={['Bearer', Utils.getAuthToken()]}
        onClose={this.props.onClose}
      />
    );
  }
}

LessTerminal.propTypes = {
  taskId: PropTypes.string.isRequired,
  path: PropTypes.string.isRequired,
  onClose: PropTypes.func
};

LessTerminal.defaultProps = {
};

export default LessTerminal;
