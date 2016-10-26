import React from 'react';
import TaskStatusIndicator from './TaskStatusIndicator';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import Utils from '../../utils';
import { Link } from 'react-router';

import { connect } from 'react-redux';

import { removeTaskGroup, expandTaskGroup, scrollToTop, scrollToBottom } from '../../actions/log';

class TaskGroupHeader extends React.Component {
  toggleLegend() {}
  // TODO

  getInstanceNoToolTip(taskData) {
    return <ToolTip id={taskData.id}>Deploy ID: {taskData.deployId}<br />Host: {taskData.host}</ToolTip>;
  }

  renderInstanceInfo() {
    const instances = _.without(this.props.tasks, undefined).map((task) => {
      return Utils.getTaskDataFromTaskId(task.taskId).instanceNo;
    }).join(', ');
    if (this.props.tasks.length > 1) {
      return <span className="instance-link">Viewing Instances {instances}</span>;
    } else if (this.props.tasks.length > 0) {
      const taskData = Utils.getTaskDataFromTaskId(this.props.tasks[0].taskId);
      return <span><div className="width-constrained"><OverlayTrigger placement="bottom" overlay={this.getInstanceNoToolTip(taskData)}><Link className="instance-link" to={`task/${ this.props.tasks[0].taskId }`}>Instance {taskData.instanceNo}</Link></OverlayTrigger></div><TaskStatusIndicator status={this.props.tasks[0].lastTaskStatus} /></span>;
    }
    return <div className="width-constrained" />;
  }

  renderTaskLegend() {
    return (this.props.tasks.length > 1) && (
      <span className="right-buttons">
        <a className="action-link" onClick={this.toggleLegend}>
          <span className="glyphicon glyphicon-menu-hamburger" />
        </a>
      </span>
    );
  }

  renderClose() {
    if (this.props.taskGroupsCount > 1) {
      return React.createElement('a', { 'className': 'action-link', ['onClick']: () => this.props.removeTaskGroup(this.props.taskGroupId), 'title': 'Close Task' }, <span className="glyphicon glyphicon-remove" />);
    }
  }

  renderExpand() {
    if (this.props.taskGroupsCount > 1) {
      return React.createElement('a', { 'className': 'action-link', ['onClick']: () => this.props.expandTaskGroup(this.props.taskGroupId), 'title': 'Show only this Task' }, <span className="glyphicon glyphicon-resize-full" />);
    }
  }

  render() {
    return React.createElement('div', { 'className': 'individual-header' }, this.renderClose(), this.renderExpand(), this.renderInstanceInfo(), this.renderTaskLegend(), React.createElement('span', { 'className': 'right-buttons' }, React.createElement('a', { 'className': 'action-link', ['onClick']: () => this.props.scrollToBottom(this.props.taskGroupId), 'title': 'Scroll to Bottom' }, <span className="glyphicon glyphicon-chevron-down" />), React.createElement('a', { 'className': 'action-link', ['onClick']: () => this.props.scrollToTop(this.props.taskGroupId), 'title': 'Scroll to Top' }, <span className="glyphicon glyphicon-chevron-up" />)));
  }
}

TaskGroupHeader.propTypes = {
  taskGroupId: React.PropTypes.number.isRequired,
  tasks: React.PropTypes.array.isRequired
};

const mapStateToProps = (state, ownProps) => {
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

const mapDispatchToProps = { scrollToTop, scrollToBottom, removeTaskGroup, expandTaskGroup };

export default connect(mapStateToProps, mapDispatchToProps)(TaskGroupHeader);
