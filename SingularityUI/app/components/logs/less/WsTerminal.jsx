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
  constructor(props) {
    super(props);

    this.terminal = new Terminal();

    this.ws = new WebSocket(this.props.url, this.props.protocols);
    this.wsAttach = new AttachAddon(this.ws);
    this.wsFit = new FitAddon();

    console.log(this.ws);
    console.log(this.wsFit);
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
        this.terminal.writeln(`Websocket closed with code: ${event.code}`);
        this.terminal.writeln(event.reason);
      } else {
        this.terminal.writeln(`Session could not be created or closed unexpectedly.`);
        this.terminal.writeln(`Websocket closed with code: ${event.code}`);
        this.terminal.writeln(event.reason);
      }
      
      if (this.props.onClose) {
        this.props.onClose(event);
      }
    });
  }

  componentDidMount() {
    this.terminal.loadAddon(this.wsAttach);
    this.terminal.loadAddon(this.wsFit);
    this.terminal.open(this.refs.terminal);

    console.log(this.terminal.rows);
    console.log(this.terminal.cols);
    this.wsFit.fit();
    // this.terminal.resize(120, 20)
    console.log(this.terminal.rows);
    console.log(this.terminal.cols)

    // setTimeout(() => this.terminal.loadAddon(this.wsAttach), 1000)

    // this.terminal.loadAddon(this.wsAttach);

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
  url: PropTypes.string.isRequired,
  protocols: PropTypes.array.isRequired,

  focus: PropTypes.bool,
  onClose: PropTypes.func,
};

WsTerminal.defaultProps = {
  focus: true,
};

export default WsTerminal;
