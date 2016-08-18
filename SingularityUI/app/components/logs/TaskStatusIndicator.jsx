import React from 'react';
import Utils from '../../utils';

class TaskStatusIndicator extends React.Component {
  getClassName() {
    if (__in__(this.props.status, Utils.TERMINAL_TASK_STATES)) {
      return 'bg-danger';
    } else {
      return 'bg-info running';
    }
  }

  render() {
    if (this.props.status) {
      return <div className="status"><div className={`indicator ${ this.getClassName() }`} />{this.props.status.toLowerCase().replace('_', ' ')}</div>;
    } else {
      return <div />;
    }
  }
}

TaskStatusIndicator.propTypes = { status: React.PropTypes.string };

export default TaskStatusIndicator;

function __in__(needle, haystack) {
  return haystack.indexOf(needle) >= 0;
}

