import React, { PropTypes, Component } from 'react';

import 'xterm-css';
import { Terminal } from 'xterm';
import { AttachAddon } from 'xterm-addon-attach';

class TaskLogTailer extends Component {
  constructor(props) {
    super(props);

    this.terminal = new Terminal();

    this.ws = new WebSocket('ws://localhost:3000/api/exec/less/attach?dist/index.js');
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
