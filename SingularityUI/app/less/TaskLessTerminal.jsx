import React, { Component, PropTypes } from 'react';
import { withRouter } from 'react-router';
import Messenger from 'messenger';
import { Terminal } from 'xterm';

import Utils from '../utils';
import WsTerminal from './WsTerminal';
import { disableLineNumbers, chain, toggleLineWrapping, horizontalScroll } from './commands';


class TaskLessTerminal extends Component {
  
  /** @param {Terminal} terminal */
  terminalToWebSocket(terminal) {
    this.terminalEtcSetup(terminal);

    const task = Utils.getTaskDataFromTaskId(this.props.taskId);

    // hyphens in hostnames appear to have been converted to underscores
    const host = task.host.replace(/_/g, '-');

    const api = window.config.lessTerminalPath.replace('$HOST', host).replace('$TASK', this.props.taskId);
    const url = `${api}?${this.getArguments(terminal)}`;
    const protocols = ['Bearer', Utils.getAuthToken()];

    const ws = new WebSocket(url, protocols);
    const wsRedirect = (event) => {
      if (event.code === 1000) {
        this.props.router.push(`/task/${this.props.taskId}`);

        Messenger().info({
          message: `Websocket session closed successfully.`,
          hideAfter: 3,
        });
      }
    };
    
    // order of these two statements ensures the redirect isn't removed on pageload
    this.props.router.listen((location, action) => {
      ws.removeEventListener('close', wsRedirect);
    });
    ws.addEventListener('close', wsRedirect);

    return ws;
  }

  /** @param {Terminal} terminal */
  getArguments(terminal) {
    const search = new URLSearchParams(window.location.search);
    search.set('cols', terminal.cols);
    search.set('rows', terminal.rows);

    const commands = search.getAll('command');

    // disable line folding/wrapping
    if (!commands.includes('-s')) {
      commands.unshift('-S');
    }

    // +F makes the terminal unusable on mobile
    commands.unshift('+G');

    // enable line numbering, if line calculation is enabled
    // if (!commands.includes('-n')) {
    //   commands.push('-N');
    // }

    // custom prompt, so we actually have enough data to kinda link to things
    // line/percent/byte (all of top line)
    // line will be '?' if the -n flag was specified
    // byte should be present as long as we're tailing a file
    // percent is always calculated by bytes, because this is enough of a pain as is
    commands.unshift('?eEND .%lt/%pt\\%/%btb');
    commands.unshift('-P');

    if (search.get('byteOffset')) {
      commands.push(`+${search.get('byteOffset')}P`);
    } else if (this.props.offset >= 1) {
      commands.push(`+${this.props.offset}`);
    }

    commands.push(this.props.path);
    
    for (let i = 0; i < commands.length; i++) {
      search.append('command', commands[i]);
    }

    return search.toString();
  }
  
  /** @param {Terminal} terminal */
  terminalEtcSetup(terminal) {
    const inlineNumberRegex = /^\s+(\d+)/;
    const promptRegex = /^(END )?([?\d]+)\/([?\d]+)%\/(\d+)b/;

    // terminal.onSelectionChange(() => {
    //   const selection = terminal.getSelection();
    //   const sp = terminal.getSelectionPosition();
    //   console.log(selection);
    //   console.log(sp);

    //   if (sp && sp.endColumn - sp.startColumn === terminal.cols && inlineNumberRegex.test(selection)) {
    //     console.log('we got a line to copy');
    //     chain(terminal, [disableLineNumbers, toggleLineWrapping]);
    //   }
    // });

    terminal.onKey(e => {
      console.log(e);
    });

    // horizontal scroll with mouse
    terminal.element.addEventListener('wheel', event => {
      horizontalScroll(terminal, event);
    });

    // setup prompt link
    terminal.registerLinkMatcher(promptRegex, (event, match) => {
      const byteOffset = promptRegex.exec(match)[4];

      const search = new URLSearchParams(window.location.search);
      search.set('byteOffset', byteOffset);

      const url = `${window.location.origin}${window.location.pathname}?${search}`;
      navigator.clipboard.writeText(url);
      
      Messenger().info({
        message: `Copied link to current top line to clipboard.`,
        hideAfter: 3,
      });
    });

    // setup line number links
    terminal.registerLinkMatcher(inlineNumberRegex, (event, match) => {
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

export default withRouter(TaskLessTerminal);
