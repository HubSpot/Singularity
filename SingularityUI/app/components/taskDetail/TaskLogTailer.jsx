import React, { PropTypes, Component } from 'react';
import Utils from '../../utils';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { Glyphicon, Tooltip } from 'react-bootstrap';

import { Terminal } from 'xterm';
import { AttachAddon } from 'xterm-addon-attach';

class TaskLogTailer extends Component {
  constructor(props) {
    super(props);

    this.terminal = new Terminal();
    this.ws = new WebSocket('ws://localhost:3000/api/exec/less');
    this.wsAttach = new AttachAddon(this.ws);
  }

  componentDidMount() {
    this.terminal.loadAddon(this.wsAttach);
    this.terminal.open(this.refs.terminal);
  }

  render() {
    return (
      <div ref="terminal">
      </div>
    )
  }
}

TaskLogTailer.propTypes = {
};

export default TaskLogTailer;
