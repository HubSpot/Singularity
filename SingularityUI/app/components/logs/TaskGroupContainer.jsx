import React from 'react';
import TaskGroupHeader from './TaskGroupHeader';
import LogLines from './LogLines';
import LoadingSpinner from './LoadingSpinner';
import FileNotFound from './FileNotFound';
import { doesFinishedLogExist } from '../../actions/log';

import { connect } from 'react-redux';

class TaskGroupContainer extends React.Component {
  componentWillReceiveProps(nextProps) {
    if (nextProps.path === config.runningTaskLogPath && !nextProps.finishedLogExists && nextProps.taskIds) {
      this.props.doesFinishedLogExist(nextProps.taskIds);
    }
  }

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
            fileNotFound={<FileNotFound fileName={this.props.path} noLongerExists={true} finishedLogExists={this.props.finishedLogExists} />}
          />
        </div>
      );
    }
    if (this.props.initialDataLoaded && this.props.invalidCompression) {
      return (
        <div className="tail-contents">
          <div className="lines-wrapper">
            <div className="empty-table-message">
              <p>
                {_.last(this.props.path.split('/'))} is not compressed in a format that can be read by the Singularity log tailer
              </p>
            </div>
          </div>
        </div>
      );
    }
    if (this.props.initialDataLoaded && !this.props.fileExists) {
      return (
        <div className="tail-contents">
          <FileNotFound fileName={this.props.path} finishedLogExists={this.props.finishedLogExists} />
        </div>
      );
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
  path: React.PropTypes.string,
  logDataLoaded: React.PropTypes.bool,

  initialDataLoaded: React.PropTypes.bool.isRequired,
  fileExists: React.PropTypes.bool.isRequired,
  invalidCompression: React.PropTypes.bool.isRequired,
  terminated: React.PropTypes.bool.isRequired,
  finishedLogExists: React.PropTypes.bool,
  doesFinishedLogExist: React.PropTypes.func.isRequired,
  taskIds: React.PropTypes.arrayOf(React.PropTypes.string)
};

const mapStateToProps = (state, ownProps) => {
  if (!(ownProps.taskGroupId in state.taskGroups)) {
    return {
      initialDataLoaded: false,
      fileExists: false,
      logDataLoaded: false,
      terminated: false,
      invalidCompression: false
    };
  }
  const taskGroup = state.taskGroups[ownProps.taskGroupId];
  const tasks = taskGroup.taskIds.map(taskId => state.tasks[taskId]);

  return {
    initialDataLoaded: _.all(_.pluck(tasks, 'initialDataLoaded')),
    logDataLoaded: _.all(_.pluck(tasks, 'logDataLoaded')),
    fileExists: _.any(_.pluck(tasks, 'exists')),
    invalidCompression: _.any(_.pluck(tasks, 'invalidCompression')),
    finishedLogExists: _.any(_.pluck(tasks, 'taskFinishedLogExists')),
    terminated: _.all(_.pluck(tasks, 'terminated')),
    path: state.path,
    taskIds: taskGroup.taskIds
  };
};

const mapDispatchToProps = { doesFinishedLogExist };

export default connect(mapStateToProps, mapDispatchToProps)(TaskGroupContainer);
