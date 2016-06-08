import React from 'react';
import TaskStatusIndicator from './TaskStatusIndicator';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getTaskDataFromTaskId } from '../../utils';

import { connect } from 'react-redux';

import { removeTaskGroup, expandTaskGroup, scrollToTop, scrollToBottom } from '../../actions/log';

class TaskGroupHeader extends React.Component {
  toggleLegend() {}
  // TODO

  getInstanceNoToolTip(taskData) {
    return <ToolTip id={taskData.id}>Deploy ID: {taskData.deployId}<br />Host: {taskData.host}</ToolTip>;
  }

  renderInstanceInfo() {
    if (this.props.tasks.length > 1) {
      return <span className="instance-link">Viewing Instances {this.props.tasks.map(({ taskId }) => getTaskDataFromTaskId(taskId).instanceNo).join(', ')}</span>;
    } else if (this.props.tasks.length > 0) {
      let taskData = getTaskDataFromTaskId(this.props.tasks[0].taskId);
      return <span><div className="width-constrained"><OverlayTrigger placement='bottom' overlay={this.getInstanceNoToolTip(taskData)}><a className="instance-link" href={`${ config.appRoot }/task/${ this.props.tasks[0].taskId }`}>Instance {taskData.instanceNo}</a></OverlayTrigger></div><TaskStatusIndicator status={this.props.tasks[0].lastTaskStatus} /></span>;
    } else {
      return <div className="width-constrained" />;
    }
  }

  renderTaskLegend() {
    if (this.props.tasks.length > 1) {
      return <span className="right-buttons"><a className="action-link" onClick={this.toggleLegend}><span className="glyphicon glyphicon-menu-hamburger" /></a></span>;
    }
  }

  renderClose() {
    if (this.props.taskGroupsCount > 1) {
      return React.createElement("a", { "className": "action-link", ["onClick"]: () => this.props.removeTaskGroup(this.props.taskGroupId), "title": "Close Task" }, <span className="glyphicon glyphicon-remove" />);
    }
  }

  renderExpand() {
    if (this.props.taskGroupsCount > 1) {
      return React.createElement("a", { "className": "action-link", ["onClick"]: () => this.props.expandTaskGroup(this.props.taskGroupId), "title": "Show only this Task" }, <span className="glyphicon glyphicon-resize-full" />);
    }
  }

  render() {
    return React.createElement("div", { "className": "individual-header" }, this.renderClose(), this.renderExpand(), this.renderInstanceInfo(), this.renderTaskLegend(), React.createElement("span", { "className": "right-buttons" }, React.createElement("a", { "className": "action-link", ["onClick"]: () => this.props.scrollToBottom(this.props.taskGroupId), "title": "Scroll to Bottom" }, <span className="glyphicon glyphicon-chevron-down" />), React.createElement("a", { "className": "action-link", ["onClick"]: () => this.props.scrollToTop(this.props.taskGroupId), "title": "Scroll to Top" }, <span className="glyphicon glyphicon-chevron-up" />)));
  }
}

TaskGroupHeader.propTypes = {
  taskGroupId: React.PropTypes.number.isRequired,
  tasks: React.PropTypes.array.isRequired
};

let mapStateToProps = function (state, ownProps) {
  if (!(ownProps.taskGroupId in state.taskGroups)) {
    return {
      taskGroupsCount: state.taskGroups.length,
      tasks: []
    };
  }
  return {
    taskGroupsCount: state.taskGroups.length,
    tasks: state.taskGroups[ownProps.taskGroupId].taskIds.map(taskId => state.tasks[taskId])
  };
};

let mapDispatchToProps = { scrollToTop, scrollToBottom, removeTaskGroup, expandTaskGroup };

export default connect(mapStateToProps, mapDispatchToProps)(TaskGroupHeader);

