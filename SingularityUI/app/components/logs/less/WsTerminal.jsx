import React, { PropTypes, Component } from 'react';

import { Terminal } from 'xterm';
import { AttachAddon } from 'xterm-addon-attach';
import { FitAddon } from 'xterm-addon-fit';
import 'xterm.css';

class WsTerminal extends Component {
  constructor(props) {
    super(props);

    this.terminal = new Terminal();

    this.ws = new WebSocket(this.props.url, this.props.protocols);
    this.wsAttach = new AttachAddon(this.ws);
    this.wsFit = new FitAddon();

    console.debug(this.ws);
    console.debug(this.terminal);

    this.ws.addEventListener('close', event => {
      this.terminal.dispose();
      
      if (this.props.onClose) {
        this.props.onClose(event);
      }
    });

    this.ws.addEventListener('error', event => {
      console.error(event);
    });
  }

  componentDidMount() {
    this.terminal.loadAddon(this.wsAttach);
    this.terminal.loadAddon(this.wsFit);
    this.terminal.open(this.refs.terminal);

    this.wsFit.fit();
    this.terminal.focus();
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

  key: PropTypes.string,
  onClose: PropTypes.func,
};

export default WsTerminal;
