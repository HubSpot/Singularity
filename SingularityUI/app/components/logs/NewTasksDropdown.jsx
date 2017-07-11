import React from 'react';
import classNames from 'classnames';

class NewTasksDropdown extends React.Component  {

  renderTaskItems() {
    if (!this.props.ready || !this.props.runningTasks) {
      return (<li><a className="disabled">Loading...</a></li>);
    }

    if (this.props.runningTasks.length === 0) {
      return (<li><a className="disabled">No running instances</a></li>);
    }

    return this.props.runningTasks.map((task, key) => {
      const checkedClass = this.props.visibleTasks.includes(task.taskId.id)
        ? 'glyphicon-check'
        : 'glyphicon-unchecked';

      return (<li key={key}>
          <a onClick={() => this.props.onToggle(task.taskId.id)}>
              <span className={classNames('glyphicon', checkedClass)} />
              <span>Instance {task.taskId.instanceNo}</span>
          </a>
      </li>);
    });
  };

  render() {
    return (
      <div className="btn-group" title="Select Instances">
        <button type="button" className="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
            <span className="glyphicon glyphicon-tasks"></span> <span className="caret"></span>
        </button>
        <ul className="dropdown-menu dropdown-menu-right">
            {this.renderTaskItems()}
        </ul>
      </div>
    );
  }
};

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
