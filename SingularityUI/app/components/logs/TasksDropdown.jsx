import React from 'react';
import classNames from 'classnames';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import { toggleTaskLog } from '../../actions/log';

import { getHostFromTaskId } from '../../utils';

import { connect } from 'react-redux';

class TasksDropdown extends React.Component {
  getTaskListTooltip(task) {
    return <ToolTip id={task.taskId.id}>Host: {task.taskId.host}</ToolTip>
  }

  renderListItems() {
    const props = this.props;
    const getTaskListTooltip = this.getTaskListTooltip;
    if (this.props.activeTasks && this.props.taskIds) {
      if (this.props.activeTasks.length > 0) {
        return _.sortBy(this.props.activeTasks, function (t) {return t.taskId.instanceNo }).map( function (task, i) {
          let classes = ['glyphicon'];
          if (__in__(task.taskId.id, props.taskIds)) {
            classes.push('glyphicon-check');
          } else {
            classes.push('glyphicon-unchecked');
          }
          return <li key={i}>
            <OverlayTrigger placement='left' overlay={getTaskListTooltip(task)}>
              <a onClick={function () { props.toggleTaskLog(task.taskId.id); }}>
                <span className={classNames(classes)}></span>
                <span> Instance {task.taskId.instanceNo}</span>
              </a>
            </OverlayTrigger>
          </li>;
        });
      } else {
        return <li><a className="disabled">No running instances</a></li>
      }
    } else {
      return <li><a className="disabled">Loading active tasks...</a></li>
    }
  }

  render() {
    return <div className="btn-group" title="Select Instances">
      <button type="button" className="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
        <span className="glyphicon glyphicon-tasks"></span> <span className="caret"></span>
      </button>
      <ul className="dropdown-menu dropdown-menu-right">
        {this.renderListItems()}
      </ul>
    </div>
  }
}

function mapStateToProps(state) {
  return {
    activeTasks: state.activeRequest.activeTasks,
    taskIds: _.flatten(_.pluck(state.taskGroups, 'taskIds')),
  };
};

const mapDispatchToProps = { toggleTaskLog };

export default connect(mapStateToProps, mapDispatchToProps)(TasksDropdown);

function __in__(needle, haystack) {
  return haystack.indexOf(needle) >= 0;
}

