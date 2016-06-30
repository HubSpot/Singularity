import React from 'react';
import TaskGroupHeader from './TaskGroupHeader';
import LogLines from './LogLines';
import LoadingSpinner from './LoadingSpinner';
import FileNotFound from './FileNotFound';

import { connect } from 'react-redux';

class TaskGroupContainer extends React.Component {
  getContainerWidth() {
    return 12 / this.props.taskGroupContainerCount;
  }

  renderLogLines() {
    if (this.props.logDataLoaded && this.props.fileExists) {
      return <LogLines taskGroupId={this.props.taskGroupId} />;
    }
    if (this.props.logDataLoaded) {
      return (
        <div>
          <LogLines
            taskGroupId={this.props.taskGroupId}
            fileNotFound={<FileNotFound fileName={this.props.path} noLongerExists={true} />}
          />
        </div>
      );
    }
    if (this.props.initialDataLoaded && !this.props.fileExists) {
      return <div className="tail-contents"><FileNotFound fileName={this.props.path} /></div>;
    }
    return <LoadingSpinner centered={true}>Loading logs...</LoadingSpinner>;
  }

  render() {
    let className = `col-md-${ this.getContainerWidth() } tail-column`;
    return <div className={className}><TaskGroupHeader taskGroupId={this.props.taskGroupId} />{this.renderLogLines()}</div>;
  }
}

TaskGroupContainer.propTypes = {
  taskGroupId: React.PropTypes.number.isRequired,
  taskGroupContainerCount: React.PropTypes.number.isRequired,
  path: React.PropTypes.string.isRequired,
  logDataLoaded: React.PropTypes.bool,

  initialDataLoaded: React.PropTypes.bool.isRequired,
  fileExists: React.PropTypes.bool.isRequired,
  terminated: React.PropTypes.bool.isRequired
};

const mapStateToProps = (state, ownProps) => {
  if (!(ownProps.taskGroupId in state.taskGroups)) {
    return {
      initialDataLoaded: false,
      fileExists: false,
      logDataLoaded: false,
      terminated: false
    };
  }
  const taskGroup = state.taskGroups[ownProps.taskGroupId];
  const tasks = taskGroup.taskIds.map(taskId => state.tasks[taskId]);

  return {
    initialDataLoaded: _.all(_.pluck(tasks, 'initialDataLoaded')),
    logDataLoaded: _.all(_.pluck(tasks, 'logDataLoaded')),
    fileExists: _.any(_.pluck(tasks, 'exists')),
    terminated: _.all(_.pluck(tasks, 'terminated')),
    path: state.path
  };
};

export default connect(mapStateToProps)(TaskGroupContainer);

