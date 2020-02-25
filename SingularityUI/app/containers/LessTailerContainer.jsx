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
import { Glyphicon } from 'react-bootstrap';
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
      });
    });

    this.state = {
      terminals,
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
