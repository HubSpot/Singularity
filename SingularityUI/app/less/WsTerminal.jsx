import React, { PropTypes, Component } from 'react';

import { Terminal } from 'xterm';
import { AttachAddon } from 'xterm-addon-attach';
import { FitAddon } from 'xterm-addon-fit';
import { WebglAddon } from 'xterm-addon-webgl';
import { WebLinksAddon } from 'xterm-addon-web-links';
import 'xterm.css';

class WsTerminal extends Component {
  componentDidMount() {
    /** @type {Terminal} */
    this.terminal = this.props.terminal || new Terminal();
    this.terminal.open(this.refs.terminal);

    this.webglAddon = new WebglAddon();
    this.terminal.loadAddon(this.webglAddon);

    this.weblinkAddon = new WebLinksAddon();
    this.terminal.loadAddon(this.weblinkAddon);

    this.fitAddon = new FitAddon();
    this.terminal.loadAddon(this.fitAddon);
    this.fitAddon.fit();
    // this.terminal.resize(1024, this.terminal.rows);

    this.ws = this.props.terminalToWebSocket(this.terminal);
    this.wsAttach = new AttachAddon(this.ws);
    this.terminal.loadAddon(this.wsAttach);

    // in the typical cannot connect to agent case, error is fired before close
    this.ws.addEventListener('error', event => {
      console.error(event);
    });

    this.ws.addEventListener('close', event => {
      // this.terminal.dispose();

      if (event.code === 1000) {
        this.terminal.writeln(`Session closed successfully.`);
        this.terminal.writeln(`WebSocket closed with code: ${event.code}`);
        this.terminal.writeln(event.reason);
      } else {
        this.terminal.writeln(`Session could not be opened, or closed unexpectedly.`);
        this.terminal.writeln(`WebSocket closed with code: ${event.code}`);
        this.terminal.writeln(event.reason);
      }
      
      if (this.props.onClose) {
        this.props.onClose(event);
      }
    });

    if (this.props.focus) {
      this.terminal.focus();
    }
  }

  componentWillUnmount() {
    this.terminal.dispose();
    this.ws.close();
  }

  render() {
    return (
      <div ref="terminal" style={{ height: '100%' }} />
    )
  }
}

WsTerminal.propTypes = {
  terminal: PropTypes.instanceOf(Terminal),
  terminalToWebSocket: PropTypes.func.isRequired,

  focus: PropTypes.bool,
  onClose: PropTypes.func,
};

WsTerminal.defaultProps = {
  focus: true,
};

export default WsTerminal;
