import React from 'react';
import TaskGroupHeader from './TaskGroupHeader';
import LogLines from './LogLines';
import LoadingSpinner from './LoadingSpinner';
import FileNotFound from './FileNotFound';
import classNames from 'classnames';

import { connect } from 'react-redux';

class TaskGroupContainer extends React.Component {
  getContainerWidth() {
    return 12 / this.props.taskGroupContainerCount;
  }

  renderLogLines() {
    if (this.props.logDataLoaded) {
      return <LogLines taskGroupId={this.props.taskGroupId} />;
    } else if (this.props.initialDataLoaded && !this.props.fileExists) {
      return <div className="tail-contents"><FileNotFound fileName={this.props.path} /></div>;
    } else {
      return <LoadingSpinner centered={true}>Loading logs...</LoadingSpinner>;
    }
  }

  render() {
    let className = `col-md-${ this.getContainerWidth() } tail-column`;
    return <div className={className}><TaskGroupHeader taskGroupId={this.props.taskGroupId} />{this.renderLogLines()}</div>;
  }
}

TaskGroupContainer.propTypes = {
  taskGroupId: React.PropTypes.number.isRequired,
  taskGroupContainerCount: React.PropTypes.number.isRequired,

  initialDataLoaded: React.PropTypes.bool.isRequired,
  fileExists: React.PropTypes.bool.isRequired,
  terminated: React.PropTypes.bool.isRequired
};

let mapStateToProps = function (state, ownProps) {
  if (!(ownProps.taskGroupId in state.taskGroups)) {
    return {
      initialDataLoaded: false,
      fileExists: false,
      logDataLoaded: false,
      terminated: false
    };
  }
  let taskGroup = state.taskGroups[ownProps.taskGroupId];
  let tasks = taskGroup.taskIds.map(taskId => state.tasks[taskId]);

  return {
    initialDataLoaded: _.all(_.pluck(tasks, 'initialDataLoaded')),
    logDataLoaded: _.all(_.pluck(tasks, 'logDataLoaded')),
    fileExists: _.any(_.pluck(tasks, 'exists')),
    terminated: _.all(_.pluck(tasks, 'terminated')),
    path: state.path
  };
};

export default connect(mapStateToProps)(TaskGroupContainer);

