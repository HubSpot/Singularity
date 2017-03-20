import React from 'react';
import classNames from 'classnames';

const NewTasksDropdown = ({ready, runningTasks, visibleTasks=[], onToggle}) => {
  const renderTaskItems = () => {
    if (!ready || !runningTasks) {
      return (<li><a className="disabled">Loading...</a></li>);
    }

    if (runningTasks.length === 0) {
      return (<li><a className="disabled">No running instances</a></li>);
    }

    return runningTasks.map((task, key) => {
      const checkedClass = visibleTasks.includes(task.taskId.id)
        ? 'glyphicon-check'
        : 'glyphicon-unchecked';

      return (<li key={key}>
          <a onClick={() => onToggle(task.taskId.id)}>
              <span className={classNames('glyphicon', checkedClass)} />
              <span>Instance {task.taskId.instanceNo}</span>
          </a>
      </li>);
    });
  };

  return (<div className="btn-group" title="Select Instances">
    <button type="button" className="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
      <span className="glyphicon glyphicon-tasks"></span> <span className="caret"></span>
    </button>
    <ul className="dropdown-menu dropdown-menu-right">
      {renderTaskItems()}
    </ul>
  </div>);
};

export default NewTasksDropdown;
