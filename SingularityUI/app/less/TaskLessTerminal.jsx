import React, { Component, PropTypes } from 'react';

import Utils from '../utils';
import WsTerminal, { makeWsTerminal } from './WsTerminal';
import { Terminal } from 'xterm';

class TaskLessTerminal extends Component {
  
  /** @param {Terminal} terminal */
  openWebSocket(terminal) {
    const task = Utils.getTaskDataFromTaskId(this.props.taskId);

    // hyphenated hosts appear to have been converted to underscores
    const host = task.host.replace(/_/g, '-');

    const url = `wss://${host}:${window.config.lessTerminalPort}/api/v1/tasks/${this.props.taskId}/exec/less?${this.getArguments(terminal)}`;
    const protocols = ['Bearer', Utils.getAuthToken()];

    return new WebSocket(url, protocols);
  }

  
  getArguments(terminal) {
    const base = [`cols=${terminal.cols}`, `rows=${terminal.rows}`];

    // enable tailing
    base.push(`command=${encodeURIComponent(`+F`)}`);

    // disable line folding/wrapping
    base.push(`command=-S`);

    // enable line numbering
    base.push(`command=-N`);

    if (this.props.offset >= 1) {
      base.push(`command=${encodeURIComponent(`+${this.props.offset}`)}`);
    }

    base.push(`command=${this.props.path}`);
    return base.join('&');
  }

  render() {
    return (
      <WsTerminal
        terminal={this.props.terminal}
        terminalToWebSocket={this.openWebSocket.bind(this)}
        {...this.props}
      />
    );
  }
}

TaskLessTerminal.propTypes = {
  terminal: PropTypes.instanceOf(Terminal).isRequired,
  taskId: PropTypes.string.isRequired,
  path: PropTypes.string.isRequired,
  offset: PropTypes.number,

  onClose: PropTypes.func
};

TaskLessTerminal.defaultProps = {
};

export default TaskLessTerminal;
