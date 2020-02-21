import React, { PropTypes, Component } from 'react';

import { Terminal } from 'xterm';
import { AttachAddon } from 'xterm-addon-attach';
import { FitAddon } from 'xterm-addon-fit';
import 'xterm.css';

const THEMES = {
  DEFAULT: null,
  LIGHT: {
    background: '#ffffff',
    foreground: '#000000',
  },
}

class WsTerminal extends Component {
  componentDidMount() {
    this.terminal = new Terminal();
    this.fitAddon = new FitAddon();

    this.terminal.open(this.refs.terminal);

    this.terminal.loadAddon(this.fitAddon);
    this.fitAddon.fit();

    if (this.props.openWebSocket) {
      this.ws = this.props.openWebSocket(this.terminal);
    } else {
      this.ws = new WebSocket(this.props.url, this.props.protocols);
    }

    this.wsAttach = new AttachAddon(this.ws);
    this.terminal.loadAddon(this.wsAttach);

    console.log(this.ws);
    console.log(this.fitAddon);
    console.log(this.terminal);

    // in the typical cannot connect to agent case, error is fired before close
    this.ws.addEventListener('error', event => {
      console.error(event);
    });

    this.ws.addEventListener('close', event => {
      console.log(event);
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

  openWebSocket() {
    if (this.props.openWebSocket) {
      return this.props.openWebSocket(this.terminal);
    }
  }

  render() {
    return (
      <div ref="terminal" style={{ height: '100%' }} />
    )
  }
}

WsTerminal.propTypes = {
  open: PropTypes.func,
  url: PropTypes.string,
  protocols: PropTypes.array,

  focus: PropTypes.bool,
  onClose: PropTypes.func,
};

WsTerminal.defaultProps = {
  focus: true,
};

export default WsTerminal;
