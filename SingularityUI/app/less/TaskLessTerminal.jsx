import React, { Component, PropTypes } from 'react';
import Messenger from 'messenger';

import Utils from '../utils';
import WsTerminal from './WsTerminal';
import { sigint } from '../less/commands';
import { Terminal } from 'xterm';

class TaskLessTerminal extends Component {
  
  /** @param {Terminal} terminal */
  terminalToWebSocket(terminal) {
    this.terminalEtcSetup(terminal);

    const task = Utils.getTaskDataFromTaskId(this.props.taskId);

    // hyphens in hostnames appear to have been converted to underscores
    const host = task.host.replace(/_/g, '-');

    const url = `wss://${host}:${window.config.lessTerminalPort}/api/v1/tasks/${this.props.taskId}/exec/less?${this.getArguments(terminal)}`;
    console.log(Utils.getAuthToken())
    const protocols = ['Bearer', Utils.getAuthToken()];

    return new WebSocket(url, protocols);
  }

  getArguments(terminal) {
    const base = [`cols=${terminal.cols}`, `rows=${terminal.rows}`];

    // start tailing by default
    // base.push(`command=${encodeURIComponent(`+F`)}`);

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
  
  /** @param {Terminal} terminal */
  terminalEtcSetup(terminal) {
    terminal.onKey(key => {
      console.log('key', key.domEvent.co);
    });

    terminal.onSelectionChange(() => {
      const selection = terminal.getSelection();
      const sp = terminal.getSelectionPosition();
      console.log(selection);
      console.log(sp);

      if (sp && sp.endColumn - sp.startColumn === terminal.cols) {
        console.log('we got a line to copy');

        // sigint(terminal);
        // terminal.paste('\r-S');

        // terminal.write('\rhi')

        // while (selection.length === terminal.cols && selection[selection.length - 1] === '>') {
        //   // terminal.paste('OC');
        //   // terminal.write('\x1b[D');
        // }
      }
    });

    // setup line number link support
    terminal.registerLinkMatcher(/^\s*(\d+)/, (event, match) => {
      const line = match.trim();

      const search = new URLSearchParams(window.location.search);
      search.set('offset', line);

      const url = `${window.location.origin}${window.location.pathname}?${search}`;
      navigator.clipboard.writeText(url);
      
      Messenger().info({
        message: `Copied link to line ${line} to clipboard.`,
        hideAfter: 3,
      });
    }, {});
  }

  render() {
    return (
      <WsTerminal
        terminal={this.props.terminal}
        terminalToWebSocket={this.terminalToWebSocket.bind(this)}
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
