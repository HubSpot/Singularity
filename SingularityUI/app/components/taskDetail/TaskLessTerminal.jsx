import React, { PropTypes, Component } from 'react';

import { Terminal } from 'xterm';
import { AttachAddon } from 'xterm-addon-attach';
import 'xterm.css';
import Utils from '../../utils';

class TaskLessTerminal extends Component {
  constructor(props) {
    super(props);

    this.terminal = new Terminal();

    this.ws = new WebSocket(`wss://${this.props.host}:4141/api/exec/less/attach?${this.props.file}`, ['Bearer', Utils.getAuthToken()]);
    this.wsAttach = new AttachAddon(this.ws);

    console.log(this.ws);
    console.log(this.terminal);

    this.ws.addEventListener('close', event => {
      this.terminal.dispose();
      
      if (this.props.onClose) {
        this.props.onClose(event);
      }
    });
  }

  componentDidMount() {
    this.terminal.loadAddon(this.wsAttach);
    this.terminal.open(this.refs.terminal);
  }

  componentWillUnmount() {
    this.terminal.dispose();
    this.ws.close();
  }

  render() {
    return (
      <div ref="terminal" />
    )
  }
}

TaskLessTerminal.propTypes = {
  file: PropTypes.string.isRequired,
  host: PropTypes.string.isRequired,
  onClose: PropTypes.func,
};

export default TaskLessTerminal;
