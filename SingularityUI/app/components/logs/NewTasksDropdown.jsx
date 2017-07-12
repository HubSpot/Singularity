import React from 'react';
import Checkbox from 'react-bootstrap/lib/Checkbox';
import {ButtonGroup, DropdownButton} from 'react-bootstrap';

class NewTasksDropdown extends React.Component {

  handleSelectAll() {
    if (this.props.visibleTasks.length >= Math.min(this.props.runningTasks.length, 8)) {
      if (!this.props.visibleTasks.includes(_.first(this.props.runningTasks).taskId.id)) {
        this.props.onToggle(_.first(this.props.runningTasks).taskId.id);
      }
      _.rest(this.props.runningTasks).forEach((task) => {
        if (this.props.visibleTasks.includes(task.taskId.id)) {
          this.props.onToggle(task.taskId.id);
        }
      });
    } else {
      _.take(this.props.runningTasks, 8).forEach((task) => {
        if (!this.props.visibleTasks.includes(task.taskId.id)) {
          this.props.onToggle(task.taskId.id);
        }
      });
      _.rest(this.props.runningTasks, 9).forEach((task) => {
        if (this.props.visibleTasks.includes(task.taskId.id)) {
          this.props.onToggle(task.taskId.id);
        }
      });
    }
  }

  renderTaskItems() {
    if (!this.props.ready || !this.props.runningTasks) {
      return (<li><a className="disabled">Loading...</a></li>);
    }

    if (this.props.runningTasks.length === 0) {
      return (<li><a className="disabled">No running instances</a></li>);
    }

    const listItems = [];

    if (this.props.runningTasks.length > 1) {
      listItems.push(
        <li key="select-all">
          <a>
            <Checkbox
              inline={true}
              checked={this.props.visibleTasks.length >= Math.min(this.props.runningTasks.length, 8)}
              onChange={() => this.handleSelectAll()}
            >
              Select All
            </Checkbox>
          </a>
        </li>
      );
    }

    listItems.push(this.props.runningTasks.map((task, key) => {
      return (
        <li key={key}>
          <a>
            <Checkbox
              inline={true}
              checked={this.props.visibleTasks.includes(task.taskId.id)}
              onChange={() => this.props.onToggle(task.taskId.id)}
              disabled={this.props.visibleTasks.includes(task.taskId.id) && this.props.visibleTasks.length === 1}
            >
              Instance {task.taskId.instanceNo}
            </Checkbox>
          </a>
        </li>
      );
    }));

    return listItems;
  }

  render() {
    return (
      <ButtonGroup title="Select Instances">
        <DropdownButton id="instance-dropdown" bsSize="small" title={<span className="glyphicon glyphicon-tasks"></span>}>
          {this.renderTaskItems()}
        </DropdownButton>
      </ButtonGroup>
    );
  }
}

NewTasksDropdown.propTypes = {
  ready: React.PropTypes.bool,
  runningTasks: React.PropTypes.array,
  visibleTasks: React.PropTypes.array,
  onToggle: React.PropTypes.func
};

NewTasksDropdown.defaultProps = {
  visibleTasks: []
};

export default NewTasksDropdown;
