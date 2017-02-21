import React from 'react';
import classNames from 'classnames';
import { TailerProvider, SandboxTailer } from 'singularityui-tailer';
import NewTaskGroupHeader from '../components/logs/NewTaskGroupHeader';
import NewHeader from '../components/logs/NewHeader';

import { connect } from 'react-redux';

import { loadColor, removeTailerGroup, pickTailerGroup, jumpToBottom, jumpToTop } from '../actions/tailer';

const prefixedLineLinkRenderer = (taskId, path) => ({start}) => {
  return (<a
    href={`${ config.appRoot }/task/${taskId}/tail/${path}?offset=${start}`}
    className="offset-link"
  >
    <div className="pre-line">
      <span className="glyphicon glyphicon-link" />
    </div>
  </a>);
}

class LogTailerContainer extends React.PureComponent {
  componentWillMount() {
    this.props.loadColor();
  }

  render() {
    const renderTailerPane = (tasks, key) => {
      const {taskId, path, offset, tailerId} = tasks[0];

      return (<section className="log-pane" key={key}>
        <NewTaskGroupHeader
          taskId={taskId}
          showRequestId={this.props.requestIds.length > 1}
          showCloseAndExpandButtons={this.props.tailerGroups.length > 1}
          onClose={() => this.props.removeTailerGroup(key)}
          onExpand={() => this.props.pickTailerGroup(key)}
          onJumpToTop={() => this.props.jumpToTop(tailerId, taskId, path)}
          onJumpToBottom={() => this.props.jumpToBottom(tailerId, taskId, path)} />
        <SandboxTailer
          goToOffset={parseInt(offset)}
          tailerId={tailerId}
          taskId={taskId}
          path={path.replace('$TASK_ID', taskId)}
          lineLinkRenderer={prefixedLineLinkRenderer(taskId, path)} />
      </section>);
    };

    return (
      <TailerProvider getTailerState={(state) => state.tailer}>
        <div className={classNames(['new-tailer', 'tail-root', this.props.color])}>
          <NewHeader />
          <div className="row tail-row">
            {this.props.tailerGroups.map(renderTailerPane)}
          </div>
        </div>
      </TailerProvider>
    );
  }
}

export default connect((state) => ({
  tailerGroups: state.tailerView.tailerGroups,
  requestIds: state.tailerView.requestIds,
  color: state.tailerView.color
}), {
  loadColor,
  removeTailerGroup,
  pickTailerGroup,
  jumpToBottom,
  jumpToTop,
})(LogTailerContainer);
