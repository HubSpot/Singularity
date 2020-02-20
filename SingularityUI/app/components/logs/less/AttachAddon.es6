/**
 * Copyright (c) 2014, 2019 The xterm.js authors. All rights reserved.
 * @license MIT
 *
 * Implements the attach method, that attaches the terminal to a WebSocket stream.
 * 
 * Ported to JS as a temporary workaround for webpack build errors, and should not reach production.
 * 
 * Original: https://github.com/xtermjs/xterm.js/blob/master/addons/xterm-addon-attach/src/AttachAddon.ts
 */

export class AttachAddon {
  _socket;
  _bidirectional;
  _disposables = [];

  constructor(socket, options) {
    this._socket = socket;
    // always set binary type to arraybuffer, we do not handle blobs
    this._socket.binaryType = 'arraybuffer';
    this._bidirectional = (options && options.bidirectional === false) ? false : true;
  }

  activate(terminal) {
    this._disposables.push(
      addSocketListener(this._socket, 'message', ev => {
        const data = ev.data;
        terminal.write(typeof data === 'string' ? data : new Uint8Array(data));
      })
    );

    if (this._bidirectional) {
      this._disposables.push(terminal.onData(data => this._sendData(data)));
      this._disposables.push(terminal.onBinary(data => this._sendBinary(data)));
    }

    this._disposables.push(addSocketListener(this._socket, 'close', () => this.dispose()));
    this._disposables.push(addSocketListener(this._socket, 'error', () => this.dispose()));
  }

  dispose() {
    this._disposables.forEach(d => d.dispose());
  }

  _sendData(data) {
    // TODO: do something better than just swallowing
    // the data if the socket is not in a working condition
    if (this._socket.readyState !== 1) {
      return;
    }
    this._socket.send(data);
  }

  sendBinary(data) {
    if (this._socket.readyState !== 1) {
      return;
    }
    const buffer = new Uint8Array(data.length);
    for (let i = 0; i < data.length; ++i) {
      buffer[i] = data.charCodeAt(i) & 255;
    }
    this._socket.send(buffer);
  }
}

function addSocketListener(socket, type, handler) {
  socket.addEventListener(type, handler);
  return {
    dispose: () => {
      if (!handler) {
        // Already disposed
        return;
      }
      socket.removeEventListener(type, handler);
    }
  };
}