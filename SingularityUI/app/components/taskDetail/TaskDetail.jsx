import React from 'react';
import { connect } from 'react-redux';

class TaskDetail extends React.Component {

  render() {
    let task = this.props.task[this.props.taskId].data;
    console.log(task);
    return (
      <h4>task detail</h4>
    );
  }
}

function mapStateToProps(state) {
  return {
    task: state.api.task
  };
}

export default connect(mapStateToProps)(TaskDetail);
