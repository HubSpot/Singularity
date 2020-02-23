import React, { Component, PropTypes } from 'react';

import Utils from '../utils';
import WsTerminal, { makeWsTerminal } from './WsTerminal';
import { Terminal } from 'xterm';

class TaskLessTerminal extends Component {
  
  /** @param {Terminal} terminal */
  terminalToWebSocket(terminal) {
    this.terminalEtcSetup(terminal);

    const task = Utils.getTaskDataFromTaskId(this.props.taskId);

    // hyphens in hostnames appear to have been converted to underscores
    const host = task.host.replace(/_/g, '-');

    const url = `wss://${host}:${window.config.lessTerminalPort}/api/v1/tasks/${this.props.taskId}/exec/less?${this.getArguments(terminal)}`;
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
      console.log(this.props.offset);
      base.push(`command=${encodeURIComponent(`+${this.props.offset}`)}`);
    }

    base.push(`command=${this.props.path}`);
    return base.join('&');
  }
  
  /** @param {Terminal} terminal */
  terminalEtcSetup(terminal) {
    terminal.element.addEventListener('contextmenu', (event) => {
      console.log('contextmenu', event);
      // event.preventDefault();
      event.stopImmediatePropagation();
    });

    document.addEventListener('contextmenu', event => {
      console.log('document/contextmenu', event);
    });

    terminal.registerLinkMatcher(/^\s*(\d+)/, (event, match) => {
      console.log(event);
      console.log(match);

      // const line = match.trim();

      // const a = document.createElement('a');
      // // document.body.appendChild(a);

      // const search = new URLSearchParams(window.location.search);
      // search.set('offset', line);
      // a.href = `${window.location.origin}${window.location.pathname}?${search}`;
      // a.innerText = 'hi';
      // document.appendChild(a);
      
      // // const clone = new Event(event.type, Object.assign({}, event, { target: a }));
      // const clone = new MouseEvent('contextmenu', Object.assign({ detail: { custom: true } }, event));
      // event.preventDefault();
      // event.stopImmediatePropagation();
      // a.dispatchEvent(clone);
    }, {
      tooltipCallback: (event, uri, location) => {
        console.log('tooltipCallback', event);
        return true;
      },
      willLinkActivate: (event, match) => {
        console.log('willLinkActivate', event);
        console.log(match);

        const line = match.trim();

        const a = document.createElement('a');
        // document.body.appendChild(a);

        const search = new URLSearchParams(window.location.search);
        search.set('offset', line);
        a.href = `${window.location.origin}${window.location.pathname}?${search}`;
        a.addEventListener('click', event => {
          debugger;
        });
        // a.innerText = 'hi';
        // document.appendChild(a);
        
        // const clone = new Event(event.type, Object.assign({}, event, { target: a }));
        const clone = new MouseEvent('click', event);
        event.preventDefault();
        event.stopImmediatePropagation();
        a.dispatchEvent(clone);

        return true;
      },
    });
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
