import React from 'react';
import classNames from 'classnames';
import { TailerProvider } from 'singularityui-tailer';
import NewTaskGroupHeader from '../components/logs/NewTaskGroupHeader';
import NewHeader from '../components/logs/NewHeader';
import FileNotFound from '../components/logs/FileNotFound';
import { Link, withRouter } from 'react-router';
import { connect } from 'react-redux';
import ReactDOM from 'react-dom';
import { actions as tailerActions } from 'singularityui-tailer';
import { Glyphicon, Modal } from 'react-bootstrap';
import Utils from '../utils';

import { loadColor, removeTailerGroup, pickTailerGroup, markNotFound, clearNotFound } from '../actions/tailer';
import { Terminal } from 'xterm';
import TaskLessTerminal from '../less/TaskLessTerminal';
import { THEMES } from '../less/themes.es6';
import { jumpToBottom, jumpToTop } from '../less/commands.es6';

class LessTailerContainer extends React.Component {

  constructor(props) {
    super(props);

    /** @type {Terminal[]} */
    const terminals = this.props.tailerGroups.map(group => {
      return new Terminal({
        theme: THEMES[this.props.color],
        fontSize: 12
      });
    });

    this.state = {
      terminals,
      showHelp: false,
    };
  }

  componentWillMount() {
    this.props.loadColor();
    this.props.clearNotFound();
  }

  componentDidUpdate(prevProps) {
    if (prevProps.color !== this.props.color) {
      this.state.terminals.forEach(terminal => {
        terminal.setOption('theme', THEMES[this.props.color])
      });
    }
  }

  renderHelpComponent() {
    return (
      <a className="action-link" onClick={() => this.setState({ showHelp: true })} title="Help">
        <Glyphicon glyph="question-sign" />
        <Modal show={this.state.showHelp} onHide={() => this.setState({ showHelp: false })}>
          <Modal.Body>
            <h4>
              Less help
            </h4>
            <p>
              Copy a link to the top line of your terminal by clicking on the prompt, if not currently tailing.
            </p>
            <h5>
              Common commands (see <a href="https://linux.die.net/man/1/less">man pages</a> for more):
            </h5>
            <ul>
              {this.renderHelpCommand('g', 'Scroll to top')}
              {this.renderHelpCommand('G', 'Scroll to bottom')}
              {this.renderHelpCommand('+50p', 'Jump to 50% through the file (by file size, not line count)')}
              {this.renderHelpCommand('/', 'Search forward')}
              {this.renderHelpCommand('?', 'Search backward')}
              {this.renderHelpCommand('&', 'Enable match-only search (pass an empty search string to disable)')}
              {this.renderHelpCommand('n', 'Jump to next match')}
              {this.renderHelpCommand('N', 'Jump to previous match')}
              {this.renderHelpCommand('-S', 'Toggle line wrapping (allows copy/paste of long lines)')}
              {this.renderHelpCommand('-N', 'Toggle visible line numbers')}
              {this.renderHelpCommand('+F', 'Tail the file')}
            </ul>
          </Modal.Body>
        </Modal>
      </a>
    );
  }

  renderHelpCommand(command, description) {
    return (
      <li><code>{command}</code> {description}</li>
    );
  }

  render() {
    const renderTailerPane = (tasks, key) => {
      const {taskId, path, offset, tailerId} = tasks[0];
      const terminal = this.state.terminals[key]

      if (Utils.maybe(this.props.notFound, [taskId], false)) {
        const fileName = Utils.fileName(path);

        let toTailOfFinished;
        const pathWithTaskId = Utils.substituteTaskId(config.runningTaskLogPath, taskId);
        if (path === pathWithTaskId) {
          const tailOfLogPath = Utils.tailerPath(taskId, path.replace(pathWithTaskId, Utils.substituteTaskId(config.finishedTaskLogPath, taskId)));
          toTailOfFinished = (
            <p>
              It may have been moved to <Link onClick={this.forceUpdate} to={`${tailOfLogPath}`}>{Utils.fileName(config.finishedTaskLogPath)}</Link>
            </p>
          );
        }

        return (
          <section className="log-pane" key={key}>
            <div className="row tail-row tail-row-centered">
                <div className="not-found-message">
                  <p>
                    {fileName} does not exist in this directory.
                  </p>
                  {toTailOfFinished}
                  <Link to={`/task/${taskId}`}>
                    <Glyphicon glyph="arrow-left" /> Back to Task Detail Page
                  </Link>
                </div>
            </div>
          </section>
        );
      } else {
        return (
          <section className="log-pane" key={key} style={{
            height: '100%',
            padding: 0,
            background: 'default !important',
            color: 'default !important',
          }}>
            <NewTaskGroupHeader
              taskId={taskId}
              showRequestId={this.props.requestIds.length > 1}
              showCloseAndExpandButtons={this.props.tailerGroups.length > 1}
              onClose={() => this.props.removeTailerGroup(key)}
              onExpand={() => this.props.pickTailerGroup(key)}
              onJumpToTop={() => jumpToTop(terminal)}
              onJumpToBottom={() => jumpToBottom(terminal)}
              helpComponent={this.renderHelpComponent()}
            />
            <TaskLessTerminal terminal={terminal} taskId={taskId} path={path} offset={parseInt(offset)} />
          </section>
        );
      }
    };

    return (
      <TailerProvider getTailerState={(state) => state.tailer}>
        <div className={classNames(['new-tailer', 'tail-root', this.props.color])}>
          <NewHeader
            oldTail='tail'
            jumpAllToTop={() => this.state.terminals.forEach(jumpToTop)}
            jumpAllToBottom={() => this.state.terminals.forEach(jumpToBottom)}
          />
          <div className="row tail-row">
            {this.props.tailerGroups.map(renderTailerPane)}
          </div>
        </div>
      </TailerProvider>
    );
  }
}

export default withRouter(connect((state) => ({
  tailerGroups: state.tailerView.tailerGroups,
  requestIds: state.tailerView.requestIds,
  color: state.tailerView.color,
  notFound: state.tailerView.notFound
}), {
  loadColor,
  removeTailerGroup,
  pickTailerGroup,
  jumpToBottom,
  jumpToTop,
  markNotFound,
  clearNotFound
})(LessTailerContainer));
