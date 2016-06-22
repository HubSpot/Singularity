import React from 'react';
import { connect } from 'react-redux';

import TaskFilters from './TaskFilters';

class TasksPage extends React.Component {

  handleFilterChange(filter) {
    console.log(filter);
  }

  render() {
    console.log(this.props);
    return (
      <TaskFilters onFilterChange={this.handleFilterChange.bind(this)} />
    );
  }
}

function mapStateToProps(state) {
  return {
    tasks: state.api.tasks.data
  }
}

export default connect(mapStateToProps)(TasksPage);
