import React from 'react';
import Checkbox from 'react-bootstrap/lib/Checkbox';
import {ButtonGroup, DropdownButton} from 'react-bootstrap';
class NewTasksDropdown extends React.Component {

  renderTaskItems() {
    if (!this.props.ready || !this.props.runningTasks) {
      return (<li><a className="disabled">Loading...</a></li>);
    }

    if (this.props.runningTasks.length === 0) {
      return (<li><a className="disabled">No running instances</a></li>);
    }

    const listItems = [];

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
