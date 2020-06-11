import React, { PropTypes, Component } from 'react';

import { Terminal } from 'xterm';
import { AttachAddon } from './TaskLessAttachAddon';
import 'xterm.css';
import Utils from '../../utils';

class TaskLessTerminal extends Component {
  constructor(props) {
    super(props);

    this.terminal = new Terminal();

    this.ws = new WebSocket(`wss://${this.props.host}:${this.props.port}/api/v1/tasks/${this.props.task}/exec/less?command=${this.props.path}`, ['Bearer', Utils.getAuthToken()]);
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
    this.terminal.focus();
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
  path: PropTypes.string.isRequired,
  host: PropTypes.string.isRequired,
  port: PropTypes.number.isRequired,
  task: PropTypes.string.isRequired,
  onClose: PropTypes.func,
};

export default TaskLessTerminal;
