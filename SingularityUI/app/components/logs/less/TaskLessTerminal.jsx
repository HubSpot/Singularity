import React, { PropTypes, Component } from 'react';

import WsTerminal from './WsTerminal';
import Utils from '../../../utils';

class TaskLessTerminal extends Component {
  render() {
    console.log('TaskLessTerminal', this.props);
    
    return (
      <WsTerminal
        url={`wss://${this.props.host}:${this.props.port}/api/v1/tasks/${this.props.task}/exec/less?command=${this.props.path}`}
        protocols={['Bearer', Utils.getAuthToken()]}
        onClose={this.props.onClose}
      />
    )
  }
}

TaskLessTerminal.propTypes = {
  path: PropTypes.string.isRequired,
  host: PropTypes.string.isRequired,
  port: PropTypes.number.isRequired,
  task: PropTypes.string.isRequired,
  onClose: PropTypes.func,
};

export default TaskLessTerminal;
