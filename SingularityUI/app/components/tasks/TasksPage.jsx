import React from 'react';
import { connect } from 'react-redux';

export default class TasksPage extends React.Component {

  render() {
    console.log(this.props);
    return (
      <div />
    );
  }
}

function mapStateToProps(state) {
    return {
        tasks: state.api.tasks.data
    }
}

export default connect(mapStateToProps)(TasksPage);
